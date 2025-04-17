package com.uniovi.sercheduler.dto;

/** Defines the direction of a file. */
public enum Direction {
  INPUT("input"),
  OUTPUT("output");

  public final String link;

  Direction(String link) {
    this.link = link;
  }

  Direction of(String link) {
    for (var direction : Direction.values()) {
      if (direction.link.equals(link)) {
        return direction;
      }
    }
    throw new IllegalArgumentException("Invalid link value: " + link);
  }
}
