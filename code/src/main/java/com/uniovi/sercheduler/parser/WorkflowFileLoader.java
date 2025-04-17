package com.uniovi.sercheduler.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.uniovi.sercheduler.dao.ScientificWorkflowDao;
import com.uniovi.sercheduler.dao.TaskDao;
import com.uniovi.sercheduler.dao.TaskFileDao;
import com.uniovi.sercheduler.dao.WorkflowDao;
import com.uniovi.sercheduler.dao.dax.ChildDao;
import com.uniovi.sercheduler.dao.dax.DagDao;
import com.uniovi.sercheduler.dao.dax.ParentDao;
import com.uniovi.sercheduler.dto.Direction;
import com.uniovi.sercheduler.dto.FileList;
import com.uniovi.sercheduler.dto.Task;
import com.uniovi.sercheduler.dto.TaskFile;
import com.uniovi.sercheduler.expception.WorkflowLoadException;
import com.uniovi.sercheduler.util.Pair;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/** Loader of workflows from files. */
@Primary
@Service
public class WorkflowFileLoader implements WorkflowLoader {

  /**
   * Loads the workflow from a file.
   *
   * @param workflowDao Object in SWF commons format.
   * @return The List of files.
   */
  @Override
  public Map<String, Task> load(WorkflowDao workflowDao) {

    /*
    We need to generate a map that given a task says the children it has because in some
    cases this information is missing from the original json so is not reliable.
    */

    // Contains the children(value) of each task(key) is the reverse of the parents map.
    var childrenMap =
        workflowDao.tasks().stream()
            .map(t -> new Pair<>(t.name(), t.parents()))
            .flatMap(pair -> pair.value().stream().map(parent -> new Pair<>(parent, pair.key())))
            .collect(
                Collectors.groupingBy(
                    Pair::key, HashMap::new, Collectors.mapping(Pair::value, Collectors.toList())));

    Map<String, Task> workflowTmp =
        workflowDao.tasks().stream()
            .map(
                t -> {
                  var inputFiles =
                      t.files().stream()
                          .filter(f -> f.link().equals(Direction.INPUT.link))
                          .map(f -> new TaskFile(f.name(), Direction.INPUT, f.size() * 8))
                          .toList();

                  var outputFiles =
                      t.files().stream()
                          .filter(f -> f.link().equals(Direction.OUTPUT.link))
                          .map(f -> new TaskFile(f.name(), Direction.OUTPUT, f.size() * 8))
                          .toList();

                  // Need to calculate the total bits transferred

                  var inputBits = inputFiles.stream().map(TaskFile::getSize).reduce(0L, Long::sum);
                  var outputBits =
                      outputFiles.stream().map(TaskFile::getSize).reduce(0L, Long::sum);
                  return new Task(
                      t.name(),
                      t.runtime(),
                      Collections.emptyList(),
                      Collections.emptyList(),
                      new FileList(inputFiles, inputBits),
                      new FileList(outputFiles, outputBits));
                })
            .collect(Collectors.toMap(Task::getName, Function.identity()));

    // In the second iteration we can assign the already created tasks.
    return workflowDao.tasks().stream()
        .map(
            t -> {
              var task = workflowTmp.get(t.name());
              var parents = t.parents().stream().map(workflowTmp::get).toList();
              var children =
                  childrenMap.getOrDefault(t.name(), Collections.emptyList()).stream()
                      .map(workflowTmp::get)
                      .toList();

              task.setParents(parents);
              task.setChildren(children);

              return task;
            })
        .collect(Collectors.toMap(Task::getName, Function.identity()));
  }

  /**
   * Reads a Json workflow from a file.
   *
   * @param workflowJson Json containing a workflow.
   * @return The parsed object.
   */
  @Override
  public WorkflowDao readFromFile(File workflowJson) {

    // Need to find the extension to know which parser to use.

    var extension = FilenameUtils.getExtension(workflowJson.getName());

    return switch (extension) {
      case "json" -> readFromJson(workflowJson);
      case "xml" -> readFromXml(workflowJson);
      default -> throw new IllegalStateException("Unexpected value: " + extension);
    };
  }

  private WorkflowDao readFromXml(File workflowJson) {

    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      var dag = xmlMapper.readValue(workflowJson, DagDao.class);

      // Contains the parents(value) of each task(key)
      var parents =
          dag.children().stream()
              .collect(
                  Collectors.toMap(
                      ChildDao::ref, c -> c.parents().stream().map(ParentDao::ref).toList()));

      // Contains the children(value) of each task(key) is the reverse of the parents map.
      var children =
          parents.entrySet().stream()
              .flatMap(e -> e.getValue().stream().map(p -> new Pair<>(p, e.getKey())))
              .collect(
                  Collectors.groupingBy(
                      Pair::key,
                      HashMap::new,
                      Collectors.mapping(Pair::value, Collectors.toList())));

      var task =
          dag.jobs().stream()
              .map(
                  j ->
                      new TaskDao(
                          j.uses().stream()
                              .map(f -> new TaskFileDao(f.file(), f.link(), f.size()))
                              .toList(),
                          j.id(),
                          parents.getOrDefault(j.id(), Collections.emptyList()),
                          children.getOrDefault(j.id(), Collections.emptyList()),
                          j.runtime()))
              .toList();

      return new WorkflowDao(0.0, task);

    } catch (IOException e) {
      throw new WorkflowLoadException(workflowJson.getName(), e);
    }
  }

  private WorkflowDao readFromJson(File workflowJson) {

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      return mapper.readValue(workflowJson, ScientificWorkflowDao.class).workflow();
    } catch (IOException e) {
      throw new WorkflowLoadException(workflowJson.getName(), e);
    }
  }
}
