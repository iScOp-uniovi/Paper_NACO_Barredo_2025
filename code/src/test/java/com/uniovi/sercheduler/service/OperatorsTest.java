package com.uniovi.sercheduler.service;

import static com.uniovi.sercheduler.util.LoadTestInstanceData.loadFitnessTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.uniovi.sercheduler.dto.InstanceData;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class OperatorsTest {

  @Test
  void mutate() {
    InstanceData instanceData = loadFitnessTest();
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


    List<PlanPair> expected =
        List.of(
            new PlanPair(instanceData.workflow().get("task01"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task02"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task04"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task05"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task03"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task06"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task08"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task07"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task09"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task10"), instanceData.hosts().get("HostC")));


    Operators operators = new Operators(instanceData, new Random(2L));

    var result = operators.mutate(plan);

    assertEquals(expected,result);

  }

  @Test
  void mutateRight() {
    InstanceData instanceData = loadFitnessTest();
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


    List<PlanPair> expected =
            List.of(
                    new PlanPair(instanceData.workflow().get("task01"), instanceData.hosts().get("HostA")),
                    new PlanPair(instanceData.workflow().get("task02"), instanceData.hosts().get("HostA")),
                    new PlanPair(instanceData.workflow().get("task05"), instanceData.hosts().get("HostC")),
                    new PlanPair(instanceData.workflow().get("task03"), instanceData.hosts().get("HostC")),
                    new PlanPair(instanceData.workflow().get("task06"), instanceData.hosts().get("HostB")),
                    new PlanPair(instanceData.workflow().get("task04"), instanceData.hosts().get("HostB")),
                    new PlanPair(instanceData.workflow().get("task07"), instanceData.hosts().get("HostA")),
                    new PlanPair(instanceData.workflow().get("task09"), instanceData.hosts().get("HostB")),
                    new PlanPair(instanceData.workflow().get("task08"), instanceData.hosts().get("HostC")),
                    new PlanPair(instanceData.workflow().get("task10"), instanceData.hosts().get("HostC")));


    Operators operators = new Operators(instanceData, new Random(4L));

    var result = operators.mutate(plan);

    assertEquals(expected,result);

  }


  @Test
  void doCrossOver() {
    InstanceData instanceData = loadFitnessTest();
    List<PlanPair> plan1 =
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


    List<PlanPair> plan2 =
        List.of(
            new PlanPair(instanceData.workflow().get("task01"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task03"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task02"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task05"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task04"), instanceData.hosts().get("HostC")),

            new PlanPair(instanceData.workflow().get("task06"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task09"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task07"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task08"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task10"), instanceData.hosts().get("HostC")));



    List<PlanPair> expected =
        List.of(
            new PlanPair(instanceData.workflow().get("task01"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task02"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task04"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task05"), instanceData.hosts().get("HostC")),
            new PlanPair(instanceData.workflow().get("task03"), instanceData.hosts().get("HostC")),

            new PlanPair(instanceData.workflow().get("task06"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task09"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task07"), instanceData.hosts().get("HostA")),
            new PlanPair(instanceData.workflow().get("task08"), instanceData.hosts().get("HostB")),
            new PlanPair(instanceData.workflow().get("task10"), instanceData.hosts().get("HostC")));


    Operators operators = new Operators(instanceData, new Random(1L));

    var result = operators.doCrossover(plan1, plan2);

    assertEquals(expected,result);

  }





}
