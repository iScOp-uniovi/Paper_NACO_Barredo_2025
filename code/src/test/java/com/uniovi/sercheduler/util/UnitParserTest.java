package com.uniovi.sercheduler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnitParserTest {

  UnitParser unitParser = new UnitParser();

  @Test
  void parseUnits() {

    var expected = 8192L;
    var unit = "1KiB";

    var result = unitParser.parseUnits(unit);

    assertEquals(expected, result);
  }
}
