package com.uniovi.sercheduler.dao;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/**
 * Defines a task in json format from SWF Commons.
 *
 * @param files All the files from a task. (Input, output and staging).
 * @param name Name of the task.
 * @param parents All the direct predecessors of the task.
 * @param children All the direct successors of the task.
 * @param runtime Time need it to run the task (It's relative to a reference CPU speed).
 */
public record TaskDao(
    List<TaskFileDao> files,
    String name,
    List<String> parents,
    List<String> children,
    @JsonAlias("runtimeInSeconds") Double runtime) {}
