package com.uniovi.sercheduler.dto;

/** Defines a server with the main specs. */
public class Host {

  /**
   * Full constructor.
   *
   * @param name Name of the host.
   * @param flops Number of operations per second.
   * @param diskSpeed disk speed in bits.
   * @param networkSpeed network speed in bits.
   * @param energyCost The cost per second of the host.
   */
  public Host(String name, Long flops, Long diskSpeed, Long networkSpeed, Double energyCost, Double energyCostStandBy) {
    this.name = name;
    this.flops = flops;
    this.diskSpeed = diskSpeed;
    this.networkSpeed = networkSpeed;
    this.energyCost = energyCost;
    this.energyCostStandBy = energyCostStandBy;
  }

  private final String name;

  private final Long flops;

  private final Long diskSpeed;

  private final Long networkSpeed;

  private final Double energyCost;

  private final Double energyCostStandBy;


  public String getName() {
    return name;
  }

  public Long getFlops() {
    return flops;
  }

  public Long getDiskSpeed() {
    return diskSpeed;
  }

  public Long getNetworkSpeed() {
    return networkSpeed;
  }

  public Double getEnergyCost() {
    return energyCost;
  }

  public Double getEnergyCostStandBy() {
    return energyCostStandBy;
  }

  @Override
  public String toString() {
    return name;
  }
}
