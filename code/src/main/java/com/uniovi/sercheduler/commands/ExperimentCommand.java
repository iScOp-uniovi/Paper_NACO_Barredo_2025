package com.uniovi.sercheduler.commands;

import com.uniovi.sercheduler.dao.Objective;
import com.uniovi.sercheduler.dao.experiment.ExperimentConfig;
import com.uniovi.sercheduler.dto.BenchmarkData;
import com.uniovi.sercheduler.jmetal.operator.ScheduleCrossover;
import com.uniovi.sercheduler.jmetal.operator.ScheduleMutation;
import com.uniovi.sercheduler.jmetal.operator.ScheduleReplacement;
import com.uniovi.sercheduler.jmetal.operator.ScheduleSelection;
import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import com.uniovi.sercheduler.jmetal.problem.SchedulingProblem;
import com.uniovi.sercheduler.parser.HostLoader;
import com.uniovi.sercheduler.parser.WorkflowLoader;
import com.uniovi.sercheduler.parser.experiment.ExperimentConfigLoader;
import com.uniovi.sercheduler.service.Operators;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.singleobjective.GeneticAlgorithmBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;

/** Class for running experiments. */
@Command
public class ExperimentCommand {

  static final Logger LOG = LoggerFactory.getLogger(ExperimentCommand.class);

  final WorkflowLoader workflowLoader;
  final HostLoader hostLoader;
  final ExperimentConfigLoader experimentConfigLoader;

  public ExperimentCommand(
      WorkflowLoader workflowLoader,
      HostLoader hostLoader,
      ExperimentConfigLoader experimentConfigLoader) {
    this.workflowLoader = workflowLoader;
    this.hostLoader = hostLoader;
    this.experimentConfigLoader = experimentConfigLoader;
  }

  /**
   * Runs an experiment with all available fitness functions.
   *
   * @param executions Number of evaluations before stopping.
   * @param seed The random seed.
   * @return An exit string.
   */
  @Command(command = "experiment")
  public String experiment(
      @Option(shortNames = 'W') String workflowsPath,
      @Option(shortNames = 'H') String hostsPath,
      @Option(shortNames = 'T') String type,
      @Option(shortNames = 'E', defaultValue = "100000") Integer executions,
      @Option(shortNames = 'S', defaultValue = "1") Long seed,
      @Option(shortNames = 'C') String experimentConfigFile) {
    var benchmarksResult = new ArrayList<BenchmarkData>();

    var experimentConfig = experimentConfigLoader.readFromFile(new File(experimentConfigFile));

    var benchmarks = experimentConfig.workflows();
    Random random = new Random(seed);
    for (var benchmark : benchmarks) {

      var fitness = experimentConfig.fitness();
      for (int i = experimentConfig.minHosts();
          i <= experimentConfig.maxHosts();
          i = i * experimentConfig.hostIncrement()) {
        for (var f : fitness) {
          var benchmarkData =
              doExperiment(
                  executions,
                  seed,
                  benchmark,
                  type,
                  i,
                  f,
                  random,
                  workflowsPath,
                  hostsPath,
                  experimentConfig);

          benchmarksResult.add(benchmarkData);
          LOG.info("Done benchmark {} with {} hosts and fitness {}", benchmark, i, f);
        }
      }
    }
    // Writing the solution to excel
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Data");

      // Create header row
      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("Benchmark");
      headerRow.createCell(1).setCellValue("Hosts");
      headerRow.createCell(2).setCellValue("Fitness");
      headerRow.createCell(3).setCellValue("Makespan");
      headerRow.createCell(4).setCellValue("Energy");
      headerRow.createCell(5).setCellValue("Time");

      // Populate data
      int rowNum = 1;

      for (var result : benchmarksResult) {

        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(result.workflow());
        row.createCell(1).setCellValue(result.hosts());
        row.createCell(2).setCellValue(result.fitness());
        row.createCell(3).setCellValue(result.makespan());
        row.createCell(4).setCellValue(result.energy());
        row.createCell(5).setCellValue(result.time());

        // Write the workbook to a file
        try (FileOutputStream outputStream = new FileOutputStream("results.xlsx")) {
          workbook.write(outputStream);
        }
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return "All experiments done";
  }

  private BenchmarkData doExperiment(
      Integer executions,
      Long seed,
      String benchmark,
      String type,
      int hosts,
      String fitness,
      Random random,
      String workflowsPath,
      String hostsPath,
      ExperimentConfig experimentConfig) {
    final Instant start = Instant.now();

    List<Objective> objectives = List.of(Objective.MAKESPAN, Objective.ENERGY);
    var problem =
        new SchedulingProblem(
            new File(workflowsPath + benchmark + ".json"),
            new File(hostsPath + type + "/hosts-" + hosts + ".json"),
            experimentConfig.referenceSpeed(),
            fitness,
            seed,
            objectives,
            Objective.MAKESPAN.objectiveName);

    Operators operators = new Operators(problem.getInstanceData(), random);
    CrossoverOperator<SchedulePermutationSolution> crossover = new ScheduleCrossover(1, operators);

    double mutationProbability = 0.1;
    MutationOperator<SchedulePermutationSolution> mutation =
        new ScheduleMutation(mutationProbability, operators);

    int populationSize = 100;
    int offspringPopulationSize = 100;
    Termination termination = new TerminationByEvaluations(executions);

    var makespans = new ArrayList<Double>();
    var energies = new ArrayList<Double>();

    for (int i = 0; i < experimentConfig.independentRuns(); i++) {
      EvolutionaryAlgorithm<SchedulePermutationSolution> gaAlgo =
          new GeneticAlgorithmBuilder<>(
                  "GGA", problem, populationSize, offspringPopulationSize, crossover, mutation)
              .setTermination(termination)
              .setEvaluation(new MultiThreadedEvaluation<>(0, problem))
              .setSelection(new ScheduleSelection(random))
              .setReplacement(new ScheduleReplacement(random, objectives.get(0)))
              .build();

      gaAlgo.run();
      var bestSolution =
          gaAlgo.result().stream()
              .min(Comparator.comparing(s -> s.getFitnessInfo().fitness().get("makespan")))
              .orElseThrow();
      makespans.add(bestSolution.objectives()[0]);
      energies.add(bestSolution.objectives()[1]);
    }

    var mean = makespans.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    var energyMean = energies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    Instant finish = Instant.now();

    var timeElapsed = Duration.between(start, finish);
    return new BenchmarkData(benchmark, hosts, fitness, mean, energyMean, timeElapsed.toSeconds());
  }
}
