package com.uniovi.sercheduler.dto.analysis;

public class MultiResult {

  short simple;
  short heft;
  short rank;
  double makespan;

  public MultiResult() {
    this.simple = 0;
    this.heft = 0;
    this.rank = 0;
    this.makespan = 0;
  }

  public short getSimple() {
    return simple;
  }

  public void setSimple(short simple) {
    this.simple = simple;
  }

  public short getHeft() {
    return heft;
  }

  public void setHeft(short heft) {
    this.heft = heft;
  }

  public short getRank() {
    return rank;
  }

  public void setRank(short rank) {
    this.rank = rank;
  }

  public double getMakespan() {
    return makespan;
  }

  public void setMakespan(double makespan) {
    this.makespan = makespan;
  }
}
