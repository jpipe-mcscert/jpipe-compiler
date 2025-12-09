package ca.mcscert.jpipe.compiler.model;

import java.io.IOException;

/**
 * Model a Sink, i.e., the end of a compilation chain.
 *
 * @param <O> the type of elements to be serialized in the output file.
 */
public interface Sink<O>  {

     /**
      * Final step of a compilation chain.
      *
      * @param output the element to serialize.
      * @param fileName the filename to use for serialization.
      * @throws IOException is something goes wrong.
      */
     void pourInto(O output, String fileName) throws IOException;

}

