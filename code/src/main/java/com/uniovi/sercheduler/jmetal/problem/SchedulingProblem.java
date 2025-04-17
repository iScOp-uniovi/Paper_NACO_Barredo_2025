package com.uniovi.sercheduler.jmetal.problem;

import com.uniovi.sercheduler.dao.Objective;
import com.uniovi.sercheduler.dto.InstanceData;
import com.uniovi.sercheduler.parser.HostFileLoader;
import com.uniovi.sercheduler.parser.HostLoader;
import com.uniovi.sercheduler.parser.WorkflowFileLoader;
import com.uniovi.sercheduler.parser.WorkflowLoader;
import com.uniovi.sercheduler.service.FitnessCalculator;
import com.uniovi.sercheduler.service.PlanGenerator;
import com.uniovi.sercheduler.service.PlanPair;
import com.uniovi.sercheduler.util.UnitParser;
import java.io.File;
import java.util.List;
import java.util.Random;
import org.uma.jmetal.problem.permutationproblem.PermutationProblem;

/** Defines the scheduling problem. */
public class SchedulingProblem implements PermutationProblem<SchedulePermutationSolution> {

  private final WorkflowLoader workflowLoader;
  private final HostLoader hostLoader;
  private final PlanGenerator planGenerator;

  private final FitnessCalculator fitnessCalculator;
  InstanceData instanceData;
  private String name;
  private List<Objective> objectives;
  private String defaultArbiter;

  /**
   * Full constructor.
   *
   * @param workflowFile The file containing the workflow.
   * @param hostsFile The file containing the hosts.
   * @param referenceSpeed The CPU reference speed to calculate the runtime.
   * @param fitness The fitness function to use.
   * @param seed The random seed to use
   */
  public SchedulingProblem(
      File workflowFile,
      File hostsFile,
      String referenceSpeed,
      String fitness,
      Long seed,
      List<Objective> objectives,
      String defaultArbiter) {
    this.workflowLoader = new WorkflowFileLoader();
    this.hostLoader = new HostFileLoader();
    this.instanceData = loadData(workflowFile, hostsFile, referenceSpeed);
    this.fitnessCalculator = FitnessCalculator.getFitness(fitness, instanceData);
    this.planGenerator = new PlanGenerator(new Random(seed), instanceData);
    this.name = "Scheduling problem";
    this.objectives = objectives;
    this.defaultArbiter = defaultArbiter;
  }

  /**
   * Full constructor.
   *
   * @param name The name of the problem.
   * @param workflowFile The file containing the workflow.
   * @param hostsFile The file containing the hosts.
   * @param referenceSpeed The CPU reference speed to calculate the runtime.
   * @param fitness The fitness function to use.
   * @param seed The random seed to use
   */
  public SchedulingProblem(
      String name,
      File workflowFile,
      File hostsFile,
      String referenceSpeed,
      String fitness,
      Long seed,
      List<Objective> objectives,
      String defaultArbiter) {
    this.name = name;
    this.workflowLoader = new WorkflowFileLoader();
    this.hostLoader = new HostFileLoader();
    this.instanceData = loadData(workflowFile, hostsFile, referenceSpeed);
    this.fitnessCalculator = FitnessCalculator.getFitness(fitness, instanceData);
    this.planGenerator = new PlanGenerator(new Random(seed), instanceData);
    this.objectives = objectives;
    this.defaultArbiter = defaultArbiter;
  }

  /**
   * Gets the size of the workflow.
   *
   * @return The length of the workflow
   */
  @Override
  public int length() {
    return instanceData.workflow().size();
  }

  /**
   * Number of variables of the problem.
   *
   * @return The number of variables of the problem.
   */
  @Override
  public int numberOfVariables() {
    return instanceData.workflow().size();
  }

  /**
   * The number of objectives.
   *
   * @return The number of objectives.
   */
  @Override
  public int numberOfObjectives() {
    return 2;
  }

  /**
   * The number of constrains.
   *
   * @return The number of constrains.
   */
  @Override
  public int numberOfConstraints() {
    return 0;
  }

  /**
   * The name of the problem.
   *
   * @return The name of the problem.
   */
  @Override
  public String name() {
    return this.name;
  }

  /**
   * Evaluates a solution.
   *
   * @param schedulePermutationSolution Solution to evaluate.
   * @return The evaluated solution.
   */
  @Override
  public SchedulePermutationSolution evaluate(
      SchedulePermutationSolution schedulePermutationSolution) {

    var fitnessInfo = fitnessCalculator.calculateFitness(schedulePermutationSolution);
    var plan = fitnessInfo.schedule().stream().map(s -> new PlanPair(s.task(), s.host())).toList();

    schedulePermutationSolution.setPlan(plan);
    schedulePermutationSolution.setFitnessInfo(fitnessInfo);

    for (int i = 0; i < objectives.size(); i++) {
      schedulePermutationSolution.objectives()[i] =
          fitnessInfo.fitness().get(objectives.get(i).objectiveName);
    }

    return schedulePermutationSolution;
  }

  /**
   * Generates a random solution.
   *
   * @return A new solution.
   */
  @Override
  public SchedulePermutationSolution createSolution() {
    var plan = planGenerator.generatePlan();
    return new SchedulePermutationSolution(numberOfVariables(), numberOfObjectives(), null, plan, defaultArbiter);
  }

  private InstanceData loadData(File workflowFile, File hostsFile, String referenceSpeed) {

    var hostsJson = hostLoader.readFromFile(hostsFile);
    var hosts = hostLoader.load(hostsJson);

    var workflow = workflowLoader.load(workflowLoader.readFromFile(workflowFile));

    return new InstanceData(workflow, hosts, UnitParser.parseUnits(referenceSpeed));
  }

  public InstanceData getInstanceData() {
    return instanceData;
  }
}
