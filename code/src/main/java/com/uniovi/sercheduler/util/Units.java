package com.uniovi.sercheduler.util;

/** Gives all units in bits, uses shift left to calculate some units. */
public enum Units {

  // Binary prefixes
  Bits(1L),

  Bytes(Bits.unitBase * 8),
  KiB(Bytes.unitBase << 10),
  MiB(KiB.unitBase << 10),
  GiB(MiB.unitBase << 10),
  TiB(GiB.unitBase << 10),
  PiB(TiB.unitBase << 10),
  EiB(PiB.unitBase << 10),

  // SI prefixes
  KB(Bytes.unitBase * 1000),
  MB(KB.unitBase * 1000),
  GB(MB.unitBase * 1000),
  TB(GB.unitBase * 1000),
  PB(TB.unitBase * 1000),
  EB(PB.unitBase * 1000),

  // Network speeds in bytes
  bps(1L),
  Kbps(bps.unitBase * 1000),
  Mbps(Kbps.unitBase * 1000),
  Gbps(Mbps.unitBase * 1000),
  Tbps(Gbps.unitBase * 1000),
  Pbps(Tbps.unitBase * 1000),
  Ebps(Pbps.unitBase * 1000),

  // Network speeds in bits
  Bps(bps.unitBase * 8),
  KBps(Bps.unitBase * 1000),
  MBps(KBps.unitBase * 1000),
  GBps(MBps.unitBase * 1000),
  TBps(GBps.unitBase * 1000),
  PBps(TBps.unitBase * 1000),
  EBps(PBps.unitBase * 1000),

  // CPU Speeds
  Flops(1L),
  Kf(Flops.unitBase * 1000),
  Mf(Kf.unitBase * 1000),
  Gf(Mf.unitBase * 1000),
  Tf(Gf.unitBase * 1000),
  Pf(Tf.unitBase * 1000),
  Ef(Pf.unitBase * 1000);

  public final Long unitBase;

  Units(Long unitBase) {
    this.unitBase = unitBase;
  }
}
