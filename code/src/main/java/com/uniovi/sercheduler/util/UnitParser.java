package com.uniovi.sercheduler.util;

import com.uniovi.sercheduler.expception.ParseUnitsException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Util class for parsing units. */

public class UnitParser {

  static final String UNIT_REGEX = "(\\d+[\\\\.\\d+]*)\\s*([A-z]+)";

  /**
   * Generates a numeric response from a human-readable format.
   *
   * @param units Units in human-readable format.
   * @return The units in the least unit possible, normally bits.
   */
  public static Long parseUnits(String units) {

    Pattern pattern = Pattern.compile(UNIT_REGEX);
    Matcher matcher = pattern.matcher(units);
    boolean matches = matcher.find();
    if (!matches) {
      throw new ParseUnitsException(units);
    }
    var number = Double.valueOf(matcher.group(1));
    var unit = matcher.group(2);

    return (long) (number * Units.valueOf(unit).unitBase);
  }
}

