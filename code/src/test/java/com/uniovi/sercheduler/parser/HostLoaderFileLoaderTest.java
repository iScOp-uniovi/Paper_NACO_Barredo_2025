package com.uniovi.sercheduler.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.uniovi.sercheduler.util.UnitParser;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class HostLoaderFileLoaderTest {

  HostFileLoader hostFileLoader = new HostFileLoader();

  @Test
  void loadHost() throws IOException {

    var hostsJson = new ClassPathResource("hosts_test.json").getFile();
    var hostsDao = hostFileLoader.readFromFile(hostsJson);
    var hosts = hostFileLoader.load(hostsDao);

    assertEquals(32000000L, hosts.get("HostA").getDiskSpeed());
    assertEquals(8000000L, hosts.get("HostA").getNetworkSpeed());
    assertEquals(8000000L, hosts.get("HostC").getNetworkSpeed());
    assertEquals(1.8D, hosts.get("HostA").getEnergyCost());
    assertEquals(0.9D, hosts.get("HostC").getEnergyCost());
    assertEquals(0.2D, hosts.get("HostA").getEnergyCostStandBy());
    assertEquals(0.1D, hosts.get("HostC").getEnergyCostStandBy());
  }
}
