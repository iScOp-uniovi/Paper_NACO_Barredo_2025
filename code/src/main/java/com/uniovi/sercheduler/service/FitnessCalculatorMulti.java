package com.uniovi.sercheduler.service;

import com.uniovi.sercheduler.dto.InstanceData;
import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import com.uniovi.sercheduler.util.ThreadSafeStringArray;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Calculates the fitness using other fitness calculators. */
public class FitnessCalculatorMulti extends FitnessCalculator {

  static final Logger LOG = LoggerFactory.getLogger(FitnessCalculatorMulti.class);

  ThreadSafeStringArray fitnessUsage = ThreadSafeStringArray.getInstance();

  List<FitnessCalculator> fitnessCalculatorsMakespan;
  List<FitnessCalculator> fitnessCalculatorsEnergy;

  String overrideObjective;

  /**
   * Basic constructor
   *
   * @param instanceData Infrastructure to use.
   */
  public FitnessCalculatorMulti(
      InstanceData instanceData,
      List<FitnessCalculator> fitnessCalculatorsMakespan,
      List<FitnessCalculator> fitnessCalculatorsEnergy) {
    super(instanceData);
    this.fitnessCalculatorsMakespan = fitnessCalculatorsMakespan;
    this.fitnessCalculatorsEnergy = fitnessCalculatorsEnergy;
    this.overrideObjective = "none";
  }

  public FitnessCalculatorMulti(
      InstanceData instanceData,
      List<FitnessCalculator> fitnessCalculatorsMakespan,
      List<FitnessCalculator> fitnessCalculatorsEnergy,
      String overrideObjective) {
    super(instanceData);
    this.fitnessCalculatorsMakespan = fitnessCalculatorsMakespan;
    this.fitnessCalculatorsEnergy = fitnessCalculatorsEnergy;
    this.overrideObjective = overrideObjective;
  }

  /**
   * Calculates the fitness using 3 calculators and returns the best schedule.
   *
   * @param solution@return The information related to the Fitness.
   */
  @Override
  public FitnessInfo calculateFitness(SchedulePermutationSolution solution) {
    List<FitnessCalculator> fitnessCalculators;

    if ((solution.getArbiter().equals("energy") || overrideObjective.equals("energy"))  && !overrideObjective.equals("makespan")) {
      fitnessCalculators = fitnessCalculatorsEnergy;
    } else if (solution.getArbiter().equals("makespan") || overrideObjective.equals("makespan")) {
      fitnessCalculators = fitnessCalculatorsMakespan;
    } else {
      throw new RuntimeException("No fitness calculator found");
    }

    var objective = overrideObjective.equals("none") ? solution.getArbiter() : overrideObjective;

    var fitness =
        fitnessCalculators.stream()
            .map(c -> c.calculateFitness(solution))
            .min(Comparator.comparing(f -> f.fitness().get(objective)))
            .orElseThrow();

    fitnessUsage.setValue(fitness.fitnessFunction(), fitness.fitness().get("makespan"));

    return fitness;
  }

  @Override
  public String fitnessName() {
    return "multi";
  }
}
