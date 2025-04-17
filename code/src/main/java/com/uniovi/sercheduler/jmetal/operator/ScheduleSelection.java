package com.uniovi.sercheduler.jmetal.operator;

import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;

/** Selection of parents to mutate / crossover. The selection is completely random. */
public class ScheduleSelection implements Selection<SchedulePermutationSolution> {
  Random random;

  public ScheduleSelection() {
    this(new Random());
  }

  public ScheduleSelection(Random random) {
    this.random = random;
  }

  /**
   * Select the solutions to be mutated.
   *
   * @param list The parents to select.
   * @return The same list but shuffled.
   */
  @Override
  public List<SchedulePermutationSolution> select(List<SchedulePermutationSolution> list) {
    var listToShuffle = new ArrayList<>(list);
    Collections.shuffle(listToShuffle, random);
    return listToShuffle;
  }
}
