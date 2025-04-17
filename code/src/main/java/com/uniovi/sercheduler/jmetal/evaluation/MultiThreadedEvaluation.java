package com.uniovi.sercheduler.jmetal.evaluation;

import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.errorchecking.Check;

public class MultiThreadedEvaluation implements Evaluation<SchedulePermutationSolution> {

  private final Problem<SchedulePermutationSolution> problem;
  private final int numberOfThreads;
  private int computedEvaluations;

  public MultiThreadedEvaluation(
      int numberOfThreads, Problem<SchedulePermutationSolution> problem) {
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
    computedEvaluations = 0;
  }

  @Override
  public List<SchedulePermutationSolution> evaluate(
      List<SchedulePermutationSolution> solutionList) {
    Check.notNull(solutionList);

    // Create a custom ForkJoinPool with the desired parallelism level
    ForkJoinPool customThreadPool = new ForkJoinPool(numberOfThreads);
      try {
          customThreadPool.submit(()->{
            solutionList.parallelStream().forEach(problem::evaluate);
          }).get();
      } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
      }finally{
        customThreadPool.shutdown();
      }

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
