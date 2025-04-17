package com.uniovi.sercheduler.expception;

/** Exception to raise when it can't parse the units. */
public class ParseUnitsException extends RuntimeException {

  public ParseUnitsException(String unit) {
    super("Can't parse the unit, is not a supported unit, attempted to parse: " + unit);
  }
}
