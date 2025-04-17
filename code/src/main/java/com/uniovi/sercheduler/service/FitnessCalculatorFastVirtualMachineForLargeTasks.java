package com.uniovi.sercheduler.service;

import com.uniovi.sercheduler.dto.InstanceData;
import com.uniovi.sercheduler.dto.Task;
import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import com.uniovi.sercheduler.service.support.EftAndAst;
import com.uniovi.sercheduler.service.support.EftAndEnergy;
import com.uniovi.sercheduler.service.support.ScheduleGap;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation for calculating the makespan using DNC model and heft second phase, focused on
 * Energy.
 */
public class FitnessCalculatorFastVirtualMachineForLargeTasks extends FitnessCalculator {
  private String planificationType;
  private double threshold;
  private Map<String, Boolean> priorityTasks;

  public FitnessCalculatorFastVirtualMachineForLargeTasks(
      InstanceData instanceData, String planificationType) {
    super(instanceData);
    this.planificationType = planificationType;
    var ranking = calculateHeftRanking();
    this.threshold = ranking.values().stream().mapToDouble(x -> x).average().orElseThrow();
    // We need a list of task that are considered as high priority and should be executed always on
    // fast machines.
    priorityTasks =
        ranking.entrySet().stream()
            .map(
                e -> {
                  if (e.getValue() > threshold) {
                    return Map.entry(e.getKey().getName(), true);
                  } else {
                    return Map.entry(e.getKey().getName(), false);
                  }
                })
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue, (o, n) -> o, HashMap::new));
  }

  /**
   * Calculates the makespan of a given schedule.
   *
   * @param solution@return The value of the makespan.
   */
  @Override
  public FitnessInfo calculateFitness(SchedulePermutationSolution solution) {
    var plan = solution.getPlan();

    double makespan = 0D;
    double energyActive = 0D;

    var availableSemiActive = new HashMap<String, Double>(instanceData.hosts().size());
    var availableActive = new HashMap<String, List<ScheduleGap>>(instanceData.hosts().size());

    var schedule = new HashMap<String, TaskSchedule>(instanceData.workflow().size());

    for (var schedulePair : plan) {
      EftAndAst eftAndAst;

      if (planificationType.equals("active")) {
        eftAndAst = calculateHeftTaskCostActive(schedulePair.task(), schedule, availableActive);

      } else {
        eftAndAst =
            calculateHeftTaskCostSemiActive(schedulePair.task(), schedule, availableSemiActive);
      }

      makespan = Math.max(eftAndAst.eft(), makespan);

      energyActive += (eftAndAst.eft() - eftAndAst.ast()) * schedulePair.host().getEnergyCost();
    }

    var orderedSchedule =
        schedule.values().stream().sorted(Comparator.comparing(TaskSchedule::ast)).toList();

    // We need to calculate the standby energy of each host
    double energyStandBy = 0;
    for (var host : instanceData.hosts().values()) {
      energyStandBy += host.getEnergyCostStandBy() * makespan;
    }

    double energy = energyActive + energyStandBy;
    return new FitnessInfo(
        Map.of("makespan", makespan, "energy", energy), orderedSchedule, fitnessName());
  }

  @Override
  public String fitnessName() {
    return "heft-energy";
  }

  private EftAndAst calculateHeftTaskCostSemiActive(
      Task task, HashMap<String, TaskSchedule> schedule, HashMap<String, Double> available) {
    HashMap<String, EftAndEnergy> tempEftAndEnergy = new HashMap<>();
    HashMap<String, TaskCosts> possibleTaskCosts = new HashMap<>();
    for (var host : instanceData.hosts().values()) {

      TaskCosts taskCosts = calculateEftSemiActive(task, host, schedule, available);

      var ast =
          taskCosts.eft()
              - computationMatrix.get(task.getName()).get(host.getName())
              - taskCosts.diskWrite()
              - taskCosts.taskCommunications()
              - taskCosts.diskReadStaging();
      double energyActive = (taskCosts.eft() - ast) * host.getEnergyCost();

      // The standby energy is calculated starting from the first instant the host is available
      // until the task is completed
      var hostReady = available.getOrDefault(host.getName(), 0D);

      double energyStandBy = (taskCosts.eft() - hostReady) * host.getEnergyCostStandBy();

      double energy = energyActive + energyStandBy;

      tempEftAndEnergy.put(host.getName(), new EftAndEnergy(taskCosts.eft(), energy));
      possibleTaskCosts.put(host.getName(), taskCosts);
    }

    // There are two possible orders, if the task is not high priority we use an
    // energy-efficient VM, if is high priority we will choose the fastest machine.
    var byEnergyAndEft =
        Comparator.comparing((Map.Entry<String, EftAndEnergy> e) -> e.getValue().energy())
            .thenComparing(e -> e.getValue().eft());
    var byEftAndEnergy =
        Comparator.comparing((Map.Entry<String, EftAndEnergy> e) -> e.getValue().eft())
            .thenComparing(e -> e.getValue().energy());

    var comparator = priorityTasks.get(task.getName()) ? byEftAndEnergy : byEnergyAndEft;

    String selectedHostName =
        tempEftAndEnergy.entrySet().stream().min(comparator).orElseThrow().getKey();

    var taskCosts = possibleTaskCosts.get(selectedHostName);
    var host = instanceData.hosts().get(selectedHostName);
    available.put(host.getName(), taskCosts.eft());
    var ast =
        available.get(host.getName())
            - computationMatrix.get(task.getName()).get(host.getName())
            - taskCosts.diskWrite()
            - taskCosts.taskCommunications()
            - taskCosts.diskReadStaging();

    schedule.put(task.getName(), new TaskSchedule(task, ast, taskCosts.eft(), host));
    return new EftAndAst(taskCosts.eft(), ast);
  }

  private EftAndAst calculateHeftTaskCostActive(
      Task task, HashMap<String, TaskSchedule> schedule, Map<String, List<ScheduleGap>> available) {
    HashMap<String, EftAndEnergy> tempEftAndEnergy = new HashMap<>();
    HashMap<String, TaskCosts> possibleTaskCosts = new HashMap<>();
    for (var host : instanceData.hosts().values()) {

      TaskCosts taskCosts = calculateEftActive(task, host, schedule, available);

      var ast =
          taskCosts.eft()
              - computationMatrix.get(task.getName()).get(host.getName())
              - taskCosts.diskWrite()
              - taskCosts.taskCommunications()
              - taskCosts.diskReadStaging();
      double energyActive = (taskCosts.eft() - ast) * host.getEnergyCost();

      // The standby energy is calculated starting from the first instant the host is available
      // until the task is completed
      List<ScheduleGap> scheduleGaps =
          available.getOrDefault(host.getName(), List.of(new ScheduleGap(0D, Double.MAX_VALUE)));

      ScheduleGap maxGap = null;
      for (ScheduleGap gap : scheduleGaps) {
        if (maxGap == null || gap.start() > maxGap.start()) {
          maxGap = gap;
        }
      }
      if (maxGap == null) {
        throw new RuntimeException("No ScheduleGap found");
      }

      var hostReady = maxGap.start();
      // If we are using a gap the energy can be negative
      double energyStandBy = (taskCosts.eft() - hostReady) * host.getEnergyCostStandBy();

      double energy = energyActive + Math.max(0, energyStandBy);

      tempEftAndEnergy.put(host.getName(), new EftAndEnergy(taskCosts.eft(), energy));
      possibleTaskCosts.put(host.getName(), taskCosts);
    }

    // There are two possible orders, if the task is not high priority we use an
    // energy-efficient VM, if is high priority we will choose the fastest machine.
    var byEnergyAndEft =
        Comparator.comparing((Map.Entry<String, EftAndEnergy> e) -> e.getValue().energy())
            .thenComparing(e -> e.getValue().eft());
    var byEftAndEnergy =
        Comparator.comparing((Map.Entry<String, EftAndEnergy> e) -> e.getValue().eft())
            .thenComparing(e -> e.getValue().energy());

    var comparator = priorityTasks.get(task.getName()) ? byEftAndEnergy : byEnergyAndEft;

    // we need to find the minimum value using the comparator.
    String selectedHostName = null;
    Map.Entry<String, EftAndEnergy> minEntry = null;

    for (Map.Entry<String, EftAndEnergy> entry : tempEftAndEnergy.entrySet()) {
      if (minEntry == null || comparator.compare(entry, minEntry) < 0) {
        minEntry = entry;
      }
    }

    if (minEntry == null) {
      throw new RuntimeException("No minimum entry found");
    }

    selectedHostName = minEntry.getKey();

    var taskCosts = possibleTaskCosts.get(selectedHostName);
    var host = instanceData.hosts().get(selectedHostName);

    // Retrieve the list of gaps or use a default value
    var availableHostGaps =
        available.getOrDefault(host.getName(), List.of(new ScheduleGap(0D, Double.MAX_VALUE)));

    // Find the closest gap to replace
    ScheduleGap gapToReplace = null;
    for (ScheduleGap gap : availableHostGaps) {
      if (taskCosts.eft() <= gap.end() && taskCosts.ast() >= gap.start()) {
        gapToReplace = gap;
        break;
      }
    }

    if (gapToReplace == null) {
      throw new RuntimeException("No suitable gap found");
    }

    // Split the gap into two, avoiding gaps with equal start and end
    List<ScheduleGap> newGaps = new ArrayList<>();
    if (!taskCosts.ast().equals(gapToReplace.start())) {
      newGaps.add(new ScheduleGap(gapToReplace.start(), taskCosts.ast()));
    }
    if (!taskCosts.eft().equals(gapToReplace.end())) {
      newGaps.add(new ScheduleGap(taskCosts.eft(), gapToReplace.end()));
    }

    // Create a new list of gaps by replacing the old gap with the new ones
    List<ScheduleGap> newHostGaps = new ArrayList<>();
    for (ScheduleGap gap : availableHostGaps) {
      if (!gap.equals(gapToReplace)) {
        newHostGaps.add(gap);
      }
    }
    newHostGaps.addAll(newGaps);

    // We need to put the available gaps
    available.put(host.getName(), newHostGaps);

    schedule.put(task.getName(), new TaskSchedule(task, taskCosts.ast(), taskCosts.eft(), host));
    return new EftAndAst(taskCosts.eft(), taskCosts.ast());
  }
}
