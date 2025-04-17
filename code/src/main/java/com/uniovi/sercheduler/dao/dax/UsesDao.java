package com.uniovi.sercheduler.dao.dax;

/**
 * Info about the file.
 *
 * @param file Name of the file.
 * @param link Determines if is input or output.
 * @param size The number of bytes of the file.
 */
public record UsesDao(String file, String link, Long size) {}
