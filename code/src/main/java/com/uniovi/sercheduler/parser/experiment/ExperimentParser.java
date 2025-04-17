package com.uniovi.sercheduler.parser.experiment;

import com.uniovi.sercheduler.dto.wrench.TaskWrench;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExperimentParser {

  public List<List<TaskWrench>> readVar(String filePath) {

    var file = new File(filePath);

      List<List<TaskWrench>> experiment = new ArrayList<>();



    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = br.readLine()) != null) {
          List<TaskWrench> taskHostPairs = new ArrayList<>();

          String[] pairs = line.split("],\\["); // Split by `],[`

        for (String pair : pairs) {
          pair = pair.replace("[", "").replace("]", ""); // Remove square brackets
          String[] parts = pair.split(","); // Split by comma
          if (parts.length == 2) {
            taskHostPairs.add(new TaskWrench(parts[0].trim(), parts[1].trim()));
          }
        }
        experiment.add(taskHostPairs);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return experiment;
  }
}
