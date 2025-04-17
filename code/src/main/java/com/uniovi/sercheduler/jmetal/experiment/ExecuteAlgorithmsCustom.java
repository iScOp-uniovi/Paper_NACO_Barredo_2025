package com.uniovi.sercheduler.jmetal.experiment;

import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

public class ExecuteAlgorithmsCustom
    extends ExecuteAlgorithms<SchedulePermutationSolution, List<SchedulePermutationSolution>> {

  private Experiment<SchedulePermutationSolution, List<SchedulePermutationSolution>> experiment;

  public ExecuteAlgorithmsCustom(
      Experiment<SchedulePermutationSolution, List<SchedulePermutationSolution>> configuration) {
    super(configuration);
    this.experiment = configuration;
  }

  @Override
  public void run() {
    JMetalLogger.logger.info("ExecuteAlgorithms: Preparing output directory");
    this.prepareOutputDirectory();
    // Create a custom ForkJoinPool with the desired parallelism level
    ForkJoinPool customThreadPool = new ForkJoinPool(this.experiment.getNumberOfCores());

    int retryCounter = 0;
    int maxRetries = 5;
    boolean computationNotFinished = true;

    while (computationNotFinished && retryCounter < maxRetries) {
      List<ExperimentAlgorithm<?, ?>> unfinishedAlgorithmList = this.checkTaskStatus();
      if (unfinishedAlgorithmList.isEmpty()) {
        computationNotFinished = false;
      } else {
        JMetalLogger.logger.info(
            "ExecuteAlgorithms: there are " + unfinishedAlgorithmList.size() + " runs pending");
          try {
              customThreadPool.submit(() -> {
                unfinishedAlgorithmList.parallelStream().forEach((algorithm) -> algorithm.runAlgorithm(this.experiment));
              }).get();
          } catch (InterruptedException | ExecutionException e) {
              throw new RuntimeException(e);
          } finally {
            customThreadPool.shutdown(); // Shut down the custom thread pool
          }
          ++retryCounter;
      }
    }

    if (computationNotFinished) {
      JMetalLogger.logger.severe("There are unfinished tasks after " + maxRetries + " tries");
    } else {
      JMetalLogger.logger.info("Algorithm runs finished. Number of tries: " + retryCounter);
    }
  }




  private void prepareOutputDirectory() {
    if (this.experimentDirectoryDoesNotExist()) {
      this.createExperimentDirectory();
    }
  }

  private boolean experimentDirectoryDoesNotExist() {
    File experimentDirectory = new File(this.experiment.getExperimentBaseDirectory());
    boolean result = !experimentDirectory.exists() || !experimentDirectory.isDirectory();
    return result;
  }

  private void createExperimentDirectory() {
    File experimentDirectory = new File(this.experiment.getExperimentBaseDirectory());
    if (experimentDirectory.exists()) {
      experimentDirectory.delete();
    }

    boolean result = (new File(this.experiment.getExperimentBaseDirectory())).mkdirs();
    if (!result) {
      throw new JMetalException(
          "Error creating org.uma.jmetal.experiment directory: "
              + this.experiment.getExperimentBaseDirectory());
    }
  }
}
