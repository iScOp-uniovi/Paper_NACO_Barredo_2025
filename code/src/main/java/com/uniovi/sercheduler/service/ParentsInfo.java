package com.uniovi.sercheduler.service;

/**
 * Information from the parents.
 *
 * @param maxEst The max est of all the parents of the task.
 * @param taskCommunications All time it takes to transfer the files from the parents.
 */
public record ParentsInfo(Double maxEst, Double taskCommunications) {}
