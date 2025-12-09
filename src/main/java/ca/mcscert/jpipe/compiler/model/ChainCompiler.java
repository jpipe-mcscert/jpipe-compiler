package ca.mcscert.jpipe.compiler.model;

import ca.mcscert.jpipe.compiler.Compiler;
import java.io.IOException;

/**
 * Implements a compiler as a transformation applied from a Source to a Sink.
 *
 * @param <I> Input type of the compiler (provided from source).
 * @param <O> Output type of the compiler (consumed by sink).
 */
public class ChainCompiler<I, O> implements Compiler {

    private final Source<I> inputSource;
    private final Transformation<I, O> chain;
    private final Sink<O> outputSink;

    /**
     * Instantiates a ChainCompiler from its three needed elements.
     *
     * @param inputSource the source providing input data as instance of I.
     * @param chain the chain transforming the source into an instance of O.
     * @param outputSink the sink processing the instance of O obtained as result.
     */
    public ChainCompiler(Source<I> inputSource, Transformation<I, O> chain, Sink<O> outputSink) {
        this.inputSource = inputSource;
        this.chain = chain;
        this.outputSink = outputSink;
    }

    /**
     * Compilation process, running the chain from source to sink.
     *
     * @param sourceFile the file path to be used by the source.
     * @param sinkFile the file path to be used by the sink.
     * @throws IOException is something goes wrong.
     */
    @Override
    public void compile(String sourceFile, String sinkFile) throws IOException {
        I i = inputSource.provideFrom(sourceFile);
        O o = chain.fire(i, sourceFile);
        outputSink.pourInto(o, sinkFile);
    }

}
