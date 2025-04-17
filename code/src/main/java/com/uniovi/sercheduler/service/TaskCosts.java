package com.uniovi.sercheduler.service;

/**
 * The cost of executing the task.
 *
 * @param diskReadStaging Time it takes to read stag files.
 * @param diskWrite Time it takes to write the output files.
 * @param eft When does the task ends in the timeline.
 * @param taskCommunications Time to transfer all the info.
 */
public record TaskCosts(
    Double diskReadStaging, Double diskWrite, Double eft, Double taskCommunications, Double ast) {}
