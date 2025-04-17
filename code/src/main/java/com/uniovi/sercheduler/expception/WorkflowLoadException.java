package com.uniovi.sercheduler.expception;

/**
 * Exception to raise when the workflow can't be loaded.
 */
public class WorkflowLoadException extends RuntimeException {

  public WorkflowLoadException(String file, Throwable e) {
    super("Can't load the json workflow from: " + file, e);
  }
}
