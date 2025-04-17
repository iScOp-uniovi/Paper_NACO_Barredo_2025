package com.uniovi.sercheduler.dto.wrench;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the task to and specific host.
 *
 * @param taskId The name of the task.
 * @param host The host to use.
 */
public record TaskWrench(@JsonProperty("task_id") String taskId, String host) {}
