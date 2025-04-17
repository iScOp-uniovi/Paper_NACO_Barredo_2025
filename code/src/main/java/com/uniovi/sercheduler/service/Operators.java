package com.uniovi.sercheduler.service;

import com.uniovi.sercheduler.dto.InstanceData;
import com.uniovi.sercheduler.dto.Task;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/** Contains operations that can be done to an existing plan. */
public class Operators implements Serializable {

  InstanceData instanceData;
  Random random;

  public Operators(InstanceData instanceData) {
    this(instanceData, new Random());
  }

  public Operators(InstanceData instanceData, Random random) {
    this.instanceData = instanceData;
    this.random = random;
  }

  /**
   * Takes two plans and create a new one that merges both by a random position.
   *
   * @param plan1 plan to merge
   * @param plan2 plan to merge
   * @return The merged plan.
   */
  public List<PlanPair> doCrossover(List<PlanPair> plan1, List<PlanPair> plan2) {

    List<PlanPair> newPlan = new ArrayList<>();
    // Choose a random position to do the cut
    int position = random.nextInt(0, plan1.size());

    for (int i = 0; i < position; i++) {
      newPlan.add(plan1.get(i));
    }
    var planSet =
        newPlan.stream().map(PlanPair::task).map(Task::getName).collect(Collectors.toSet());
    for (int i = 0; i < plan2.size() && position < plan1.size(); i++) {
      // We use a set to more efficiently know if the plan already ahs that element

      if (planSet.add(plan2.get(i).task().getName())) {
        newPlan.add(new PlanPair(plan2.get(i).task(), plan2.get(i).host()));
        position++;
      }
    }
    return newPlan;
  }

  /**
   * Creates a new plan selecting a random position and moving the element to a new random position.
   *
   * <p>HAs a chance of modifying the host too.
   *
   * @param plan Original plan to mutate.
   * @return The new plan.
   */
  public List<PlanPair> mutate(List<PlanPair> plan) {

    // Choose a random position to do the cut
    int position = random.nextInt(0, plan.size());

    int positionLeft = position - 1;

    // We need to know the viable range to mutate and move the task.

    boolean leftNotFound = true;
    while (positionLeft >= 0 && leftNotFound) {
      if (plan.get(position).task().getParents().contains(plan.get(positionLeft).task())) {
        leftNotFound = false;
      } else {
        positionLeft--;
      }
    }

    int positionRight = position + 1;
    boolean rightNotFound = true;

    while (positionRight < plan.size() - 1 && rightNotFound) {
      if (plan.get(position).task().getChildren().contains(plan.get(positionRight).task())) {
        rightNotFound = false;
      } else {
        positionRight++;
      }
    }

    // now we can get a new position

    int newPosition = random.nextInt(positionLeft + 1, positionRight);
    var newPlan = new ArrayList<>(List.copyOf(plan));

    // Now we need to find if we move left or right

    if (newPosition < position) {
      // Go left
      for (int i = position - 1; i >= newPosition && i >= 0; i--) {
        newPlan.set(i + 1, newPlan.get(i));
      }
    } else if (newPosition > position) {
      for (int i = position + 1; i <= newPosition; i++) {
        newPlan.set(i - 1, newPlan.get(i));
      }
    }
    // put the mutated element in the new position
    var newHost =
        instanceData.hosts().values().stream()
            .toList()
            .get(random.nextInt(0, instanceData.hosts().size()));
    newPlan.set(newPosition, new PlanPair(plan.get(position).task(), newHost));
    return Collections.unmodifiableList(newPlan);
  }
}
