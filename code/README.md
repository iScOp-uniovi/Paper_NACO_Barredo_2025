# Sercheduler

The project is a CLI to execute a GA for the scientific workflow scheduling problem.

# Getting Started

To create the jar you have to issue the following command.

```bash
mvn clean install
```

Then you can run:

```bash
java -jar target/sercheduler-0.0.1-SNAPSHOT.jar evaluate \
  --workflowFile workflowFile \
  --hostsFile hostsFile \
  --seed seed \
  --executions executions \
  --fitness [simple,heft,rank,multi]

```

## Example

```bash
java -jar target/sercheduler-0.0.1-SNAPSHOT.jar evaluate --workflowFile src/test/resources/montage.json \
--hostsFile  src/test/resources/hosts_test.json \
 --seed 1 \
 --executions 1000000 \
 --fitness heft
```

## Execute all experiments

We need a folder with the following structure

- benchmark-data
    - workflows (inside all workflows)
    - hosts
        - mixed (inside all hosts)
        - fast (inside all hosts)

```bash
java -jar target/sercheduler-0.0.1-SNAPSHOT.jar experiment \
  -W workflowsPth \
  -H hostsPath \
  -T type \
  --seed seed \
  --executions executions
  -C experimentConfigJson
```

### Example

```bash
java -jar target/sercheduler-0.0.1-SNAPSHOT.jar experiment -W ../benchmark-data/workflows/ \
-H ../benchmark-data/hosts/ \
-T mixed \
--seed 1 \
--executions 100000 \
-C src/test/resources/experimentConfig.json
```

## Experiment with Jmetal

We can make experiments using the JMetal framework. The framework creates a folder with the name configured in the JSON. If we want to redo
the execution we need to delete it before running the program again.

We need a folder with the following structure

- benchmark-data
  - workflows (inside all workflows)
  - hosts
    - mixed (inside all hosts)
    - fast (inside all hosts)

```bash
java -jar target/sercheduler-0.0.1-SNAPSHOT.jar jmetal \
  -W workflowsPth \
  -H hostsPath \
  -T type \
  --seed seed \
  --executions executions
  -C experimentConfigJson
```

### Example

```bash
java -jar target/sercheduler-0.0.1-SNAPSHOT.jar jmetal -W ../benchmark-data/workflows/ \
-H ../benchmark-data/hosts/ \
-T mixed \
--seed 1 \
--executions 100000 \
-C src/test/resources/experimentConfig.json
```



### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.1.0/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.1.0/maven-plugin/reference/html/#build-image)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/docs/3.1.0/reference/htmlsingle/#using.devtools)
* [Spring Configuration Processor](https://docs.spring.io/spring-boot/docs/3.1.0/reference/htmlsingle/#appendix.configuration-metadata.annotation-processor)
* [Docker Compose Support](https://docs.spring.io/spring-boot/docs/3.1.0/reference/htmlsingle/#features.docker-compose)
* [Spring Shell](https://spring.io/projects/spring-shell)


