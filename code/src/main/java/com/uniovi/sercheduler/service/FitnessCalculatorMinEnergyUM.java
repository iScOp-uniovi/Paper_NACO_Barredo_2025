package com.uniovi.sercheduler.service;

import com.uniovi.sercheduler.dto.Host;
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
public class FitnessCalculatorMinEnergyUM extends FitnessCalculator {
  private String planificationType;

  public FitnessCalculatorMinEnergyUM(InstanceData instanceData, String planificationType) {
    super(instanceData);
    this.planificationType = planificationType;
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
        eftAndAst =
            calculateHeftTaskCostActive(schedulePair.task(), schedule, availableActive, makespan);

      } else {
        eftAndAst =
            calculateHeftTaskCostSemiActive(
                schedulePair.task(), schedule, availableSemiActive, makespan);
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
    return "min-energy-UM";
  }

  private EftAndAst calculateHeftTaskCostSemiActive(
      Task task,
      HashMap<String, TaskSchedule> schedule,
      HashMap<String, Double> available,
      Double currentMakespan) {
    HashMap<String, EftAndEnergy> tempEftAndEnergy = new HashMap<>();
    HashMap<String, TaskCosts> possibleTaskCosts = new HashMap<>();

    for (var host : instanceData.hosts().values()) {

      var taskCosts = calculateEftSemiActive(task, host, schedule, available);

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

    // We need to sort the possible solutions and find the one that doesn't modify the makespan and
    // has the less energy consumption. If we have to modify the makespan we will choose the first
    // item in the list which is the one that consumes less and take less.

    var sortedEftAndEnergy =
        tempEftAndEnergy.entrySet().stream()
            .sorted(
                Comparator.comparing((Map.Entry<String, EftAndEnergy> e) -> e.getValue().energy())
                    .thenComparing(e -> e.getValue().eft()))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

    String selectedHostName =
        sortedEftAndEnergy.entrySet().stream()
            .filter(e -> e.getValue().eft() < currentMakespan)
            .findFirst()
            .orElse(sortedEftAndEnergy.entrySet().iterator().next())
            .getKey();

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
      Task task,
      HashMap<String, TaskSchedule> schedule,
      Map<String, List<ScheduleGap>> available,
      Double currentMakespan) {
    HashMap<String, EftAndEnergy> tempEftAndEnergy = new HashMap<>();
    HashMap<String, TaskCosts> possibleTaskCosts = new HashMap<>();

    for (var host : instanceData.hosts().values()) {

      var taskCosts = calculateEftActive(task, host, schedule, available);

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

    // Sort the possible solutions by energy consumption, then by EFT (earliest finish time)
    List<Map.Entry<String, EftAndEnergy>> sortedEftAndEnergy =
        new ArrayList<>(tempEftAndEnergy.entrySet());
    sortedEftAndEnergy.sort(
        Comparator.comparing((Map.Entry<String, EftAndEnergy> e) -> e.getValue().energy())
            .thenComparing(e -> e.getValue().eft()));

    // Find the best host name that doesn't modify the makespan
    String selectedHostName = null;
    for (Map.Entry<String, EftAndEnergy> entry : sortedEftAndEnergy) {
      if (entry.getValue().eft() < currentMakespan) {
        selectedHostName = entry.getKey();
        break;
      }
    }

    // If no such entry is found, use the first item in the sorted list
    if (selectedHostName == null && !sortedEftAndEnergy.isEmpty()) {
      selectedHostName = sortedEftAndEnergy.get(0).getKey();
    }

    if (selectedHostName == null) {
      throw new RuntimeException("No suitable host found");
    }

    var taskCosts = possibleTaskCosts.get(selectedHostName);
    var host = instanceData.hosts().get(selectedHostName);
    // we need to find the closest gap

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
