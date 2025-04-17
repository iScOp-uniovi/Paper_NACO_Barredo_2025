package com.uniovi.sercheduler.jmetal.operator;

import com.uniovi.sercheduler.dao.Objective;
import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;

/** Defines the replacement operator. */
public class ScheduleReplacement implements Replacement<SchedulePermutationSolution> {

  Random random;
  Objective objective;

  public ScheduleReplacement(Objective objective) {
    this(new Random(), objective);
  }

  public ScheduleReplacement(Random random, Objective objective) {
    this.random = random;
    this.objective = objective;
  }

  /**
   * Executes the replacement. Uses a tournament 4:2.
   *
   * @param parents The list of parents.
   * @param children The list of children.
   * @return The new pool of solution.
   */
  @Override
  public List<SchedulePermutationSolution> replace(
      List<SchedulePermutationSolution> parents, List<SchedulePermutationSolution> children) {
    var replacement = new ArrayList<SchedulePermutationSolution>();
    for (int i = 0; i < parents.size(); i = i + 2) {
      var parent1 = parents.get(i);
      var parent2 = parents.get(i + 1);
      var child1 = children.get(i);
      var child2 = children.get(i + 1);
      var tournament = List.of(parent1, parent2, child1, child2);
      var result =
          tournament.stream()
              .sorted(
                  Comparator.comparing(
                      s -> s.getFitnessInfo().fitness().get(objective.objectiveName)))
              .collect(
                  Collectors.collectingAndThen(
                      Collectors.toCollection(
                          () ->
                              new TreeSet<>(
                                  Comparator.comparing(
                                      s ->
                                          s.getFitnessInfo()
                                              .fitness()
                                              .get(objective.objectiveName)))),
                      ArrayList::new));

      if (result.size() == 1) {
        result.add(result.get(0));
      }
      replacement.addAll(result.subList(0, 2));
    }

    return replacement;
  }
}
