package com.uniovi.sercheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniovi.sercheduler.dao.HostDao;
import com.uniovi.sercheduler.dto.wrench.HostWrench;
import com.uniovi.sercheduler.dto.wrench.ScheduleWrench;
import com.uniovi.sercheduler.dto.wrench.TaskWrench;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service to export a schedule to a valid Wrench format. */
@Service
public class ScheduleExporter {

  /**
   * Creates a json compatible with wrench.
   *
   * @param fitnessInfo Information about the entity to export.
   * @param hosts The hosts in the valid format.
   * @param scheduleJson The file to use.
   */
  public void generateJsonSchedule(
      FitnessInfo fitnessInfo, List<HostDao> hosts, File scheduleJson, String referenceSpeed) {

    var hostsWrench =
        hosts.stream()
            .map(
                h ->
                    new HostWrench(
                        h.name(),
                        h.cores().toString(),
                        h.cpuSpeed(),
                        h.diskSpeed(),
                        h.networkSpeed(),
                        h.energyCost().toString()))
            .toList();

    var tasks =
        fitnessInfo.schedule().stream()
            .map(s -> new TaskWrench(s.task().getName(), s.host().getName()))
            .toList();

    var scheduleWrench = new ScheduleWrench(hostsWrench, tasks, referenceSpeed);

    var objectMapper = new ObjectMapper();
    try {
      objectMapper.writeValue(scheduleJson, scheduleWrench);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
