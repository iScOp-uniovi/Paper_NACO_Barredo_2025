package com.uniovi.sercheduler.dao;

import java.util.List;

/**
 * Defines a workflow in SWF commons format available in json.
 *
 * @param makespan Defines the makespan of the workflow.
 * @param tasks Contains all the tasks of the workflow.
 */
public record WorkflowDao(Double makespan, List<TaskDao> tasks) {}
