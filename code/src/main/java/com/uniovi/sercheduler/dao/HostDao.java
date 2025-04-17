package com.uniovi.sercheduler.dao;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the host object from a custom format.
 *
 * @param name Unique name of the host.
 * @param cpuSpeed Units of compute per second
 * @param cores Number of cores.
 * @param diskSpeed Speed of the disk.
 * @param networkSpeed Bandwidth.
 */
public record HostDao(
    String name,
    @JsonProperty("cpu_speed") String cpuSpeed,
    Integer cores,
    @JsonProperty("disk_speed") String diskSpeed,
    @JsonProperty("network_speed") String networkSpeed,
    @JsonProperty("energy_cost") Double energyCost,
    @JsonProperty("energy_cost_stand_by") Double energyCostStandBy) {}
