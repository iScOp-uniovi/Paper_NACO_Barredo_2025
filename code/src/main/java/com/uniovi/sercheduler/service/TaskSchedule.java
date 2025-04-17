package com.uniovi.sercheduler.service;

import com.uniovi.sercheduler.dto.Host;
import com.uniovi.sercheduler.dto.Task;

/**
 * Schedule for each task.
 *
 * @param task Task to schedule.
 * @param ast When does the task start in the timeline.
 * @param eft When does the task ends in the timeline.
 * @param host Where the task runs.
 */
public record TaskSchedule(Task task, Double ast, Double eft, Host host) {}
