package com.uniovi.sercheduler.service;

import com.uniovi.sercheduler.dto.Host;
import com.uniovi.sercheduler.dto.InstanceData;
import com.uniovi.sercheduler.dto.Task;
import com.uniovi.sercheduler.dto.TaskFile;
import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import com.uniovi.sercheduler.service.support.ScheduleGap;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Abstract class for defining the process of calculating a makespan from a solution. */
public abstract class FitnessCalculator {

  InstanceData instanceData;
  Map<String, Map<String, Double>> computationMatrix;
  Map<String, Map<String, Long>> networkMatrix;

  Double referenceSpeedRead;
  Double referenceSpeedWrite;

  /**
   * Full constructor.
   *
   * @param instanceData Infrastructure to use.
   */
  protected FitnessCalculator(InstanceData instanceData) {
    this.instanceData = instanceData;
    this.computationMatrix = calculateComputationMatrix(instanceData.referenceFlops());
    this.networkMatrix = calculateNetworkMatrix();
    this.referenceSpeedWrite = calculateReferenceSpeedWrite();
    this.referenceSpeedRead = calculateReferenceSpeedRead();
  }

  private static Map.Entry<String, Map<String, Long>> calculateStaging(
      Map.Entry<Task, Map<String, Long>> entry) {
    // Do the staging
    Task task = entry.getKey();
    Map<String, Long> comms = entry.getValue();
    Long tasksBits = comms.values().stream().reduce(0L, Long::sum);
    var newComms =
        Stream.concat(
                comms.entrySet().stream(),
                Stream.of(Map.entry(task.getName(), task.getInput().getSizeInBits() - tasksBits)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return Map.entry(task.getName(), newComms);
  }

  private static Map.Entry<Task, Map<String, Long>> calculateTasksCommns(Task task) {
    return Map.entry(
        task,
        task.getParents().stream()
            .map(
                parent -> {
                  Long bitsTransferred =
                      parent.getOutput().getFiles().stream()
                          .filter(
                              f ->
                                  task.getInput().getFiles().stream()
                                      .map(TaskFile::getName)
                                      .toList()
                                      .contains(f.getName()))
                          .map(TaskFile::getSize)
                          .reduce(0L, Long::sum);

                  return Map.entry(parent.getName(), bitsTransferred);
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  /**
   * Get the fitness calculator for a specific method.
   *
   * @param fitness The requested fitness.
   * @param instanceData The data related to the problem.
   * @return The Fitness calculator.
   */
  public static FitnessCalculator getFitness(String fitness, InstanceData instanceData) {
    return switch (fitness) {
      case "simple",
          "simple-mono",
          "simple-makespan",
          "simple-makespan-mono",
          "simple-energy",
          "simple-energy-mono" ->
          new FitnessCalculatorSimple(instanceData);
      case "heft", "heft-makespan-mono", "heft-spea2", "heft-pesa2" ->
          new FitnessCalculatorHeft(instanceData);
      case "heft-energy-active", "heft-energy-mono-active" ->
          new FitnessCalculatorHeftEnergy(instanceData, "active");
      case "heft-energy-semi-active", "heft-energy-mono-semi-active" ->
          new FitnessCalculatorHeftEnergy(instanceData, "semi-active");

      case "min-energy-UM-active", "min-energy-UM-mono-active" ->
          new FitnessCalculatorMinEnergyUM(instanceData, "active");
      case "min-energy-UM-semi-active", "min-energy-UM-mono-semi-active" ->
          new FitnessCalculatorMinEnergyUM(instanceData, "semi-active");

      case "fvlt-me-active", "fvlt-me-mono-active" ->
          new FitnessCalculatorFastVirtualMachineForLargeTasks(instanceData, "active");
      case "fvlt-me-semi-active", "fvlt-me-mono-semi-active" ->
          new FitnessCalculatorFastVirtualMachineForLargeTasks(instanceData, "semi-active");

      case "rank", "rank-makespan", "rank-makespan-mono" -> new FitnessCalculatorRank(instanceData);
      case "multi" ->
          new FitnessCalculatorMulti(
              instanceData,
              List.of(
                  new FitnessCalculatorSimple(instanceData),
                  new FitnessCalculatorHeft(instanceData),
                  new FitnessCalculatorRank(instanceData)),
              List.of(
                  new FitnessCalculatorSimple(instanceData),
                  new FitnessCalculatorMinEnergyUM(instanceData, "active"),
                  new FitnessCalculatorFastVirtualMachineForLargeTasks(instanceData, "active")),
              "none");

      case "multi-makespan", "multi-makespan-mono" ->
          new FitnessCalculatorMulti(
              instanceData,
              List.of(
                  new FitnessCalculatorSimple(instanceData),
                  new FitnessCalculatorHeft(instanceData),
                  new FitnessCalculatorRank(instanceData)),
              Collections.emptyList(),
              "makespan");
      case "multi-energy-no-fvlt", "multi-energy-mono-no-fvlt" ->
          new FitnessCalculatorMulti(
              instanceData,
              Collections.emptyList(),
              List.of(
                  new FitnessCalculatorSimple(instanceData),
                  new FitnessCalculatorHeftEnergy(instanceData, "active"),
                  new FitnessCalculatorMinEnergyUM(instanceData, "active")));
      case "multi-energy", "multi-energy-mono" ->
          new FitnessCalculatorMulti(
              instanceData,
              Collections.emptyList(),
              List.of(
                  new FitnessCalculatorSimple(instanceData),
                  new FitnessCalculatorMinEnergyUM(instanceData, "active"),
                  new FitnessCalculatorFastVirtualMachineForLargeTasks(instanceData, "active")),
              "energy");
      default -> throw new IllegalStateException("Unexpected value: " + fitness);
    };
  }

  /**
   * Calculates the time it takes to execute a task in each host.
   *
   * <p>The runtime from the workflow comes in second, but we don't know the flops, so we need to
   * calculate them with a simple rule of three.
   *
   * @param referenceFlops The flops of the hardware that executed the workflow the first time.
   * @return A matrix with the time it takes to execute in each host.
   */
  public Map<String, Map<String, Double>> calculateComputationMatrix(Long referenceFlops) {

    return instanceData.workflow().values().stream()
        .map(
            task ->
                Map.entry(
                    task.getName(),
                    instanceData.hosts().values().stream()
                        .map(
                            host ->
                                Map.entry(
                                    host.getName(),
                                    task.getRuntime()
                                        * (referenceFlops / host.getFlops().doubleValue())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Calculates the communications between tasks.
   *
   * @return A map stating the input form each task.
   */
  public Map<String, Map<String, Long>> calculateNetworkMatrix() {

    return instanceData.workflow().values().stream()
        .map(FitnessCalculator::calculateTasksCommns)
        .map(FitnessCalculator::calculateStaging)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Calculates the ranking for the HEFT algorithm, the ranking is in DECREASING ORDER, being the
   * tasks with the highest cost the ones that should be executed first.
   *
   * @return The ranking in DECREASING order.
   */
  public LinkedHashMap<Task, Double> calculateHeftRanking() {
    Map<String, Double> savedCosts = new HashMap<>();

    var childrenStatus =
        instanceData.workflow().values().stream()
            .map(
                t ->
                    Map.entry(
                        t.getName(),
                        t.getChildren().stream().map(Task::getName).collect(Collectors.toList())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    var tasksToExplore =
        instanceData.workflow().values().stream()
            .filter(t -> t.getChildren().isEmpty())
            .collect(Collectors.toList());

    for (int i = 0; i < instanceData.workflow().size(); i++) {
      var taskToExplore = tasksToExplore.get(i);
      savedCosts.put(taskToExplore.getName(), calculateTaskCost(taskToExplore, savedCosts));

      // We need to remove the task from the children list of non-calculated parents.
      for (var parent : taskToExplore.getParents()) {
        childrenStatus.get(parent.getName()).remove(taskToExplore.getName());
        if (childrenStatus.get(parent.getName()).isEmpty()) {
          tasksToExplore.add(parent);
        }
      }
    }
    return savedCosts.entrySet().stream()
        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
        .collect(
            Collectors.toMap(
                t -> instanceData.workflow().get(t.getKey()),
                Map.Entry::getValue,
                (o, n) -> o,
                LinkedHashMap::new));
  }

  /**
   * Calculates the possible cost of a task, with the average of communications and computation
   * time.
   *
   * @param task Task to calculate.
   * @param savedCosts Contains the saved cost of past operations (acts as a cache).
   * @return The cost.
   */
  public Double calculateTaskCost(Task task, Map<String, Double> savedCosts) {
    if (savedCosts.get(task.getName()) != null) {
      return savedCosts.get(task.getName());
    }

    var taskCost =
        computationMatrix.get(task.getName()).values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElseThrow();

    var maxChild =
        task.getChildren().stream()
            .map(Task::getName)
            .map(savedCosts::get)
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0D);

    taskCost += task.getInput().getSizeInBits() / referenceSpeedRead;
    taskCost += task.getOutput().getSizeInBits() / referenceSpeedWrite;

    taskCost += maxChild;

    return taskCost;
  }

  /**
   * Calculates the average of the communications speed for the input.
   *
   * @return the average.
   */
  public Double calculateReferenceSpeedRead() {
    return instanceData.hosts().values().stream()
        .map(h -> Math.min(h.getNetworkSpeed(), h.getDiskSpeed()))
        .mapToLong(Long::longValue)
        .average()
        .orElseThrow();
  }

  /**
   * Calculates the average of the communications speed for the output.
   *
   * @return the average.
   */
  public Double calculateReferenceSpeedWrite() {
    return instanceData.hosts().values().stream()
        .map(Host::getDiskSpeed)
        .mapToLong(Long::longValue)
        .average()
        .orElseThrow();
  }

  public abstract FitnessInfo calculateFitness(SchedulePermutationSolution solution);

  /**
   * Calculates the eft of a given task. Without insertion
   *
   * @param task Task to execute.
   * @param host Where does the task run.
   * @param schedule The schedule to update.
   * @param available When each machine is available.
   * @return Information about the executed task.
   */
  public TaskCosts calculateEftSemiActive(
      Task task, Host host, Map<String, TaskSchedule> schedule, Map<String, Double> available) {
    var parentsInfo = findTaskCommunications(task, host, schedule);
    var taskCommunications = parentsInfo.taskCommunications();
    Double diskReadStaging =
        networkMatrix.get(task.getName()).get(task.getName()) / host.getDiskSpeed().doubleValue();
    Double diskWrite = task.getOutput().getSizeInBits() / host.getDiskSpeed().doubleValue();
    Double ast = Math.max(available.getOrDefault(host.getName(), 0D), parentsInfo.maxEst());
    Double eft =
        diskReadStaging
            + diskWrite
            + computationMatrix.get(task.getName()).get(host.getName())
            + taskCommunications
            + ast;

    return new TaskCosts(diskReadStaging, diskWrite, eft, taskCommunications, ast);
  }

  /**
   * Calculates the eft of a given task. With insertion, which means that it take into account the
   * gaps.
   *
   * @param task Task to execute.
   * @param host Where does the task run.
   * @param schedule The schedule to update.
   * @param available When each machine is available.
   * @return Information about the executed task.
   */
  public TaskCosts calculateEftActive(
      Task task,
      Host host,
      Map<String, TaskSchedule> schedule,
      Map<String, List<ScheduleGap>> available) {
    var parentsInfo = findTaskCommunications(task, host, schedule);
    var taskCommunications = parentsInfo.taskCommunications();
    Double diskReadStaging =
        networkMatrix.get(task.getName()).get(task.getName()) / host.getDiskSpeed().doubleValue();
    Double diskWrite = task.getOutput().getSizeInBits() / host.getDiskSpeed().doubleValue();

    // We need to find the first available schedule where we can execute the full task and the ast
    // will be after the eft of the parents.

    double taskTime =
        diskReadStaging
            + diskWrite
            + computationMatrix.get(task.getName()).get(host.getName())
            + taskCommunications;

    var availableHostGaps =
        available.getOrDefault(host.getName(), List.of(new ScheduleGap(0D, Double.MAX_VALUE)));

    Double ast = null;
    double maxEst = parentsInfo.maxEst();

    for (ScheduleGap gap : availableHostGaps) {
      if (gap.start() >= maxEst && taskTime <= (gap.end() - gap.start())) {
        if (ast == null || gap.start() < ast) {
          ast = gap.start();
        }
      }
    }

    // Use default value if no matching gap is found
    if (ast == null) {
      ast = parentsInfo.maxEst();
    }

    Double eft = ast + taskTime;

    return new TaskCosts(diskReadStaging, diskWrite, eft, taskCommunications, ast);
  }

  /**
   * Find the time it takes to transfer all information between the task and it's parents.
   *
   * @param task Task to check.
   * @param host The host where it's going to run.
   * @param schedule The schedule to check the parents' info.
   * @return Information about parents.
   */
  public ParentsInfo findTaskCommunications(
      Task task, Host host, Map<String, TaskSchedule> schedule) {

    double taskCommunications = 0D;
    double maxEst = 0D;
    for (var parent : task.getParents()) {
      var parentHost = schedule.get(parent.getName()).host();

      var slowestSpeed = findHostSpeed(host, parentHost);

      taskCommunications +=
          networkMatrix.get(task.getName()).get(parent.getName()) / slowestSpeed.doubleValue();
      maxEst = Math.max(maxEst, schedule.get(parent.getName()).eft());
    }

    return new ParentsInfo(maxEst, taskCommunications);
  }

  /**
   * Finds the transfer speed between two hosts. Normally is going to be the slowest one from all
   * mediums.
   *
   * @param host Target host.
   * @param parentHost Source Host.
   * @return The speed in bits per second.
   */
  public Long findHostSpeed(Host host, Host parentHost) {
    // If the parent and the current host are the same we should return the disk
    // speed

    if (host.getName().equals(parentHost.getName())) {
      return host.getDiskSpeed();
    }

    // we need to find which network is worse
    var bandwidth = Math.min(host.getNetworkSpeed(), parentHost.getNetworkSpeed());

    // We need to do the minimum between bandwidth and parent disk
    return Math.min(bandwidth, parentHost.getDiskSpeed());
  }

  /**
   * Provides the name of the fitness used.
   *
   * @return The name of the fitness.
   */
  public abstract String fitnessName();
}
