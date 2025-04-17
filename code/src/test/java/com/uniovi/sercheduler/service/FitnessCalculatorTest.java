package com.uniovi.sercheduler.service;

import static com.uniovi.sercheduler.util.LoadTestInstanceData.loadCalculatorTest;
import static com.uniovi.sercheduler.util.LoadTestInstanceData.loadFitnessTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.uniovi.sercheduler.dto.Host;
import com.uniovi.sercheduler.dto.InstanceData;
import com.uniovi.sercheduler.jmetal.problem.SchedulePermutationSolution;
import com.uniovi.sercheduler.util.UnitParser;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class FitnessCalculatorTest {

  // The tests uses the following graph
  /*
   *           │10
   *        ┌──▼─┐
   *    ┌───┤ T1 ├────┐
   *    │   └─┬──┘    │
   *    │18   │12     │8
   * ┌──▼─┐ ┌─▼──┐ ┌──▼─┐16
   * │ T2 │ │ T3 │ │ T4 ◄──
   * └──┬─┘ └─┬──┘ └──┬─┘
   *    │20   │24     │28
   *    │   ┌─▼──┐    │
   *    └───► T5 ◄────┘
   *        └────┘
   */
  @Test
  void calculateComputationMatrix() throws IOException {

    // Given a workflow of 5 tasks
    // Given a hosts list of 3

    InstanceData instanceData = loadCalculatorTest();

    // Then

    var expected =
        Map.ofEntries(
            new AbstractMap.SimpleEntry<>("task01", Map.of("HostA", 10D, "HostB", 5D, "HostC", 4D)),
            new AbstractMap.SimpleEntry<>(
                "task02", Map.of("HostA", 15D, "HostB", 7.5D, "HostC", 6D)),
            new AbstractMap.SimpleEntry<>(
                "task03", Map.of("HostA", 5D, "HostB", 2.5D, "HostC", 2D)),
            new AbstractMap.SimpleEntry<>(
                "task04", Map.of("HostA", 20D, "HostB", 10D, "HostC", 8D)),
            new AbstractMap.SimpleEntry<>(
                "task05", Map.of("HostA", 8D, "HostB", 4D, "HostC", 3.2D)));

    FitnessCalculator fitnessCalculator = new FitnessCalculatorSimple(instanceData);
    var result = fitnessCalculator.calculateComputationMatrix(new UnitParser().parseUnits("1Gf"));

    assertEquals(expected, result);
  }

  @Test
  void calculateNetworkMatrix() throws IOException {

    // Given a workflow of 5 tasks
    // Given a hosts list of 3
    InstanceData instanceData = loadCalculatorTest();

    var expected =
        Map.ofEntries(
            new AbstractMap.SimpleEntry<>("task01", Map.of("task01", 80000000L)),
            new AbstractMap.SimpleEntry<>("task02", Map.of("task01", 144000000L, "task02", 0L)),
            new AbstractMap.SimpleEntry<>("task03", Map.of("task01", 96000000L, "task03", 0L)),
            new AbstractMap.SimpleEntry<>(
                "task04", Map.of("task01", 64000000L, "task04", 128000000L)),
            new AbstractMap.SimpleEntry<>(
                "task05",
                Map.of(
                    "task02",
                    160000000L,
                    "task03",
                    192000000L,
                    "task04",
                    224000000L,
                    "task05",
                    0L)));

    FitnessCalculator fitnessCalculator = new FitnessCalculatorSimple(instanceData);
    Map<String, Map<String, Long>> result = fitnessCalculator.calculateNetworkMatrix();

    assertEquals(expected, result);
  }

  /** Uses a workflow of 10 tasks to compute the fitness suing classic DNC */
  @Test
  void CalculateFitnessSimple() {

    InstanceData instanceData = loadFitnessTest();
    FitnessCalculator fitnessCalculator = new FitnessCalculatorSimple(instanceData);

    List<PlanPair> plan =
        List.of(
            new PlanPair(instanceData.workflow().get("task01"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task04"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task03"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task02"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task05"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task06"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task07"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task09"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task08"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task10"), instanceData.hosts().get("HostC")));

    FitnessInfo result =
        fitnessCalculator.calculateFitness(new SchedulePermutationSolution(1,2,null, plan,"makespan"));

    assertEquals(210D, result.fitness().get("makespan"));
    assertEquals (679.65D, result.fitness().get("energy"), 1e-10);
  }


  @Test
  void CalculateFitnessSimpleCheck() {

    InstanceData instanceData = loadFitnessTest();
    FitnessCalculator fitnessCalculator = new FitnessCalculatorSimple(instanceData);

    List<PlanPair> plan =
        List.of(
            new PlanPair(instanceData.workflow().get("task01"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task02"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task04"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task05"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task06"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task03"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task09"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task08"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task07"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task10"), instanceData.hosts().get("HostC")));

    FitnessInfo result =
        fitnessCalculator.calculateFitness(new SchedulePermutationSolution(1,2,null, plan, "makespan"));

    assertEquals(181.5D, result.fitness().get("makespan"));
  }



  @Test
  void CalculateFitnessHeft() {

    InstanceData instanceData = loadFitnessTest();
    FitnessCalculator fitnessCalculator = new FitnessCalculatorHeft(instanceData);
    List<PlanPair> plan =
        List.of(
            new PlanPair(instanceData.workflow().get("task01"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task02"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task04"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task05"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task03"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task06"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task07"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task09"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task08"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task10"), instanceData.hosts().get("HostC")));

    FitnessInfo result =
        fitnessCalculator.calculateFitness(new SchedulePermutationSolution(1,2,null, plan, "makespan"));

    assertEquals(205D, result.fitness().get("makespan"));
  }

  @Test
  void CalculateFitnessRank() {

    InstanceData instanceData = loadFitnessTest();
    FitnessCalculator fitnessCalculator = new FitnessCalculatorRank(instanceData);
    List<PlanPair> plan =
        List.of(
            new PlanPair(instanceData.workflow().get("task01"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task02"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task04"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task05"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task03"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task06"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task07"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task09"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task08"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task10"), instanceData.hosts().get("HostC")));

    FitnessInfo result =
        fitnessCalculator.calculateFitness(new SchedulePermutationSolution(1,2,null, plan, "makespan"));

    assertEquals(209D, result.fitness().get("makespan"));
  }

  @Test
  void CalculateFitnessHeuristic() {

    InstanceData instanceData = loadFitnessTest();
    FitnessCalculator fitnessCalculator = new FitnessCalculatorHeuristic(instanceData);
    List<PlanPair> plan =
        List.of(
            new PlanPair(instanceData.workflow().get("task01"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task02"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task04"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task05"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task03"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task06"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task07"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task09"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task08"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task10"), instanceData.hosts().get("HostC")));

    FitnessInfo result =
        fitnessCalculator.calculateFitness(new SchedulePermutationSolution(1,2,null, plan, "makespan"));

    assertEquals(180.5D, result.fitness().get("makespan"));
  }

  @Test
  void CalculateFitnessMulti() {

    InstanceData instanceData = loadFitnessTest();
    FitnessCalculator fitnessCalculator =
        new FitnessCalculatorMulti(
            instanceData,
            List.of(
                new FitnessCalculatorSimple(instanceData),
                new FitnessCalculatorHeft(instanceData),
                new FitnessCalculatorRank(instanceData)),
            Collections.emptyList());

    List<PlanPair> plan =
        List.of(
            new PlanPair(instanceData.workflow().get("task01"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task04"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task03"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task02"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task05"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task06"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task07"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task09"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task08"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task10"), instanceData.hosts().get("HostC")));

    FitnessInfo result =
        fitnessCalculator.calculateFitness(new SchedulePermutationSolution(1,2,null, plan, "makespan"));

    assertEquals(209D, result.fitness().get("makespan"));
  }




  @Test
  void findHostSpeedSame() {
    var hosts =
        Map.of(
            "HostA",
            new Host("HostA", 100L, 100L, 50L, 0.9D, 0.1D),
            "HostB",
            new Host("HostB", 100L, 25L, 100L, 1.8D, 0.2D));
    var instanceData = new InstanceData(Collections.emptyMap(), hosts,0L);
    FitnessCalculator fitnessCalculator = new FitnessCalculatorSimple(instanceData);

    var expected = 100L;

    var result = fitnessCalculator.findHostSpeed(hosts.get("HostA"), hosts.get("HostA"));

    assertEquals(expected,result);
  }


  @Test
  void findHostSpeedParentSlowDisk() {
    var hosts =
        Map.of(
            "HostA",
            new Host("HostA", 100L, 100L, 50L, 0.9D, 0.1D),
            "HostB",
            new Host("HostB", 100L, 25L, 100L, 1.8D, 0.2D));
    var instanceData = new InstanceData(Collections.emptyMap(), hosts,0L);
    FitnessCalculator fitnessCalculator = new FitnessCalculatorSimple(instanceData);

    var expected = 25L;

    var result = fitnessCalculator.findHostSpeed(hosts.get("HostA"), hosts.get("HostB"));

    assertEquals(expected,result);
  }

  @Test
  void findHostSpeedParentSlowNetwork() {
    var hosts =
        Map.of(
            "HostA",
            new Host("HostA", 100L, 100L, 50L, 0.9D,0.1D),
            "HostB",
            new Host("HostB", 100L, 25L, 100L, 1.8D, 0.2D));
    var instanceData = new InstanceData(Collections.emptyMap(), hosts, 0L);
    FitnessCalculator fitnessCalculator = new FitnessCalculatorSimple(instanceData);

    var expected = 50L;

    var result = fitnessCalculator.findHostSpeed(hosts.get("HostB"), hosts.get("HostA"));

    assertEquals(expected,result);
  }

  @Test
  void findHostSpeedCurrentSlowNetwork() {
    var hosts =
        Map.of(
            "HostA",
            new Host("HostA", 100L, 100L, 50L,0.9D, 0.1D),
            "HostB",
            new Host("HostB", 100L, 10L, 25L, 1.8D,0.2D));
    var instanceData = new InstanceData(Collections.emptyMap(), hosts, 0L);
    FitnessCalculator fitnessCalculator = new FitnessCalculatorSimple(instanceData);

    var expected = 25L;

    var result = fitnessCalculator.findHostSpeed(hosts.get("HostB"), hosts.get("HostA"));

    assertEquals(expected,result);
  }
}
