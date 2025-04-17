package com.uniovi.sercheduler.dao;


public enum Objective {
  MAKESPAN("makespan"),
  ENERGY("energy");

  public final String objectiveName;

  Objective(String objectiveName) {
    this.objectiveName = objectiveName;
  }

  public static Objective of(String objectiveName) {

    for (var objective : Objective.values()) {
      if (objective.objectiveName.equals(objectiveName)) {
        return objective;
      }
    }
    throw new IllegalArgumentException("Invalid objective value: " + objectiveName);
  }
}
