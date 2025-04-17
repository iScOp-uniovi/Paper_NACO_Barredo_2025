package com.uniovi.sercheduler.expception;

/** Exception for errors loading the experiment config file. */
public class ExperimentConfigLoadException extends RuntimeException {

  public ExperimentConfigLoadException(String file, Throwable e) {
    super("Can't load file experiment config from file: " + file, e);
  }
}
