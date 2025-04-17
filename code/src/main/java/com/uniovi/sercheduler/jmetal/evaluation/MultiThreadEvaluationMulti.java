package com.uniovi.sercheduler.jmetal.evaluation;

import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import java.util.List;
import java.util.stream.Stream;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;

public class MultiThreadEvaluationMulti implements Evaluation<SchedulePermutationSolution> {

  private final Problem<SchedulePermutationSolution> problem;
  private final int numberOfThreads;
  private int computedEvaluations;
  private String alternativeArbiter;

  public MultiThreadEvaluationMulti(
      int numberOfThreads, Problem<SchedulePermutationSolution> problem, String alternativeArbiter) {
    Check.that(
        numberOfThreads >= 0, "The number of threads is a negative value: " + numberOfThreads);
    Check.notNull(problem);

    if (numberOfThreads == 0) {
      numberOfThreads = Runtime.getRuntime().availableProcessors();
    }
    System.setProperty(
        "java.util.concurrent.ForkJoinPool.common.parallelism", "" + numberOfThreads);

    this.numberOfThreads = numberOfThreads;
    this.problem = problem;
    this.alternativeArbiter = alternativeArbiter;
    computedEvaluations = 0;
  }

  @Override
  public List<SchedulePermutationSolution> evaluate(
      List<SchedulePermutationSolution> solutionList) {
    Check.notNull(solutionList);

    solutionList =
        solutionList.stream()
            .flatMap(
                s -> {
                  var copy = (SchedulePermutationSolution) s.copy();
                  copy.setArbiter(alternativeArbiter);

                  return Stream.of(s, copy);
                })
            .toList();
    solutionList.parallelStream().forEach(problem::evaluate);
    computedEvaluations = solutionList.size();

    return solutionList;
  }

  @Override
  public int computedEvaluations() {
    return computedEvaluations;
  }

  public int numberOfThreads() {
    return numberOfThreads;
  }

  @Override
  public Problem<SchedulePermutationSolution> problem() {
    return problem;
  }
}
