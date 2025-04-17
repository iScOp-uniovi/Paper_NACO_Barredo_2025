package com.uniovi.sercheduler.dto;

import java.util.List;

/** Define a task to operate in the algorithm. */
public class Task {

  private String name;
  private Double runtime;
  private List<Task> parents;
  private List<Task> children;
  private FileList input;
  private FileList output;

  /**
   * Full constructor.
   *
   * @param name The name of task.
   * @param runtime time need it to execute the task in seconds.
   * @param parents All direct successors.
   * @param children All direct ancestor.
   * @param input Files need it to start the task.
   * @param output Files generated at the end of the task.
   */
  public Task(
      String name,
      Double runtime,
      List<Task> parents,
      List<Task> children,
      FileList input,
      FileList output) {
    this.name = name;
    this.runtime = runtime;
    this.parents = parents;
    this.children = children;
    this.input = input;
    this.output = output;
  }

  public String getName() {
    return name;
  }

  public Double getRuntime() {
    return runtime;
  }

  public void setParents(List<Task> parents) {
    this.parents = parents;
  }

  public void setChildren(List<Task> children) {
    this.children = children;
  }

  public List<Task> getChildren() {
    return children;
  }


  public List<Task> getParents() {
    return parents;
  }

  public FileList getInput() {
    return input;
  }

  public FileList getOutput() {
    return output;
  }

  @Override
  public String toString() {
    return name;
  }
}
