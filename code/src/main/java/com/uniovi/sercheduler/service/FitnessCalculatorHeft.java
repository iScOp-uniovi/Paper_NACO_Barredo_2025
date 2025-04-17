package com.uniovi.sercheduler.service;

import com.uniovi.sercheduler.dto.Host;
import com.uniovi.sercheduler.dto.InstanceData;
import com.uniovi.sercheduler.dto.Task;
import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import com.uniovi.sercheduler.service.support.EftAndAst;
import com.uniovi.sercheduler.service.support.ScheduleGap;

import java.util.*;
import java.util.stream.Stream;

/** Implementation for calculating the makespan using DNC model and heft second phase. */
public class FitnessCalculatorHeft extends FitnessCalculator {
  public FitnessCalculatorHeft(InstanceData instanceData) {
    super(instanceData);
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

    var available = new HashMap<String,  List<ScheduleGap>>(instanceData.hosts().size());
    var schedule = new HashMap<String, TaskSchedule>(instanceData.workflow().size());

    for (var schedulePair : plan) {

      var eftAndAst = calculateHeftTaskCost(schedulePair.task(), schedule, available);

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
    return "heft";
  }

  private EftAndAst calculateHeftTaskCost(
      Task task, HashMap<String, TaskSchedule> schedule, Map<String, List<ScheduleGap>> available) {
    double minEft = Double.MAX_VALUE;
    Optional<Host> selectedHost = Optional.empty();
    Optional<TaskCosts> selectedTaskCosts = Optional.empty();
    for (var host : instanceData.hosts().values()) {

      var taskCosts = calculateEftActive(task, host, schedule, available);
      double tmpEft = minEft;

      minEft = Math.min(minEft, taskCosts.eft());
      if (minEft != tmpEft) {
        selectedHost = Optional.of(host);
        selectedTaskCosts = Optional.of(taskCosts);
      }
    }
    var taskCosts = selectedTaskCosts.orElseThrow();
    var host = selectedHost.orElseThrow();

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
    return new EftAndAst(minEft, taskCosts.ast());
  }
}
