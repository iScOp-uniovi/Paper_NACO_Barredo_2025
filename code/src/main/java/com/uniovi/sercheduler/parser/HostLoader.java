package com.uniovi.sercheduler.parser;

import com.uniovi.sercheduler.dao.HostDao;
import com.uniovi.sercheduler.dao.MachinesDao;
import com.uniovi.sercheduler.dto.Host;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Defines a generic host loader.
 */
public interface HostLoader {

  public Map<String, Host> load(List<HostDao> hostsDao);

  public List<HostDao> readFromFile(File hostsJson);
}
