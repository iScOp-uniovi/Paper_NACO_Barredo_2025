package com.uniovi.sercheduler.util;

import com.uniovi.sercheduler.dto.analysis.MultiResult;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CsvUtils {

  public static void writeMapsToCsvTransposed(
      String filename, List<Map<String, Integer>> maps, List<String> mapNames) throws IOException {
    if (maps.size() != mapNames.size()) {
      throw new IllegalArgumentException("The number of maps and map names must match.");
    }

    // Collect all unique keys from all maps
    Set<String> allKeys = new LinkedHashSet<>();
    for (Map<String, Integer> map : maps) {
      allKeys.addAll(map.keySet());
    }

    try (FileWriter csvWriter = new FileWriter(filename)) {
      // Write the header row with all keys
      csvWriter.append("Map");
      for (String key : allKeys) {
        csvWriter.append(",").append(key);
      }
      csvWriter.append("\n");

      // Iterate over each map and write the map name and values for each key
      for (int i = 0; i < maps.size(); i++) {
        csvWriter.append(mapNames.get(i)); // Use the provided map name
        Map<String, Integer> map = maps.get(i);
        for (String key : allKeys) {
          csvWriter.append(",").append(map.getOrDefault(key, 0).toString());
        }
        csvWriter.append("\n");
      }

      csvWriter.flush();
    }
  }

  public static void exportToCsv(String fileName, List<MultiResult> results) {
    try (FileWriter writer = new FileWriter(fileName)) {
      // Write the header
      writer.append("Eval,Simple,Heft,Rank,Makespan\n");

      // Write the data rows
      int evalCounter = 1; // Starting eval number
      for (MultiResult result : results) {
        writer
            .append(Integer.toString(evalCounter++))
            .append(',')
            .append(Short.toString(result.getSimple()))
            .append(',')
            .append(Short.toString(result.getHeft()))
            .append(',')
            .append(Short.toString(result.getRank()))
            .append(',')
            .append(Double.toString(result.getMakespan()))
            .append('\n');
      }

      System.out.println("CSV file was created successfully.");
    } catch (IOException e) {
      System.out.println("An error occurred while writing the CSV file.");
      e.printStackTrace();
    }
  }
}
