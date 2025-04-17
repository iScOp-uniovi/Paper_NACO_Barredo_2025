package com.uniovi.sercheduler.dto;

/**
 * Holds information about the run.
 *
 * @param workflow The executed workflow.
 * @param hosts The infrastructure.
 * @param fitness The fitness function used.
 * @param makespan The makespan.
 * @param time The time it took to execute.
 */
public record BenchmarkData(
    String workflow, Integer hosts, String fitness, Double makespan, Double energy, Long time) {}
