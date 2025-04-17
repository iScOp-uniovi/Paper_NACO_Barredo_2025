package com.uniovi.sercheduler.service;

import com.uniovi.sercheduler.dto.Host;
import com.uniovi.sercheduler.dto.Task;

/**
 * Defines a pair of Task and Host for a Schedule.
 *
 * @param task Task to schedule.
 * @param host Host to execute the task with.
 */
public record PlanPair(Task task, Host host) {

  @Override
  public String toString() {
    return "[" + task + ", " + host + ']';
  }
}
