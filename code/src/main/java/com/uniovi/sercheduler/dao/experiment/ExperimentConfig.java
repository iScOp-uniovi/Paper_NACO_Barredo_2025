package com.uniovi.sercheduler.dao.experiment;

import java.util.List;

public record ExperimentConfig(
    List<String> fitness,
    List<String> workflows,
    int minHosts,
    int maxHosts,
    int hostIncrement,
    String referenceSpeed,
    int independentRuns,
    List<String> objectives,
    boolean jmetalAnalysis) {}
