package com.uniovi.sercheduler.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Defines a structure to hold information related to the instance to solve.
 *
 * @param workflow Map of tasks to solve.
 * @param hosts The infrastructure used to run the workflow.
 */
public record InstanceData(Map<String, Task> workflow, Map<String, Host> hosts, Long referenceFlops)
    implements Serializable {}
