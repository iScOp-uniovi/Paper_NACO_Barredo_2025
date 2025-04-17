package com.uniovi.sercheduler.dto.wrench;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Schedule object to be used on wrench.
 *
 * @param hosts Contains the hosts used.
 * @param tasks List of task and host it should be mapped.
 */
public record ScheduleWrench(
    @JsonProperty("machines") List<HostWrench> hosts,
    @JsonProperty("schedule") List<TaskWrench> tasks,
    @JsonProperty("reference_speed") String referenceSpeed) {}
