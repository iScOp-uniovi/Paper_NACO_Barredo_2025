package com.uniovi.sercheduler.dto;

import java.util.Objects;

/** Defines a file of a task. */
public class TaskFile {

  private String name;
  private Direction link;
  private Long size;

  /**
   * Full constructor.
   *
   * @param name The name of the file.
   * @param link If the file is an input or output.
   * @param size The size of the file in bits.
   */
  public TaskFile(String name, Direction link, Long size) {
    this.name = name;
    this.link = link;
    this.size = size;
  }

  public Long getSize() {
    return size;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TaskFile taskFile = (TaskFile) o;

    if (!Objects.equals(name, taskFile.name)) {
      return false;
    }
    if (link != taskFile.link) {
      return false;
    }
    return Objects.equals(size, taskFile.size);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (link != null ? link.hashCode() : 0);
    result = 31 * result + (size != null ? size.hashCode() : 0);
    return result;
  }
}
