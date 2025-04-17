package com.uniovi.sercheduler.dao.dax;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import java.util.List;

/**
 * Representation of a job.
 *
 * <p>IPORTANT: In this case the task does not have unique name, and creating one can generate
 * problems, so we need to use the unique identifier.
 *
 * @param id Unique id of the job.
 * @param runtime The time it takes to execute the job.
 * @param uses The list of files need it.
 */
public record JobDao(
    String id, Double runtime, @JacksonXmlElementWrapper(useWrapping = false) List<UsesDao> uses) {}
