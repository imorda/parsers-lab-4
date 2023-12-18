grammar gram;

code: GRAMMAR package SEMICOLON
      ROOT NAME SEMICOLON
      body;

package: (NAME DOT)*NAME;

body : (parseRule
     | lexRule)+;

lexRule: LEX_NAME COLON (STRING | REGEXP) (TO_SKIP)? SEMICOLON;

parseRule: NAME args? returnType? COLON parseRuleBody SEMICOLON;

parseRuleBody: (singleRule OR)* singleRule;

singleRule: singleRuleBody* returnType?;

singleRuleBody : nonterminal | LEX_NAME | CODE;

nonterminal: NAME args?;

args: (LBRACKET CODE RBRACKET);

returnType: (RETURN CODE);


SEMICOLON : ';';
DOT : '.';
COLON : ':';
GRAMMAR : 'grammar';
ROOT : 'root';
TO_SKIP : 'skip';
RETURN : '->';
LBRACKET : '(';
RBRACKET : ')';
OR : '|';
LEX_NAME : ([A-Z])([_\-a-zA-Z0-9])*;
NAME : ([_a-zA-Z])([_\-a-zA-Z0-9])*;
STRING : '"' (~('"') | '\\"')+ '"';
CODE : '`'~[`]+'`';
REGEXP : '/' (~('/'|'\r'|'\n') | '\\/')+ '/';
NUMBER : ('-')? [0-9]+;
WS: [ \t\r\n]+ -> skip;
