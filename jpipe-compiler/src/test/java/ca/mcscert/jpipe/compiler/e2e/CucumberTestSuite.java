package ca.mcscert.jpipe.compiler.e2e;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/** Entry point for Cucumber end-to-end tests. */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
public class CucumberTestSuite {
}
