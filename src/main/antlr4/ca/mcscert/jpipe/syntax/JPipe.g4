grammar JPipe;

/******************
 ** Parser rules **
 ******************/

// Root rule for parsing (called by the compilationChain)
unit            : (justification | template | load | composition)+ EOF;

// Declare a justification
justification   : JUSTIFICATION id=ID (IMPLEMENTS parent=ID)? OPEN justif_body CLOSE;
justif_body     : (evidence | sub_conclusion | strategy | relation | conclusion)+;

// Declare a pattern (like a justification, but allowing abstract supports)
template         : TEMPLATE id=ID (IMPLEMENTS parent=ID)? OPEN template_body CLOSE;
template_body    : (evidence | sub_conclusion | strategy | relation | conclusion | abstract)+;

// load another file
load            : LOAD path=STRING;

// Body of a justification/pattern content
element         : id=ID IS name=STRING;
evidence        : EVIDENCE      element;
strategy        : STRATEGY      element;
sub_conclusion  : SUBCONCLUSION element;
conclusion      : CONCLUSION    element;
abstract        : ABSTRACT_SUP  element;

relation        : from=ID SUPPORT_LNK to=ID;

// composition unit
composition     : COMPOSITION OPEN (rule_decl)+ CLOSE;
rule_decl       : type=(JUSTIFICATION | TEMPLATE) id=ID IS operator=ID OPEN_P params_decl* CLOSE_P rule_config?;
params_decl     : id=ID (COMMA params_decl)*;
rule_config     : OPEN key_val_decl+ CLOSE;
key_val_decl    : key=ID COLON value=STRING;

/*****************
 ** Lexer rules **
 *****************/

// Keywords

JUSTIFICATION   : 'justification';
IMPLEMENTS      : 'implements';
IS              : 'is';

EVIDENCE        : 'evidence';
STRATEGY        : 'strategy';
SUBCONCLUSION   : 'sub-conclusion';
CONCLUSION      : 'conclusion';
TEMPLATE         : 'template';
ABSTRACT_SUP    : '@support';

SUPPORT_LNK     : 'supports';

LOAD            : 'load';

COMPOSITION     : 'composition';

// Making whitespaces and newlines irrelevant to the syntax
WHITESPACE  : [ \t]+                -> channel(HIDDEN);
NEWLINE     : ('\r'? '\n' | '\r')+  -> channel(HIDDEN);

// Supporting multi-line and single-line comments
COMMENT     : '/*' .*? '*/'         -> channel(HIDDEN);
LINE_COMMENT: '//'STRING_CHAR*      -> channel(HIDDEN);

// Symbols & strings
INTEGER     : [0-9][1-9]*;
ID          : ( [A-Z] | [a-z] | INTEGER | '_')+;
STRING      : '"' STRING_CHAR* '"' | '\'' STRING_CHAR* '\'' ;
OPEN        : '{';
CLOSE       : '}';
OPEN_P      : '(';
CLOSE_P     : ')';
COMMA       : ',';
COLON       : ':';

fragment STRING_CHAR : ~('\r' | '\n');

