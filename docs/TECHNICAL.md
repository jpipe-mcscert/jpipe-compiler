# jPipe Compiler

<div align="center">

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ace-design_jpipe&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ace-design_jpipe)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=ace-design_jpipe&metric=bugs)](https://sonarcloud.io/summary/new_code?id=ace-design_jpipe)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=ace-design_jpipe&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=ace-design_jpipe)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ace-design_jpipe&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ace-design_jpipe)

</div>

## Installing the compiler as a user

If you don't plan to contribute to the source code, the easiest way is to follow the [installation instructions](https://github.com/ace-design/jpipe/wiki/Setup) to install a stable release on your operating system. 

### Installing the compiler as a jPipe developer

The first step is to clone this repository on your local computer. The compiler source code is in the `compiler` directory.

```
~ $ git clone git@github.com:ace-design/jpipe.git
~ $ cd jpipe/compiler
compiler $
```

### Compiling the source code (aka packaging)

We're using Maven to handle dependencies and build lifecycle. 

```
compiler $ mvn clean package
```

Packaging only run integration tests. Non-regression tests are defined as integration tests, and 
can be triggered by running:

```
compiler $ mvn clean verify
```

One can skip some tests execution at build time by using the following options:

- `-Dskip.tests.unit` to skip unit tests execution
- `-Dskip.tests.integration` to skip integration tests execution

When packaging, the checkstyle conventions for coding are not enforced.

### Executing the compiler from CLI

Assuming your code is packaged, you can call the compiler using maven:

```
 compiler $ mvn -q exec:java -Dexec.mainClass="ca.mcscert.jpipe.Main" -Dexec.args="-h"
```

The `-Dexec.args` argument contains the command line argument you want to pass to the compiler (here, `-h`).

### Building the compiler artefact

The released compiler is built as a turn-key JAR file, `jpipe.jar`. To build such an artefact, you can use maven:

```
 compiler $ mvn install
```

When installing:

  1. Code is compiled, unit tests are ran, and a jar file is produced;
  2. Integration tests are ran to verify non-regression; 
  1. maven triggers checkstyle verifications to ensure the code respects the coding 
conventions we agreed on for the project (a [slight variation](./src/main/resources/checkstyle/checkstyle-suppressions.xml) of the one released by Google). 
2. 
3. Maven will refuse installing the compiler until your code pass all these steps 

To avoid bad surprises when installing the compiler and check coding practices on the fly, you 
might want to configure your IDE to check these rules while you are coding. 

We provide a standalone configuration file for IntelliJ (`./src/main/resources/checkstyle/jpipe_checks.xml`)

### How to run the compiler?

Once the compiler is installed, you can use it directly by invoking the JAR file from the command line and get rid of maven:

```
compiler $ java -jar jpipe.jar -h
```

### Running sonarqube code quality analysis

to execute this manually, you need a `SONAR_TOKEN` from sonarcloud.io. If you don't have one, 
you can just rely on the automated CI/CD pipeline that run the analysis on each pull requests.

To run the analysis manually:

```
compiler $ mvn verify \
                org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                -Dsonar.projectKey=ace-design_jpipe 
```

### Listing outdated dependencies

```
compiler $ mvn versions:display-dependency-updates
```

## How to contribute to the compiler?

Feel free to contact [Sébastien Mosser](mossers@mcmaster.ca) to discuss bout potential
collaboration on jPipe.

Before contributing, you might want to read some [_Architectural Decisions_](adr.md) recorded as Y-statements, providing some explanations on the global architecture.

External contributions are accepted as pull requests.


<div align="center">

[![SonarQube Cloud](https://sonarcloud.io/images/project_badges/sonarcloud-light.svg)](https://sonarcloud.io/summary/new_code?id=ace-design_jpipe)

</div>