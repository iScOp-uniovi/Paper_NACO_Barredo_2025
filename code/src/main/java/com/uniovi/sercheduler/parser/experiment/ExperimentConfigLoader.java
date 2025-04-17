package com.uniovi.sercheduler.parser.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniovi.sercheduler.dao.experiment.ExperimentConfig;
import com.uniovi.sercheduler.expception.ExperimentConfigLoadException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/** Class or loading experiment configuration. */
@Service
public class ExperimentConfigLoader {

  /**
   * Reads the configuration from a json file.
   *
   * @param experimentConfigJson path to json file.
   * @return The configuration.
   */
  public ExperimentConfig readFromFile(File experimentConfigJson) {

    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(experimentConfigJson, ExperimentConfig.class);
    } catch (IOException e) {
      throw new ExperimentConfigLoadException(experimentConfigJson.getName(), e);
    }
  }
}
