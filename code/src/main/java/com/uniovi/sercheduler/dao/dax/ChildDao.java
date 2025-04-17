package com.uniovi.sercheduler.dao.dax;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;

/**
 * Represents a relationship between tasks.
 *
 * @param ref The name of the child.
 * @param parents List of parents of the child.
 */
public record ChildDao(
    String ref,
    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "parent")
        List<ParentDao> parents) {}
