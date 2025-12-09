package ca.mcscert.jpipe.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ConfigurationTest {


    @Test
    public void readDiagramNameFromCLI() throws Exception {
        CommandLineParser cli = new CommandLineParser(args("-d", "diag"));
        Configuration config = new Configuration(cli.read().get());
        assertEquals("diag", config.getDiagramName());
    }


    private String[] args(String... params) {
        return params;
    }

}
