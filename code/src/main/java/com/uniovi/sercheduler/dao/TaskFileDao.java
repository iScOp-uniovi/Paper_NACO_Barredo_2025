package com.uniovi.sercheduler.dao;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines a file used in a task.
 *
 * @param link Indicates the direction of the file. (input or output).
 * @param name Name of the file.
 * @param size How many bytes the files has.
 */
public record TaskFileDao(String link, String name, @JsonProperty("sizeInBytes") Long size) {}
