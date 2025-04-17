package com.uniovi.sercheduler.jmetal.operator;

import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import com.uniovi.sercheduler.service.Operators;
import java.util.List;
import org.uma.jmetal.operator.crossover.CrossoverOperator;

/** Crossover for schedules with topology order. */
public class ScheduleCrossover implements CrossoverOperator<SchedulePermutationSolution> {

  private final Operators operators;
  double crossoverProbability;

  public ScheduleCrossover(double crossoverProbability, Operators operators) {
    this.crossoverProbability = crossoverProbability;
    this.operators = operators;
  }

  /**
   * Getter for the crossover probability.
   *
   * @return The crossoverProbability.
   */
  @Override
  public double crossoverProbability() {
    return crossoverProbability;
  }

  /**
   * The number of parents required for the crossover.
   *
   * @return The required parents.
   */
  @Override
  public int numberOfRequiredParents() {
    return 2;
  }

  /**
   * The number of children generated via crossover.
   *
   * @return The number of generated children.
   */
  @Override
  public int numberOfGeneratedChildren() {
    return 2;
  }

  /**
   * Executes the crossover.
   *
   * @param schedulePermutationSolutions The list of solutions to cross.
   * @return A list of new solutions.
   */
  @Override
  public List<SchedulePermutationSolution> execute(
      List<SchedulePermutationSolution> schedulePermutationSolutions) {
    var schedule1 = schedulePermutationSolutions.get(0);

    // I need to create two children to have in the selection
    // an equal number of parents and children.

    var newPlan =
        operators.doCrossover(
            schedulePermutationSolutions.get(0).getPlan(),
            schedulePermutationSolutions.get(1).getPlan());

    var newPlan2 =
        operators.doCrossover(
            schedulePermutationSolutions.get(1).getPlan(),
            schedulePermutationSolutions.get(0).getPlan());
    return List.of(
        new SchedulePermutationSolution(
            schedule1.variables().size(),
            schedule1.objectives().length,
            null,
            newPlan,
            schedule1.getArbiter()),
        new SchedulePermutationSolution(
            schedule1.variables().size(),
            schedule1.objectives().length,
            null,
            newPlan2,
            schedule1.getArbiter()));
  }
}
