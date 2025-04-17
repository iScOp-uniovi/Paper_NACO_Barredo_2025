package com.uniovi.sercheduler.expception;

/**
 * Exception to raise when the workflow can't be loaded.
 */
public class HostLoadException extends RuntimeException {

  public HostLoadException(String file, Throwable e) {
    super("Can't load the json hosts from: " + file, e);
  }
}
