package com.uniovi.sercheduler.commands;

import com.uniovi.sercheduler.dao.Objective;
import com.uniovi.sercheduler.jmetal.evaluation.MultiThreadEvaluationMulti;
import com.uniovi.sercheduler.jmetal.evaluation.MultiThreadedEvaluation;
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.pesa2.PESA2Builder;
import org.uma.jmetal.algorithm.multiobjective.spea2.SPEA2Builder;
import org.uma.jmetal.component.algorithm.multiobjective.NSGAIIBuilder;
import org.uma.jmetal.component.algorithm.singleobjective.GeneticAlgorithmBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.component.impl.GenerateBoxplotsWithR;
import org.uma.jmetal.lab.experiment.component.impl.GenerateFriedmanHolmTestTables;
import org.uma.jmetal.lab.experiment.component.impl.GenerateHtmlPages;
import org.uma.jmetal.lab.experiment.component.impl.GenerateLatexTablesWithStatistics;
import org.uma.jmetal.lab.experiment.component.impl.GenerateReferenceParetoFront;
import org.uma.jmetal.lab.experiment.component.impl.GenerateWilcoxonTestTablesWithR;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.GenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;

/** Class for running experiments using JMetal experiment tools. */
@Command
public class ExperimentJmetalCommand {

  static final Logger LOG = LoggerFactory.getLogger(ExperimentJmetalCommand.class);

  final WorkflowLoader workflowLoader;
  final HostLoader hostLoader;
  final ExperimentConfigLoader experimentConfigLoader;

  public ExperimentJmetalCommand(
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
  @Command(command = "jmetal")
  public String experiment(
      @Option(shortNames = 'W') String workflowsPath,
      @Option(shortNames = 'H') String hostsPath,
      @Option(shortNames = 'T') String type,
      @Option(shortNames = 'E', defaultValue = "100000") Integer executions,
      @Option(shortNames = 'S', defaultValue = "1") Long seed,
      @Option(shortNames = 'X', defaultValue = ".") String experimentPath,
      @Option(shortNames = 'C' ) String experimentConfigFile,
      @Option(shortNames = 'P', defaultValue = "1") int maxParallel) {

    var experimentConfig = experimentConfigLoader.readFromFile(new File(experimentConfigFile));

    var benchmarks = experimentConfig.workflows();

    Random random = new Random(seed);

    var fitness = experimentConfig.fitness();

    var experimentBaseDirectory = experimentPath + "/executions";
    double mutationProbability = 0.1;
    int populationSize = 100;
    int offspringPopulationSize = 100;
    Termination termination = new TerminationByEvaluations(executions);
    List<ExperimentProblem<SchedulePermutationSolution>> problemList = new ArrayList<>();
    List<ExperimentAlgorithm<SchedulePermutationSolution, List<SchedulePermutationSolution>>>
        algorithmList = new ArrayList<>();

    var objectives = experimentConfig.objectives().stream().map(Objective::of).toList();

    for (var benchmark : benchmarks) {

      for (int i = experimentConfig.minHosts();
          i <= experimentConfig.maxHosts();
          i = i * experimentConfig.hostIncrement()) {
        var baseProblem =
            new SchedulingProblem(
                benchmark + "-hosts-" + i,
                new File(workflowsPath + benchmark + ".json"),
                new File(hostsPath + type + "/hosts-" + i + ".json"),
                "441Gf",
                "simple",
                seed,
                objectives,
                objectives.get(0).objectiveName);

        var experimentProblem = new ExperimentProblem<>(baseProblem);
        problemList.add(experimentProblem);

        for (var f : fitness) {

          var problem =
              new SchedulingProblem(
                  benchmark + "-hosts-" + i,
                  new File(workflowsPath + benchmark + ".json"),
                  new File(hostsPath + type + "/hosts-" + i + ".json"),
                  experimentConfig.referenceSpeed(),
                  f,
                  seed,
                  objectives,
                  objectives.get(0).objectiveName);

          Operators operators = new Operators(problem.getInstanceData(), random);
          CrossoverOperator<SchedulePermutationSolution> crossover =
              new ScheduleCrossover(1, operators);

          MutationOperator<SchedulePermutationSolution> mutation =
              new ScheduleMutation(mutationProbability, operators);

          for (int run = 0; run < experimentConfig.independentRuns(); run++) {
            Algorithm<List<SchedulePermutationSolution>> algorithm;

            if (f.contains("mono")) {
              algorithm =
                  new GeneticAlgorithmBuilder<>(
                          "GGA",
                          problem,
                          populationSize,
                          offspringPopulationSize,
                          crossover,
                          mutation)
                      .setTermination(termination)
                      .setEvaluation(getEvaluator("simple", problem, objectives))
                      .setSelection(new ScheduleSelection(random))
                      .setReplacement(new ScheduleReplacement(random, objectives.get(0)))
                      .build();

            } else if (f.contains("spea2")) {
              algorithm =
                  new SPEA2Builder<>(problem, crossover, mutation)
                      .setPopulationSize(populationSize)
                      .setMaxIterations(executions / populationSize)
                      .build();

            } else if (f.contains("pesa2")) {
              algorithm =
                  new PESA2Builder<>(problem, crossover, mutation)
                      .setPopulationSize(populationSize)
                      .setMaxEvaluations(executions)
                      .build();
            } else if (f.equals("multi")) {
              algorithm =
                  new NSGAIIBuilder<>(problem, 50, offspringPopulationSize, crossover, mutation)
                      .setTermination(termination)
                      .setEvaluation(getEvaluator("multi", problem, objectives))
                      .build();
            } else {

              algorithm =
                  new NSGAIIBuilder<>(
                          problem, populationSize, offspringPopulationSize, crossover, mutation)
                      .setTermination(termination)
                      .setEvaluation(getEvaluator("simple", problem, objectives))
                      .build();
            }

            algorithmList.add(new ExperimentAlgorithm<>(algorithm, f, experimentProblem, run));
          }

          LOG.info("Done benchmark {} with {} hosts and fitness {}", benchmark, i, f);
        }
      }
    }

    Experiment<SchedulePermutationSolution, List<SchedulePermutationSolution>> experiment =
        new ExperimentBuilder<SchedulePermutationSolution, List<SchedulePermutationSolution>>(
                "Scheduling")
            .setAlgorithmList(algorithmList)
            .setProblemList(problemList)
            .setExperimentBaseDirectory(experimentBaseDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setReferenceFrontDirectory(experimentBaseDirectory + "/Scheduling/referenceFronts")
            .setIndicatorList(
                List.of(
                    new Epsilon(),
                    new Spread(),
                    new GenerationalDistance(),
                    new PISAHypervolume(),
                    new InvertedGenerationalDistance(),
                    new InvertedGenerationalDistancePlus()))
            .setIndependentRuns(experimentConfig.independentRuns())
            .setNumberOfCores(maxParallel)
            .build();
    new ExecuteAlgorithms<>(experiment).run();

    try {

      if (experimentConfig.jmetalAnalysis()) {
        new GenerateReferenceParetoFront(experiment).run();
        new ComputeQualityIndicators<>(experiment).run();
        new GenerateLatexTablesWithStatistics(experiment).run();
        new GenerateFriedmanHolmTestTables<>(experiment).run();
        new GenerateWilcoxonTestTablesWithR<>(experiment).run();
        new GenerateBoxplotsWithR<>(experiment).setRows(3).setColumns(2).run();
        new GenerateHtmlPages<>(experiment).run();
      }

      computeStatistics(experiment, objectives);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return "All experiments done";
  }

  private Evaluation<SchedulePermutationSolution> getEvaluator(
      String evaluator, Problem<SchedulePermutationSolution> problem, List<Objective> objectives) {
    return switch (evaluator) {
      case "simple" -> new SequentialEvaluation<>(problem);
      case "multi" -> new MultiThreadEvaluationMulti(0, problem, objectives.get(1).objectiveName);
      default -> new MultiThreadedEvaluation(0, problem);
    };
  }

  // Method to compute and save statistics
  private static void computeStatistics(
      Experiment<SchedulePermutationSolution, List<SchedulePermutationSolution>> experiment,
      List<Objective> objectives)
      throws IOException {
    System.out.println("Computing statistics...");
    String outputDirectory = experiment.getExperimentBaseDirectory() + "/statistics/";
    new File(outputDirectory).mkdirs();

    // Iterate through all algorithms
    var algorithms =
        experiment.getAlgorithmList().stream()
            .map(ExperimentAlgorithm::getAlgorithmTag)
            .collect(Collectors.toSet());
    var workflows = experiment.getProblemList().stream().map(ExperimentProblem::getTag).toList();
    Map<String, List<ExecutionStat>> executionStatistics = new HashMap<>();
    for (var objective : objectives) {
      executionStatistics.put(objective.name(), new ArrayList<>());
    }

    for (String algorithm : algorithms) {

      for (var workflow : workflows) {

        Map<String, List<Double>> fitnessValues = new HashMap<>();
        for (var objective : objectives) {
          fitnessValues.put(objective.name(), new ArrayList<>());
        }
        // Collect fitness values from independent runs
        for (int run = 0; run < experiment.getIndependentRuns(); run++) {
          Map<String, List<Double>> fitnessValuesRun = new HashMap<>();
          for (var objective : objectives) {
            fitnessValuesRun.put(objective.name(), new ArrayList<>());
          }
          String resultFile =
              experiment.getExperimentBaseDirectory()
                  + "/data/"
                  + algorithm
                  + "/"
                  + workflow
                  + "/FUN"
                  + run
                  + ".csv";

          // Read the best fitness from the result file
          try (Scanner scanner = new Scanner(new File(resultFile))) {
            while (scanner.hasNextLine()) {

              var values = scanner.nextLine().trim().split(",");

              int i = 0;
              for (var objective : objectives) {
                fitnessValuesRun.get(objective.name()).add(Double.parseDouble(values[i]));
                i++;
              }
            }
          }

          for (var objective : objectives) {
            Double bestOfTheRun =
                fitnessValuesRun.get(objective.name()).stream()
                    .mapToDouble(Double::doubleValue)
                    .min()
                    .orElseThrow();
            fitnessValues.get(objective.name()).add(bestOfTheRun);
          }
        }

        // Compute statistics for each metric
        for (var objective : objectives) {
          DoubleSummaryStatistics stats =
              fitnessValues.get(objective.name()).stream()
                  .mapToDouble(Double::doubleValue)
                  .summaryStatistics();
          var executionName = workflow + "-" + algorithm;
          executionStatistics
              .get(objective.name())
              .add(new ExecutionStat(executionName, workflow, algorithm, stats));

          System.out.printf(
              "Statistics for %s and objective %s: Mean = %.4f, Std. Dev. = %.4f, Min = %.4f, Max = %.4f%n",
              executionName,
              objective.name(),
              stats.getAverage(),
              Math.sqrt(
                  fitnessValues.get(objective.name()).stream()
                          .mapToDouble(val -> Math.pow(val - stats.getAverage(), 2))
                          .sum()
                      / stats.getCount()),
              stats.getMin(),
              stats.getMax());
        }
      }
    }

    // Write statistics to a CSV file for each algorithm
    try (FileWriter writer = new FileWriter(outputDirectory + "stats.csv")) {

      // TODO: Change to be compatible with more than two objectives
      var tableObjectives = List.of(Objective.ENERGY, Objective.MAKESPAN);
      String objective1 = tableObjectives.get(0).name();
      String objective2 = tableObjectives.get(1).name();

      writer.write(
          String.format(
              "Execution,Algorithm,Workflow,Hosts,Best %s,Mean %s,Min %s,Max %s,Best %s,Mean %s,Min %s,Max %s\n",
              objective1,
              objective1,
              objective1,
              objective1,
              objective2,
              objective2,
              objective2,
              objective2));
      for (int i = 0; i < executionStatistics.get(tableObjectives.get(0).name()).size(); i++) {
        // Find the hosts number with a regex
        var workflowName = executionStatistics.get(objective1).get(i).workflow();
        Pattern pattern = Pattern.compile(".*-hosts-(\\d+)$");
        Matcher matcher = pattern.matcher(workflowName);
        int hostsNumber = 0;
        if (matcher.find()) {
          hostsNumber = Integer.parseInt(matcher.group(1));
        }

        writer.write(
            String.format(
                "%s,%s,%s,%d,%f,%f,%f,%f,%f,%f,%f,%f\n",
                executionStatistics.get(objective1).get(i).excutionName(),
                executionStatistics.get(objective1).get(i).algorithm(),
                executionStatistics.get(objective1).get(i).workflow(),
                hostsNumber,
                executionStatistics.get(objective1).get(i).statistics().getMin(),
                executionStatistics.get(objective1).get(i).statistics().getAverage(),
                executionStatistics.get(objective1).get(i).statistics().getMin(),
                executionStatistics.get(objective1).get(i).statistics().getMax(),
                executionStatistics.get(objective2).get(i).statistics().getMin(),
                executionStatistics.get(objective2).get(i).statistics().getAverage(),
                executionStatistics.get(objective2).get(i).statistics().getMin(),
                executionStatistics.get(objective2).get(i).statistics().getMax()));
      }
    }
  }

  private record ExecutionStat(
      String excutionName, String workflow, String algorithm, DoubleSummaryStatistics statistics) {}
}
