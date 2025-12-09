package ca.mcscert.jpipe.model.cloning;

/**
 * As using the Cloneable interface is a pain, we redefined the behavior of the clone method
 * as a "replicate" one. Replication produce a deep clone, with all contained elements being also
 * replicated recursively.
 */
public interface Replicable<T> {

    /**
     * Create a replica (aka deep clone) of the current object.
     *
     * @return a deep copy of the object.
     */
    T replicate();

}
