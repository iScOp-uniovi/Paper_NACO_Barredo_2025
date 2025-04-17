package com.uniovi.sercheduler.service;

import com.uniovi.sercheduler.dto.Host;
import com.uniovi.sercheduler.dto.InstanceData;
import com.uniovi.sercheduler.dto.Task;
import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import com.uniovi.sercheduler.service.support.EftAndAst;
import com.uniovi.sercheduler.service.support.EftAndEnergy;
import com.uniovi.sercheduler.service.support.ScheduleGap;

import java.util.*;
import java.util.stream.Stream;

/**
 * Implementation for calculating the makespan using DNC model and heft second phase, focused on
 * Energy.
 */
public class FitnessCalculatorHeftEnergy extends FitnessCalculator {
  private String planificationType;

  public FitnessCalculatorHeftEnergy(InstanceData instanceData, String planificationType) {
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
    var availableActive = new HashMap<String,  List<ScheduleGap>>(instanceData.hosts().size());


    var schedule = new HashMap<String, TaskSchedule>(instanceData.workflow().size());

    for (var schedulePair : plan) {
      EftAndAst eftAndAst;

      if (planificationType.equals("active")) {
        eftAndAst = calculateHeftTaskCostActive(schedulePair.task(), schedule, availableActive);

      } else {
        eftAndAst = calculateHeftTaskCostSemiActive(schedulePair.task(), schedule, availableSemiActive);
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

    // We sort the possible solutions by energy and eft. We choose the minimum.
    String selectedHostName =
        tempEftAndEnergy.entrySet().stream()
            .min(
                Comparator.comparing((Map.Entry<String, EftAndEnergy> e) -> e.getValue().energy())
                    .thenComparing(e -> e.getValue().eft()))
            .orElseThrow()
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
          Task task, HashMap<String, TaskSchedule> schedule,Map<String, List<ScheduleGap>> available) {
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
      var hostReady =
              available
                      .getOrDefault(host.getName(), List.of(new ScheduleGap(0D, Double.MAX_VALUE)))
                      .stream()
                      .max(Comparator.comparing(ScheduleGap::start))
                      .orElseThrow()
                      .start();

      double energyStandBy = (taskCosts.eft() - hostReady) * host.getEnergyCostStandBy();

      double energy = energyActive + Math.max(0,energyStandBy);

      tempEftAndEnergy.put(host.getName(), new EftAndEnergy(taskCosts.eft(), energy));
      possibleTaskCosts.put(host.getName(), taskCosts);
    }

    // We sort the possible solutions by energy and eft. We choose the minimum.
    String selectedHostName =
            tempEftAndEnergy.entrySet().stream()
                    .min(
                            Comparator.comparing((Map.Entry<String, EftAndEnergy> e) -> e.getValue().energy())
                                    .thenComparing(e -> e.getValue().eft()))
                    .orElseThrow()
                    .getKey();

    var taskCosts = possibleTaskCosts.get(selectedHostName);
    var host = instanceData.hosts().get(selectedHostName);

    // we need to find the closest gap

    var availableHostGaps =
            available.getOrDefault(host.getName(), List.of(new ScheduleGap(0D, Double.MAX_VALUE)));
    var gapToReplace =
            availableHostGaps.stream()
                    .filter(gap -> taskCosts.eft() <= gap.end() && taskCosts.ast() >= gap.start())
                    .findFirst()
                    .orElseThrow();
    // Now we need to split the gap in two, using the eft as the slice, depending of the cut we can
    // have one or two gaps. We always generate two gaps so we need to remove the gaps where the
    // start and the end are the same.

    var newGaps =
            Stream.of(
                            new ScheduleGap(gapToReplace.start(), taskCosts.ast()),
                            new ScheduleGap(taskCosts.eft(), gapToReplace.end()))
                    .filter(gap -> !gap.start().equals(gap.end()))
                    .toList();

    // Now we generate a new list with the old gaps and the new ones, removing the used gap.
    var newHostGaps = Stream.concat(availableHostGaps.stream()
            .filter(gap -> ! gap.equals(gapToReplace)),newGaps.stream()
    ).toList();

    // We need to put the available gaps
    available.put(host.getName(), newHostGaps);

    schedule.put(task.getName(), new TaskSchedule(task, taskCosts.ast(), taskCosts.eft(), host));
    return new EftAndAst(taskCosts.eft(), taskCosts.ast());
  }
}
