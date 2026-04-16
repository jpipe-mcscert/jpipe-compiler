"""
Generate railroad diagram SVGs for the jPipe grammar rules.
One SVG per parser rule, written to docs/design/img/.

Usage (from the repository root):
    python3 jpipe-lang/gen_railroad.py

Requires the railroad-diagrams package:
    pip install railroad-diagrams
"""

import io
import os
import railroad as rr

OUT = os.path.join(os.path.dirname(__file__), "..", "docs", "design", "img")
OUT = os.path.normpath(OUT)
os.makedirs(OUT, exist_ok=True)


def write(name, diagram):
    path = os.path.join(OUT, f"grammar-{name}.svg")
    buf = io.StringIO()
    diagram.writeStandalone(buf.write)
    with open(path, "w", encoding="utf-8") as f:
        f.write(buf.getvalue())
    print(f"  wrote {path}")


# ── unit ──────────────────────────────────────────────────────────────────────
# unit : (justification | template | load)+ EOF
write("unit", rr.Diagram(
    rr.OneOrMore(
        rr.Choice(0,
            rr.NonTerminal("justification"),
            rr.NonTerminal("template"),
            rr.NonTerminal("load"),
        )
    ),
    rr.Terminal("EOF"),
))

# ── load ──────────────────────────────────────────────────────────────────────
# load : LOAD path=STRING (AS namespace=ID)?
write("load", rr.Diagram(
    rr.Terminal("load"),
    rr.NonTerminal("STRING", title="file path"),
    rr.Optional(
        rr.Sequence(rr.Terminal("as"), rr.NonTerminal("ID", title="namespace alias"))
    ),
))

# ── justification ─────────────────────────────────────────────────────────────
# justification : JUSTIFICATION id=ID (IMPLEMENTS parent=qualified_id)?
#                 ( OPEN justif_body CLOSE
#                 | IS operator=qualified_id OPEN_P params_decl? CLOSE_P rule_config? )
write("justification", rr.Diagram(
    rr.Terminal("justification"),
    rr.NonTerminal("ID", title="name"),
    rr.Optional(
        rr.Sequence(rr.Terminal("implements"), rr.NonTerminal("qualified_id", title="parent"))
    ),
    rr.Choice(0,
        rr.Sequence(
            rr.Terminal("{"),
            rr.NonTerminal("justif_body"),
            rr.Terminal("}"),
        ),
        rr.Sequence(
            rr.Terminal("is"),
            rr.NonTerminal("qualified_id", title="operator"),
            rr.Terminal("("),
            rr.Optional(rr.NonTerminal("params_decl")),
            rr.Terminal(")"),
            rr.Optional(rr.NonTerminal("rule_config")),
        ),
    ),
))

# ── template ──────────────────────────────────────────────────────────────────
# template : TEMPLATE id=ID (IMPLEMENTS parent=qualified_id)?
#            ( OPEN template_body CLOSE | IS operator=... )
write("template", rr.Diagram(
    rr.Terminal("template"),
    rr.NonTerminal("ID", title="name"),
    rr.Optional(
        rr.Sequence(rr.Terminal("implements"), rr.NonTerminal("qualified_id", title="parent"))
    ),
    rr.Choice(0,
        rr.Sequence(
            rr.Terminal("{"),
            rr.NonTerminal("template_body"),
            rr.Terminal("}"),
        ),
        rr.Sequence(
            rr.Terminal("is"),
            rr.NonTerminal("qualified_id", title="operator"),
            rr.Terminal("("),
            rr.Optional(rr.NonTerminal("params_decl")),
            rr.Terminal(")"),
            rr.Optional(rr.NonTerminal("rule_config")),
        ),
    ),
))

# ── justif_body ───────────────────────────────────────────────────────────────
# justif_body : (evidence | sub_conclusion | strategy | relation | conclusion)+
write("justif_body", rr.Diagram(
    rr.OneOrMore(
        rr.Choice(0,
            rr.NonTerminal("conclusion"),
            rr.NonTerminal("sub-conclusion"),
            rr.NonTerminal("strategy"),
            rr.NonTerminal("evidence"),
            rr.NonTerminal("relation"),
        )
    )
))

# ── template_body ─────────────────────────────────────────────────────────────
# template_body : (evidence | sub_conclusion | strategy | relation | conclusion | abstract)+
write("template_body", rr.Diagram(
    rr.OneOrMore(
        rr.Choice(0,
            rr.NonTerminal("conclusion"),
            rr.NonTerminal("sub-conclusion"),
            rr.NonTerminal("strategy"),
            rr.NonTerminal("evidence"),
            rr.NonTerminal("relation"),
            rr.NonTerminal("@support"),
        )
    )
))

# ── element declarations ──────────────────────────────────────────────────────
# evidence       : EVIDENCE      element
# strategy       : STRATEGY      element
# sub_conclusion : SUBCONCLUSION element
# conclusion     : CONCLUSION    element
# abstract       : ABSTRACT_SUP  element
# element : id=qualified_id IS name=STRING
def element_rule(keyword):
    return rr.Diagram(
        rr.Terminal(keyword),
        rr.NonTerminal("qualified_id", title="id"),
        rr.Terminal("is"),
        rr.NonTerminal("STRING", title="label"),
    )


write("evidence",       element_rule("evidence"))
write("strategy",       element_rule("strategy"))
write("sub-conclusion", element_rule("sub-conclusion"))
write("conclusion",     element_rule("conclusion"))
write("abstract",       element_rule("@support"))

# ── relation ──────────────────────────────────────────────────────────────────
# relation : from=qualified_id SUPPORT_LNK to=qualified_id
write("relation", rr.Diagram(
    rr.NonTerminal("qualified_id", title="from"),
    rr.Terminal("supports"),
    rr.NonTerminal("qualified_id", title="to"),
))

# ── qualified_id ──────────────────────────────────────────────────────────────
# qualified_id : parts+=ID (COLON parts+=ID)*
write("qualified_id", rr.Diagram(
    rr.NonTerminal("ID"),
    rr.ZeroOrMore(
        rr.Sequence(rr.Terminal(":"), rr.NonTerminal("ID"))
    )
))

# ── params_decl ───────────────────────────────────────────────────────────────
# params_decl : id+=qualified_id (COMMA id+=qualified_id)*
write("params_decl", rr.Diagram(
    rr.NonTerminal("qualified_id"),
    rr.ZeroOrMore(
        rr.Sequence(rr.Terminal(","), rr.NonTerminal("qualified_id"))
    )
))

# ── rule_config ───────────────────────────────────────────────────────────────
# rule_config : OPEN key_val_decl+ CLOSE
# key_val_decl : key=ID COLON value=STRING
write("rule_config", rr.Diagram(
    rr.Terminal("{"),
    rr.OneOrMore(
        rr.Sequence(
            rr.NonTerminal("ID", title="key"),
            rr.Terminal(":"),
            rr.NonTerminal("STRING", title="value"),
        )
    ),
    rr.Terminal("}"),
))

print("Done.")
