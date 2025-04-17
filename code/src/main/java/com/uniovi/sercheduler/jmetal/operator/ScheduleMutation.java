package com.uniovi.sercheduler.jmetal.operator;

import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import com.uniovi.sercheduler.service.Operators;
import org.uma.jmetal.operator.mutation.MutationOperator;

/** Mutation operator. */
public class ScheduleMutation implements MutationOperator<SchedulePermutationSolution> {
  final Operators operators;
  double mutationProbability;

  public ScheduleMutation(double mutationProbability, Operators operators) {
    this.mutationProbability = mutationProbability;
    this.operators = operators;
  }

  /**
   * Executes the mutation.
   *
   * @param schedulePermutationSolution Solution to mutate.
   * @return The mutated solution.
   */
  @Override
  public SchedulePermutationSolution execute(
      SchedulePermutationSolution schedulePermutationSolution) {

    return new SchedulePermutationSolution(
        schedulePermutationSolution.variables().size(),
        schedulePermutationSolution.objectives().length,
        null,
        operators.mutate(schedulePermutationSolution.getPlan()),
        schedulePermutationSolution.getArbiter());
  }

  /**
   * The probability of mutation.
   *
   * @return The mutation probability.
   */
  @Override
  public double mutationProbability() {
    return mutationProbability;
  }
}
