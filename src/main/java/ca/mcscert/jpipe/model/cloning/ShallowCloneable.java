package ca.mcscert.jpipe.model.cloning;

/**
 * Method creating a shallow clone of the object. In jPipe, a shallow clone contains only scalar
 * attributes (e.g., int, String, booleans), and all references to other objects are nulled.
 *
 * @param <T> The clone's type
 */
public interface ShallowCloneable<T> {

    /**
     * Create a shallow clone of the current object.
     *
     * @return a copy of the current object, with all references set to null.
     */
    T shallow();

}
