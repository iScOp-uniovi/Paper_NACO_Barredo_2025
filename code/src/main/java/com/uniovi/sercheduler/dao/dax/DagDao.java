package com.uniovi.sercheduler.dao.dax;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;

/**
 * Contains a xml DAG representation.
 *
 * @param jobs All the jobs in the dag.
 * @param children All the links between tasks.
 */
public record DagDao(
    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "job")
        List<JobDao> jobs,
    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "child")
        List<ChildDao> children) {}
