package ca.mcscert.jpipe.operators.internals;

/**
 * Model what a binary operator is in jPipe.
 *
 * @param <L> type of the Left operand.
 * @param <R> type of the Right operand.
 * @param <O> type of the Output value.
 */
public interface BinaryOperator<L, R, O> {

    /**
     * Apply the operator as in o = this(left, right).
     *
     * @param left the left operand.
     * @param right the right operand.
     * @return an instance of O being the result of this(left, right).
     */
    O apply(L left, R right);

}
