package com.uniovi.sercheduler.parser;

import com.uniovi.sercheduler.dao.WorkflowDao;
import com.uniovi.sercheduler.dto.Task;
import java.io.File;
import java.util.Map;

/** Define a generic loader. */
public interface WorkflowLoader {

  public Map<String, Task> load(WorkflowDao workflowDao);

  public WorkflowDao readFromFile(File workflowJson);
}
