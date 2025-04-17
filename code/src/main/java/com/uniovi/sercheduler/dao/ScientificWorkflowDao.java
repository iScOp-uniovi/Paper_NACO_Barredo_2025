package com.uniovi.sercheduler.dao;

/**
 * Top object of workflow json in SWF commons format.
 *
 * @param name The name of the workflow.
 * @param schemaVersion The version of the json file.
 * @param workflowDao The workflow object containing the tasks.
 */
public record ScientificWorkflowDao(String name, String schemaVersion, WorkflowDao workflow) {}
