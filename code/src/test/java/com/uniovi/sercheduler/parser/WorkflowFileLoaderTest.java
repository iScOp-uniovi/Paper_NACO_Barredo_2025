package com.uniovi.sercheduler.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

public class WorkflowFileLoaderTest {

  WorkflowFileLoader workflowFileLoader = new WorkflowFileLoader();

  @Test
  void workflowJsonShouldLoad() throws IOException {

    var workflowJson = new ClassPathResource("workflow_test.json").getFile();
    var workflowDao = workflowFileLoader.readFromFile(workflowJson);
    var workflow = workflowFileLoader.load(workflowDao);

    assertEquals(10, workflow.size());

    assertEquals(5, workflow.get("task01").getChildren().size());

    // We need to check if the children have the up and down references
    assertEquals(
        workflow.get("task02"),
        workflow.get("task01").getChildren().stream()
            .filter(c -> "task02".equals(c.getName()))
            .findAny()
            .orElseThrow());
  }


  @Test
  void workflowXmlShouldLoad() throws IOException {

    var workflowJson = new ClassPathResource("workflow_test.xml").getFile();
    var workflowDao = workflowFileLoader.readFromFile(workflowJson);
    var workflow = workflowFileLoader.load(workflowDao);

    assertEquals(25, workflow.size());

    assertEquals(1, workflow.get("ID00005").getChildren().size());
    assertEquals(2, workflow.get("ID00005").getParents().size());

    // We need to check if the children have the up and down references
    assertEquals(
        workflow.get("ID00005"),
        workflow.get("ID00000").getChildren().stream()
            .filter(c -> "ID00005".equals(c.getName()))
            .findAny()
            .orElseThrow());
  }
  @Test
  void loadMontage() throws IOException{

    var workflowJson = new ClassPathResource("montage.json").getFile();
    var workflowDao = workflowFileLoader.readFromFile(workflowJson);
    var workflow = workflowFileLoader.load(workflowDao);
    assertEquals(58, workflow.size());

  }

  @Test
  void load1000genome() throws IOException{

    var workflowJson = new ClassPathResource("1000genome.json").getFile();
    var workflowDao = workflowFileLoader.readFromFile(workflowJson);
    var workflow = workflowFileLoader.load(workflowDao);
    assertEquals(52, workflow.size());

  }

  @Test
  void loadCycles() throws IOException{

    var workflowJson = new ClassPathResource("cycles.json").getFile();
    var workflowDao = workflowFileLoader.readFromFile(workflowJson);
    var workflow = workflowFileLoader.load(workflowDao);
    assertEquals(67, workflow.size());

  }

  @Test
  void loadSoyKb() throws IOException{

    var workflowJson = new ClassPathResource("soykb.json").getFile();
    var workflowDao = workflowFileLoader.readFromFile(workflowJson);
    var workflow = workflowFileLoader.load(workflowDao);
    assertEquals(96, workflow.size());

  }

  @Test
  void loadSraSearch() throws IOException{

    var workflowJson = new ClassPathResource("srasearch.json").getFile();
    var workflowDao = workflowFileLoader.readFromFile(workflowJson);
    var workflow = workflowFileLoader.load(workflowDao);
    assertEquals(22, workflow.size());

  }
}
