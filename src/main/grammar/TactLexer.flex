package org.ton.intellij.tact.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.ton.intellij.tact.psi.TactElementTypes.*;

%%

%{
  public _TactLexer() {
    this((java.io.Reader)null);
  }
%}

%{
  /**
    * Dedicated storage for starting position of some previously successful
    * match
    */
  private int zzPostponedMarkedPos = -1;

  /**
    * Dedicated nested-comment level counter
    */
  private int zzNestedCommentLevel = 0;

  private int zzBlockDepth = 0;
  private int zzParenDepth = 0;
  private boolean zzStructScope = false;
  private boolean zzContractScope = false;
  private boolean zzStructIdentifierExpected = false;
%}

%{
  IElementType imbueBlockComment() {
      assert(zzNestedCommentLevel == 0);
      popState();

      zzStartRead = zzPostponedMarkedPos;
      zzPostponedMarkedPos = -1;

//      if (yylength() >= 3) {
//          if (yycharat(2) == '!') {
//              return INNER_BLOCK_DOC_COMMENT;
//          } else if (yycharat(2) == '*' && (yylength() == 3 || yycharat(3) != '*' && yycharat(3) != '/')) {
//              return OUTER_BLOCK_DOC_COMMENT;
//          }
//      }

      return BLOCK_COMMENT;
  }
%}

%public
%class _TactLexer
%implements FlexLexer
%function advance
%type IElementType

%{
    private static final class State {
        final int lBraceCount;
        final int state;

        public State(int state, int lBraceCount) {
            this.state = state;
            this.lBraceCount = lBraceCount;
        }

        @Override
        public String toString() {
            return "yystate = " + state + (lBraceCount == 0 ? "" : "lBraceCount = " + lBraceCount);
        }
    }

    private final Stack<State> states = new Stack<State>();
    private int lBraceCount;

    private int commentStart;
    private int commentDepth;

    private void pushState(int state) {
        states.push(new State(yystate(), lBraceCount));
        lBraceCount = 0;
        yybegin(state);
    }

    private void popState() {
        State state = states.pop();
        lBraceCount = state.lBraceCount;
        yybegin(state.state);
    }
%}

%s IN_BLOCK_COMMENT
%s IN_NAME_ATTRIBUTE
%s STRING
%s STRUCT

%unicode

EOL=\R
WHITE_SPACE=\s+

WHITE_SPACE=[ \t\n\x0B\f\r]+
NON_ZERO_DIGIT=[1-9]
HEX_DIGIT=[0-9a-fA-F]
DIGIT=[0-9]
BIN_DIGIT=[01]
OCT_DIGIT=[0-7]
INTEGER_LITERAL_DEC = ({NON_ZERO_DIGIT}(_?{DIGIT})*) | 0{DIGIT}*
INTEGER_LITERAL_HEX = 0[xX] {HEX_DIGIT} (_?{HEX_DIGIT})*
INTEGER_LITERAL_BIN = 0[bB] {BIN_DIGIT} (_?{BIN_DIGIT})*
INTEGER_LITERAL_OCT = 0[oO] {OCT_DIGIT} (_?{OCT_DIGIT})*
INTEGER_LITERAL= {INTEGER_LITERAL_HEX} | {INTEGER_LITERAL_BIN} | {INTEGER_LITERAL_OCT} | {INTEGER_LITERAL_DEC}
IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
FUNC_IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_?!:&']*

REGULAR_STRING_PART=[^\\\"]+
ESCAPE_SEQUENCE=\\\\ // backslash
| \\\" // double quote
| \\n // newline
| \\r // carriage return
| \\t // tab
| \\v // vertical tab
| \\b // backspace
| \\f // form feed
| \\u\{ {HEX_DIGIT} {HEX_DIGIT}? {HEX_DIGIT}? {HEX_DIGIT}? {HEX_DIGIT}? {HEX_DIGIT}? \} // unicode escape
| \\u {HEX_DIGIT} {HEX_DIGIT} {HEX_DIGIT} {HEX_DIGIT} // hex escape
| \\x {HEX_DIGIT} {HEX_DIGIT} // hex escape
| \\[^\n] // any other character

%%

<IN_BLOCK_COMMENT> {
  "/*"    { if (zzNestedCommentLevel++ == 0)
              zzPostponedMarkedPos = zzStartRead;
          }

  "*/"    { if (--zzNestedCommentLevel == 0)
              return imbueBlockComment();
          }

  <<EOF>> { zzNestedCommentLevel = 0; return imbueBlockComment(); }

  [^]     { }
}

<STRING> {
    {REGULAR_STRING_PART} { return REGULAR_STRING_PART; }
    {ESCAPE_SEQUENCE}     { return ESCAPE_SEQUENCE; }
    \"                    { popState(); return CLOSE_QUOTE; }
    [^]                   { popState(); yypushback(1); }
}

<IN_NAME_ATTRIBUTE> {
   "@name" { return NAME_MACRO; }
   "("    { zzParenDepth++; return LPAREN; }
   ")"    { zzParenDepth--; popState(); return RPAREN; }
   {FUNC_IDENTIFIER} { popState(); return FUNC_IDENTIFIER; }
   [^]    { popState(); yypushback(1); }
}

{WHITE_SPACE}           { return WHITE_SPACE; }
\"                      { pushState(STRING); return OPEN_QUOTE; }

"/*"                    { pushState(IN_BLOCK_COMMENT); yypushback(2); }
"//".*                  { return LINE_COMMENT; }

"{"                     {
          zzBlockDepth++;
          if (zzStructScope) {
              zzStructIdentifierExpected = true;
          }
          return LBRACE;
      }
"}"                     {
        if (zzBlockDepth-- == 0) {
            zzStructScope = false;
            zzContractScope = false;
        }
        return RBRACE;
    }
"["          { return LBRACK; }
"]"          { return RBRACK; }
"("          { zzParenDepth++; return LPAREN; }
")"          { zzParenDepth--; return RPAREN; }
":"          {
          if (zzStructScope) {
              zzStructIdentifierExpected = false;
          }
          return COLON;
      }
";"          {
          if (zzStructScope) {
              zzStructIdentifierExpected = true;
          }
          return SEMICOLON;
      }
","          { return COMMA; }
"."          { return DOT; }
"+"          { return PLUS; }
"-"          { return MINUS; }
"*"          { return MUL; }
"/"          { return DIV; }
"%"          { return REM; }
"&"          { return AND; }
"|"          { return OR; }
"^"          { return XOR; }
"<"          { return LT; }
">"          { return GT; }
"="          { return EQ; }
"?"          { return Q; }
"!"          { return EXCL; }
"~"          { return TILDE; }
"+="         { return PLUSLET; }
"-="         { return MINUSLET; }
"*="         { return TIMESLET; }
"/="         { return DIVLET; }
"%="         { return MODLET; }
"&="         { return ANDLET; }
"|="         { return ORLET; }
"^="         { return XORLET; }
"=="         { return EQEQ; }
"!="         { return EXCLEQ; }
">="         { return GTEQ; }
"<="         { return LTEQ; }
">>"         { return GTGT; }
"<<"         { return LTLT; }
"||"         { return OROR; }
"&&"         { return ANDAND; }
"!!"         { return EXCLEXCL; }
"if"         { return IF_KEYWORD; }
"else"       { return ELSE_KEYWORD; }
"while"      { return WHILE_KEYWORD; }
"do"         { return DO_KEYWORD; }
"until"      { return UNTIL_KEYWORD; }
"repeat"     { return REPEAT_KEYWORD; }
"return"     { return RETURN_KEYWORD; }
"extends"    { return EXTENDS_KEYWORD; }
"mutates"    { return MUTATES_KEYWORD; }
"virtual"    { return VIRTUAL_KEYWORD; }
"override"   { return OVERRIDE_KEYWORD; }
"inline"     { return INLINE_KEYWORD; }
"native"     { return NATIVE_KEYWORD; }
"let"        { return LET_KEYWORD; }
"const"      { return CONST_KEYWORD; }
"fun"        { return FUN_KEYWORD; }
"initOf"     { return INIT_OF_KEYWORD; }
"as"         { return AS_KEYWORD; }
"abstract"   { return ABSTRACT_KEYWORD; }
"import"     { return IMPORT_KEYWORD; }
"struct"     { zzStructScope = true; return STRUCT_KEYWORD; }
"message"    { return zzBlockDepth == 0 ? MESSAGE_KEYWORD : IDENTIFIER; }
"contract"   { zzContractScope = true; return CONTRACT_KEYWORD; }
"trait"      { return TRAIT_KEYWORD; }
"with"       { return WITH_KEYWORD; }
"receive"    { return RECEIVE_KEYWORD; }
"external"   { return EXTERNAL_KEYWORD; }
"true"       { return BOOLEAN_LITERAL; }
"false"      { return BOOLEAN_LITERAL; }
"null"       { return NULL_LITERAL; }
"primitive"  { return PRIMITIVE_KEYWORD; }
"self"       { return SELF_KEYWORD; }
"map"        { return MAP_KEYWORD; }
"try"        { return TRY_KEYWORD; }
"catch"      { return CATCH_KEYWORD; }
"foreach"    { return FOREACH_KEYWORD; }
"in"         { return IN_KEYWORD; }
"bounced"    {
          if (zzStructScope && zzStructIdentifierExpected) {
              zzStructIdentifierExpected = false;
              return IDENTIFIER;
          }
          return BOUNCED_KEYWORD;
}
"init"       { return zzBlockDepth == 1 && zzParenDepth == 0 ? INIT_KEYWORD : IDENTIFIER; }
"get"        { return zzBlockDepth <= 1 ? GET_KEYWORD : IDENTIFIER; }
"@interface" { return INTERFACE_MACRO; }
"@name"      { pushState(IN_NAME_ATTRIBUTE); yypushback(5); }

{INTEGER_LITERAL}       { return INTEGER_LITERAL; }
{IDENTIFIER}            { return IDENTIFIER; }

[^] { return BAD_CHARACTER; }
