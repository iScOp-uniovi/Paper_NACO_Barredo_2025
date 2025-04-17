package com.uniovi.sercheduler.service;

import java.util.List;
import java.util.Map;

/**
 * Contains information about the fitness of an evaluated plan.
 *
 * @param fitness Map containing all possible fitness.
 * @param schedule The final schedule of the plan.
 */
public record FitnessInfo(Map<String, Double> fitness, List<TaskSchedule> schedule, String fitnessFunction) {}
