package com.uniovi.sercheduler.service;

import com.uniovi.sercheduler.dto.InstanceData;
import com.uniovi.sercheduler.dto.Task;
import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import java.util.ArrayList;
import java.util.List;

/** Implementation for calculating the makespan using DNC model. */
public class FitnessCalculatorRank extends FitnessCalculatorSimple {

  List<Task> heftRanking;

  /**
   * Full constructor.
   *
   * @param instanceData Information related to the instance.
   */
  public FitnessCalculatorRank(InstanceData instanceData) {
    super(instanceData);

    heftRanking = calculateHeftRanking().keySet().stream().toList();
  }

  /**
   * Calculates the makespan of a given schedule.
   *
   * @param solution@return The value of the makespan.
   */
  @Override
  public FitnessInfo calculateFitness(SchedulePermutationSolution solution) {
    var plan = solution.getPlan();
    var newPlan = new ArrayList<PlanPair>();

    for (int i = 0; i < plan.size(); i++) {
      newPlan.add(new PlanPair(heftRanking.get(i), plan.get(i).host()));
    }
    solution.setPlan(newPlan);
    return super.calculateFitness(solution);
  }

  @Override
  public String fitnessName(){
    return "rank";
  }
}
