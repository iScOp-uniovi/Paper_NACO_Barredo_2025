package com.uniovi.sercheduler.dto;

import java.util.List;

/** Holds the information of all the files and has pre-calculated the size of all of them. */
public class FileList {
  private List<TaskFile> files;
  private Long sizeInBits;

  /**
   * Full constructor.
   *
   * @param files The list of files.
   * @param sizeInBits The sum of all the files size in bits.
   */
  public FileList(List<TaskFile> files, Long sizeInBits) {
    this.files = files;
    this.sizeInBits = sizeInBits;
  }

  public List<TaskFile> getFiles() {
    return files;
  }

  public Long getSizeInBits() {
    return sizeInBits;
  }
}
