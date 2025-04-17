package com.uniovi.sercheduler.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniovi.sercheduler.dao.HostDao;
import com.uniovi.sercheduler.dao.MachinesDao;
import com.uniovi.sercheduler.dto.Host;
import com.uniovi.sercheduler.expception.HostLoadException;
import com.uniovi.sercheduler.util.UnitParser;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/** Loader of hosts from files. */
@Primary
@Service
public class HostFileLoader implements HostLoader {




  /**
   * Parse a list of hosts to easier to use format.
   *
   * @param hostsDao List of hosts to parse.
   * @return The parsed hosts ready for use in the GA.
   */
  @Override
  public Map<String, Host> load(List<HostDao> hostsDao) {

    return hostsDao.stream()
        .map(
            h ->
                new Host(
                    h.name(),
                    UnitParser.parseUnits(h.cpuSpeed()) * h.cores(),
                    UnitParser.parseUnits(h.diskSpeed()),
                    UnitParser.parseUnits(h.networkSpeed()),
                        h.energyCost(),
                        h.energyCostStandBy()))
        .collect(Collectors.toMap(Host::getName, Function.identity()));
  }

  /**
   * Read the hosts from a file.
   *
   * @param hostsJson File containing the hosts' definition.
   * @return The list of hosts contained in the file.
   */
  @Override
  public List<HostDao> readFromFile(File hostsJson) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      return mapper.readValue(hostsJson, MachinesDao.class).hosts();
    } catch (IOException e) {
      throw new HostLoadException(hostsJson.getName(), e);
    }
  }
}
