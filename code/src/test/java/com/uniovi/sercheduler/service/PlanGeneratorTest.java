package com.uniovi.sercheduler.service;

import static org.junit.jupiter.api.Assertions.*;

import com.uniovi.sercheduler.dto.InstanceData;
import com.uniovi.sercheduler.util.LoadTestInstanceData;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PlanGeneratorTest {

  @Test
  void generatePlan() {
    InstanceData instanceData = LoadTestInstanceData.loadCalculatorTest();

    PlanGenerator planGenerator = new PlanGenerator(new Random(1L), instanceData);

    var expected =
        List.of(
            new PlanPair(
                instanceData.workflow().get("task01"), instanceData.hosts().get("HostC")),
            new PlanPair(
                instanceData.workflow().get("task03"), instanceData.hosts().get("HostB")),
            new PlanPair(
                instanceData.workflow().get("task02"), instanceData.hosts().get("HostC")),
            new PlanPair(
                instanceData.workflow().get("task04"), instanceData.hosts().get("HostC")),
            new PlanPair(
                instanceData.workflow().get("task05"), instanceData.hosts().get("HostC")));

    var result = planGenerator.generatePlan();

    assertEquals(expected, result);

    // Now ge need to generate 1000 schedules to verify we always have topology order.

    for (int i = 0; i < 1000; i++) {
      result = planGenerator.generatePlan();
      assertEquals("task01", result.get(0).task().getName());
      assertEquals("task05", result.get(result.size()-1).task().getName());
    }
  }
}
