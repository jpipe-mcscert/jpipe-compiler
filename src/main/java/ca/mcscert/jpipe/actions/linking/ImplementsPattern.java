package ca.mcscert.jpipe.actions.linking;

import ca.mcscert.jpipe.actions.RegularAction;
import ca.mcscert.jpipe.error.UnknownSymbol;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.model.elements.Pattern;
import ca.mcscert.jpipe.operators.CompositionOperator;
import ca.mcscert.jpipe.operators.PatternImplementation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Implement a pattern into a given justification model, and lock it.
 */
public class ImplementsPattern extends RegularAction {

    private final String currentId;
    private final String parentId;

    public ImplementsPattern(String currentId, String parentId) {
        this.currentId = currentId;
        this.parentId = parentId;
    }

    @SuppressWarnings({"checkstyle:LeftCurly", "checkstyle:OneStatementPerLine"})
    @Override
    public Function<Unit, Boolean> condition() {
        return (u -> {
            try { u.get(currentId); } catch (UnknownSymbol us) { return false; }
            try { u.get(parentId);  } catch (UnknownSymbol us) { return false; }
            return (u.get(parentId).isFrozen());
        });
    }

    @Override
    public void execute(Unit context) throws Exception {
        JustificationModel model = context.get(this.currentId);
        Pattern pattern = (Pattern) context.get(this.parentId); // Cast OK by design (condition)
        PatternImplementation op = new PatternImplementation();

        JustificationModel implementing =
                op.run(CompositionOperator.ReturnType.of(model), model.getName(),
                        List.of(model, pattern), Map.of());
        context.remove(model);
        context.add(implementing);
        implementing.freeze();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ImplementsPattern{");
        sb.append("currentId='").append(currentId).append('\'');
        sb.append(", parentId='").append(parentId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
