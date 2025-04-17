package com.uniovi.sercheduler.dto.wrench;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for wrench json about hosts information.
 *
 * @param name Name of the host.
 * @param cores Number of cores.
 * @param cpuSpeed Speed of the cpu.
 * @param diskSpeed Speed of the disk.
 * @param networkSpeed Speed of the network.
 */
public record HostWrench(
    String name,
    String cores,
    @JsonProperty("cpu_speed") String cpuSpeed,
    @JsonProperty("disk_speed") String diskSpeed,
    @JsonProperty("network_speed") String networkSpeed,
    @JsonProperty("energy_cost") String energyCost) {}
