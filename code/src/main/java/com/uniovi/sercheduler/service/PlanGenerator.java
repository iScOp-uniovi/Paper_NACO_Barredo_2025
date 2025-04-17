package com.uniovi.sercheduler.service;

import com.uniovi.sercheduler.dto.InstanceData;
import com.uniovi.sercheduler.dto.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/** Generator of possible schedules for workflows. */
public class PlanGenerator {

  Random random;
  InstanceData instanceData;

  /**
   * Full constructor.
   *
   * @param random Random generator to use.
   * @param instanceData Infrastructure to use,
   */
  public PlanGenerator(Random random, InstanceData instanceData) {
    this.random = random;
    this.instanceData = instanceData;
  }

  /**
   * Constructor with a default generator.
   *
   * @param instanceData Infrastructure to use,
   */
  public PlanGenerator(InstanceData instanceData) {
    this.random = new Random();
    this.instanceData = instanceData;
  }

  /**
   * Generates a random plan, but always in topology order.
   *
   * @return A list of plan pairs.
   */
  public List<PlanPair> generatePlan() {

    List<PlanPair> plan = new ArrayList<>();

    var parentStatus =
        instanceData.workflow().values().stream()
            .map(
                t ->
                    Map.entry(
                        t.getName(),
                        t.getParents().stream().map(Task::getName).collect(Collectors.toList())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    var hostsNames = instanceData.hosts().values().stream().toList();

    var tasksToExplore =
        instanceData.workflow().values().stream()
            .filter(t -> t.getParents().isEmpty())
            .collect(Collectors.toList());

    for (int i = 0; i < instanceData.workflow().size(); i++) {
      // Get a random task from the pool of possible
      var taskToExplore = tasksToExplore.remove(random.nextInt(0, tasksToExplore.size()));

      // We get a random host and create the schedule pair.
      plan.add(new PlanPair(taskToExplore, hostsNames.get(random.nextInt(0, hostsNames.size()))));

      // We need to remove the task from the parent list of non schedule parents.
      for (var child : taskToExplore.getChildren()) {
        parentStatus.get(child.getName()).remove(taskToExplore.getName());
        if (parentStatus.get(child.getName()).isEmpty()) {
          tasksToExplore.add(child);
        }
      }
    }

    return plan;
  }
}
