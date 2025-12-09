package ca.mcscert.jpipe.visitors.exporters;

import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.model.elements.Pattern;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.visitors.ModelVisitor;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Export a given justification using a JSON common format.
 */
@SuppressWarnings({ "checkstyle:LeftCurly", "checkstyle:RightCurly",
                    "checkstyle:EmptyLineSeparator" })
public class JsonExporter extends ModelVisitor<JSONObject> {

    /**
     * Initialize the JSON object that will contain the result of the visit.
     */
    public JsonExporter() {
        super(new JSONObject());
        this.accumulator.put("elements", new JSONArray());
        this.accumulator.put("relations", new JSONArray());
    }

    @Override public void visit(Conclusion c)       { processElement("conclusion", c);        }
    @Override public void visit(Evidence e)         { processElement("evidence", e);          }
    @Override public void visit(Strategy s)         { processElement("strategy", s);          }
    @Override public void visit(SubConclusion sc)   { processElement("sub-conclusion", sc);   }
    @Override public void visit(AbstractSupport as) { processElement("abstract-support", as); }
    @Override public void visit(Justification j)    { processModel("justification", j);       }
    @Override public void visit(Pattern p)          { processModel("pattern", p);             }

    @Override
    public void visit(Unit u) {
        throw new RuntimeException("Cannot export an entire compilation unit!");
    }

    private void processElement(String type, JustificationElement e) {
        JSONArray elements = (JSONArray) this.accumulator.get("elements");
        JSONObject obj = new JSONObject();
        elements.put(obj.put("type", type).put("id", e.getIdentifier()).put("label", e.getLabel()));
    }

    private void processModel(String type, JustificationModel m) {
        this.accumulator.put("name", m.getName());
        this.accumulator.put("type", type);
        for (JustificationElement je : m.contents()) {  // Exporting elements
            je.accept(this);
        }
        for (JustificationElement je : m.contents()) { // Exporting structure
            for (JustificationElement supporter : je.getSupports()) {
                processRelation(supporter, je);
            }
        }
    }

    private void processRelation(JustificationElement supporter, JustificationElement element) {
        JSONArray relations = (JSONArray) this.accumulator.get("relations");
        JSONObject obj = new JSONObject();
        relations.put(obj.put("source", supporter.getIdentifier())
                         .put("target", element.getIdentifier()));
    }

}
