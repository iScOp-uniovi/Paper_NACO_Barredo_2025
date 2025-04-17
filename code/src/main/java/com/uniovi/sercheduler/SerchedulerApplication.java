package com.uniovi.sercheduler;

import com.uniovi.sercheduler.dao.ScientificWorkflowDao;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.CommandScan;

/** Application for the execution of GA for the Scheduling problem of scientific workflow. */
@SpringBootApplication
@CommandScan
public class SerchedulerApplication {

  /**
   * Entrypoint of the application.
   *
   * @param args Parameters of the application.
   */
  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(SerchedulerApplication.class);
    application.setBannerMode(Banner.Mode.OFF);
    application.run(args);
  }
}
