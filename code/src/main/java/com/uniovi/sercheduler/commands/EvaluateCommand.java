package com.uniovi.sercheduler.commands;

import static org.uma.jmetal.util.AbstractAlgorithmRunner.printFinalSolutionSet;

import com.uniovi.sercheduler.dao.Objective;
import com.uniovi.sercheduler.jmetal.evaluation.MultiThreadEvaluationMulti;
import com.uniovi.sercheduler.jmetal.operator.ScheduleCrossover;
import com.uniovi.sercheduler.jmetal.operator.ScheduleMutation;
import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import com.uniovi.sercheduler.jmetal.problem.SchedulingProblem;
import com.uniovi.sercheduler.parser.HostLoader;
import com.uniovi.sercheduler.parser.WorkflowLoader;
import com.uniovi.sercheduler.service.Operators;
import com.uniovi.sercheduler.service.ScheduleExporter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.NSGAIIBuilder;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.util.observer.impl.FitnessObserver;

/** Contains all commands related to the execution of the GA. */
@Command
public class EvaluateCommand {

  static final Logger LOG = LoggerFactory.getLogger(EvaluateCommand.class);

  final WorkflowLoader workflowLoader;
  final HostLoader hostLoader;

  final ScheduleExporter scheduleExporter;

  /**
   * Full constructor.
   *
   * @param workflowLoader Loads workflows.
   * @param hostLoader Loads hosts.
   * @param scheduleExporter Exports the schedules.
   */
  public EvaluateCommand(
      WorkflowLoader workflowLoader, HostLoader hostLoader, ScheduleExporter scheduleExporter) {
    this.workflowLoader = workflowLoader;
    this.hostLoader = hostLoader;
    this.scheduleExporter = scheduleExporter;
  }

  /**
   * Command to run the GA for a given instance an infrastructure.
   *
   * @param hostsFile Relative or Absolute path to the hosts file.
   * @param workflowFile Relative or Absolute path to the workflow file.
   * @param executions Number of schedule to generate
   * @param seed Random seed to choose.
   * @return The text to print at the end.
   */
  @Command(command = "evaluate")
  public String evaluate(
      @Option(shortNames = 'H', required = true) String hostsFile,
      @Option(shortNames = 'W', required = true) String workflowFile,
      @Option(shortNames = 'E', defaultValue = "1000") Integer executions,
      @Option(shortNames = 'S', defaultValue = "1") Long seed,
      @Option(shortNames = 'F', defaultValue = "simple") String fitness) {
    final Instant start = Instant.now();
    List<Objective> objectives = List.of(Objective.MAKESPAN, Objective.ENERGY);

    var problem =
        new SchedulingProblem(
            new File(workflowFile),
            new File(hostsFile),
            "441Gf",
            fitness,
            seed,
            objectives,
            Objective.MAKESPAN.objectiveName);

    Operators operators = new Operators(problem.getInstanceData(), new Random(seed));
    CrossoverOperator<SchedulePermutationSolution> crossover = new ScheduleCrossover(1, operators);

    double mutationProbability = 0.1;
    MutationOperator<SchedulePermutationSolution> mutation =
        new ScheduleMutation(mutationProbability, operators);

    int populationSize = 100;
    int offspringPopulationSize = 100;

    Termination termination = new TerminationByEvaluations(executions);

    EvolutionaryAlgorithm<SchedulePermutationSolution> gaAlgo =
        new NSGAIIBuilder<>(problem, populationSize, offspringPopulationSize, crossover, mutation)
            .setTermination(termination)
            .setEvaluation(
                new MultiThreadEvaluationMulti(16, problem, objectives.get(1).objectiveName))
            //  .setSelection(new ScheduleSelection(new Random(seed)))
            //  .setReplacement(new ScheduleReplacement(new Random(seed)))
            .build();

    gaAlgo.observable().register(new FitnessObserver(100));

    gaAlgo.run();

    var population = gaAlgo.result();
    LOG.info("Total execution time : {} ms", gaAlgo.totalComputingTime());
    LOG.info("Number of evaluations: {} ", gaAlgo.numberOfEvaluations());
    printFinalSolutionSet(population);

    var bestSolution =
        gaAlgo.result().stream()
            .min(
                Comparator.comparing(
                    s -> s.getFitnessInfo().fitness().get(Objective.MAKESPAN.objectiveName)));
    var makespan =
        bestSolution.orElseThrow().getFitnessInfo().fitness().get(Objective.MAKESPAN.objectiveName);
    var energy =
        bestSolution.orElseThrow().getFitnessInfo().fitness().get(Objective.ENERGY.objectiveName);

    Instant finish = Instant.now();

    var timeElapsed = Duration.between(start, finish);

    // Writing the solution to excel

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Data");

      // Create header row
      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("Benchmark");
      headerRow.createCell(1).setCellValue("Hosts");
      headerRow.createCell(2).setCellValue("Makespan");
      headerRow.createCell(3).setCellValue("Time");

      // Populate data
      int rowNum = 1;

      Row row = sheet.createRow(rowNum);
      row.createCell(0).setCellValue(workflowFile);
      row.createCell(1).setCellValue(hostsFile);
      row.createCell(2).setCellValue(makespan);
      row.createCell(3).setCellValue(timeElapsed.toSeconds());

      // Write the workbook to a file
      try (FileOutputStream outputStream = new FileOutputStream("results.xlsx")) {
        workbook.write(outputStream);
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return String.format(
        "Evaluation done, it took %d and the best fitness is %f and the energy is %f",
        timeElapsed.toSeconds(), makespan, energy);
  }
}
