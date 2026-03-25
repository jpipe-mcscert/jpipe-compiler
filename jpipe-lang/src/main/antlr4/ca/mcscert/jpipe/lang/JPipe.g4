grammar JPipe;

/******************
 ** Parser rules **
 ******************/

// Root rule for parsing (called by the compilationChain)
unit            : (justification | template | load)+ EOF;

// Qualified identifier: a colon-separated path in the symbol table (e.g. a:b:c or just a)
qualified_id    : parts+=ID (COLON parts+=ID)*;

// Declare a justification: either a direct body or the result of an operator call
justification   : JUSTIFICATION id=ID (IMPLEMENTS parent=ID)?
                  ( OPEN justif_body CLOSE | IS operator=qualified_id OPEN_P params_decl? CLOSE_P rule_config? );
justif_body     : (evidence | sub_conclusion | strategy | relation | conclusion)+;

// Declare a template: either a direct body or the result of an operator call
template        : TEMPLATE id=ID (IMPLEMENTS parent=ID)?
                  ( OPEN template_body CLOSE | IS operator=qualified_id OPEN_P params_decl? CLOSE_P rule_config? );
template_body   : (evidence | sub_conclusion | strategy | relation | conclusion | abstract)+;

// load another file and bind its symbols under a namespace alias
load            : LOAD path=STRING AS namespace=ID;

// Body of a justification/template content
element         : id=ID IS name=STRING;
evidence        : EVIDENCE      element;
strategy        : STRATEGY      element;
sub_conclusion  : SUBCONCLUSION element;
conclusion      : CONCLUSION    element;
abstract        : ABSTRACT_SUP  element;

relation        : from=ID SUPPORT_LNK to=ID;

// Operator call configuration
params_decl     : id+=qualified_id (COMMA id+=qualified_id)*;
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
TEMPLATE        : 'template';
ABSTRACT_SUP    : '@support';

SUPPORT_LNK     : 'supports';

LOAD            : 'load';
AS              : 'as';

// Making whitespaces and newlines irrelevant to the syntax
WHITESPACE  : [ \t]+                -> channel(HIDDEN);
NEWLINE     : ('\r'? '\n' | '\r')+  -> channel(HIDDEN);

// Supporting multi-line and single-line comments
COMMENT     : '/*' .*? '*/'         -> channel(HIDDEN);
LINE_COMMENT: '//' ~[\r\n]*         -> channel(HIDDEN);

// Symbols & strings
fragment INTEGER : [0-9]+;
ID               : [A-Za-z_] [A-Za-z0-9_]*;
STRING           : '"' STRING_CHAR* '"' | '\'' STRING_CHAR* '\'' ;
OPEN             : '{';
CLOSE            : '}';
OPEN_P           : '(';
CLOSE_P          : ')';
COMMA            : ',';
COLON            : ':';

fragment STRING_CHAR : ~('\r' | '\n');

// Catch-all: skip unrecognized characters, error reporting handled by the error listener
ERROR_TOKEN : . -> skip;