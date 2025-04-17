package com.uniovi.sercheduler.dao;

import java.util.List;

/**
 * Top object of the hosts file.
 *
 * @param hosts List of all the hosts.
 */
public record MachinesDao(List<HostDao> hosts) {}
