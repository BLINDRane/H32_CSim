/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import java.util.ArrayList;
import lexer.Lexer;
import lexer.SType;
import lexer.Token;
import ptn.ArrayDecl;
import ptn.AssignState;
import ptn.Block;
import ptn.Decl;
import ptn.DeclStats;
import ptn.ExprList;
import ptn.Expression;
import ptn.ForState;
import ptn.Funcall;
import ptn.Funct;
import ptn.Functs;
import ptn.IfState;
import ptn.IncDecState;
import ptn.Literal;
import ptn.Name;
import ptn.Operator;
import ptn.PTNode;
import ptn.Param;
import ptn.ParamList;
import ptn.Proccall;
import ptn.Program;
import ptn.RetState;
import ptn.Statement;
import ptn.Type;
import ptn.VarDecl;
import ptn.VarRef;
import ptn.WhileState;

/**
 *
 * @author Lab Admin
 */
public class Parser {

    //Create a new parser
    public Parser(Lexer lex) {
        this.lex = lex;
        this.stab = SymbolTable.getInstance();
        errorMessages = new ArrayList<>();
        errors = false;
    }
    
    public SymbolTable getSymbolTable() {
        return stab;
    }

    public boolean hasErrors() {
        return errors;
    }

    public ArrayList<String> getErrorMessages() {
        return errorMessages;
    }

    public PTNode parse() {
        try {
            return parseProgram();
        } catch (ParseException ex) {
            errorMessages.add(ex.getMessage());
            errors = true;
            return null;
        }
    }

    //Checks to see if the file passed to the parser follows the expected format for new programs
    private Program parseProgram() throws ParseException {
        Token s = (Token) lex.peek();
        Program n = new Program();
        if (s==null){
            throwParseException("Empty program.",new Token("ERROR",SType.KW,0,0));
        }
        //If the program doesn't start with "main" start parsing functions
        if (!s.getSymbol().equals("main")) {
            n.setFuncts(parseFuncts());
        }
        s = (Token) lex.next(); // main
        if (s==null || !s.getSymbol().equals("main")) {
            throwParseException("expecting 'main'", s);
        }
        s = (Token) lex.next(); // (
        if (s==null || !s.getSymbol().equals("(")) {
            throwParseException("expecting '('", s);
        }
        s = (Token) lex.next(); // )
        if (s==null || !s.getSymbol().equals(")")) {
            throwParseException("expecting ')'", s);
        }
        n.setBody(parseBlock());
        if (lex.hasNext()) {
            throwParseException("unexpected input after end of program", s);
        }
        return n;
    }

    //Go through the program looking for functions (methods)
    private Functs parseFuncts() throws ParseException {
        Functs n = new Functs();
        Token s = lex.peek();
        while (s != null && !s.getSymbol().equals("main")) {
            try {
                n.addChild(parseFunct());
                s = lex.peek();
            } catch (ParseException ex) {
                errorMessages.add(ex.getMessage());
                errors = true;
                synchToToken("}");
                s = lex.peek();
            }
        }
        return n;
    }
    //Read over a found function and check each piece for validity
    private Funct parseFunct() throws ParseException {
        Token s;
        Funct n = new Funct();
        n.setType(parseType());
        n.setName(parseName());
        s = (Token) lex.next();
        if (!s.getSymbol().equals("(")) {
            throwParseException("expecting '('", s);
        }
        stab.addFunction(n.getName().toString());
        s = (Token) lex.peek();
        if (!s.getSymbol().equals(")")) {
            n.setParamList(parseParamList());
        }
        s = (Token) lex.next();
        if (!s.getSymbol().equals(")")) {
            throwParseException("missing ')'", s);
        }

        n.getName().getSymbol().setSymbol(n.getNameMangle());
        stab.add(n.getName(), n.getType());
        stab.pushScope();
        n.setScope(stab.getCurrentScope());
        if (n.getParamList() != null) {
            n.getParamList().pushParams(stab);
        }
        n.setBody(parseBlock());
        stab.popScope();
        return n;
    }
    //Find the symbol type of the token the parser is currently looking at
    private Type parseType() throws ParseException {
        Token s = lex.next();
        if (s.getSType() != SType.TY) {
            throwParseException("expecting type", s);
        }
        Type t = new Type(s);
        s = lex.peek();
        // THIS WHILE LOOP IS HANDLING POINTER DECLARATION
        while (s.getSymbol().equals("*")) {
            lex.next();
            t.incPointer();
            s = lex.peek();
        }
        return t;
    }
    //Look for the open close parentheses that signals a set of function parameters
    private ParamList parseParamList() throws ParseException {
        ParamList n = new ParamList();
        Token s = lex.peek();
        while (!s.getSymbol().equals(")")) {
            n.addParam(parseParam());
            s = lex.peek();
            if (s.getSymbol().equals(",")) {
                lex.next();
                s = lex.peek();
            }
        }
        return n;
    }
    //Check over each parameter and return them as n
    private Param parseParam() throws ParseException {
        Param n = new Param();
        n.setType(parseType());
        n.setName(parseName());
        //stab.getCurrentScope().add(n.getName(), n.getType());
        //n.setScope(stab.getCurrentScope());
        return n;
    }
    //Looks for blocks and iterates through them
    private Block parseBlock() throws ParseException {
        Token s = lex.next();
        Block n = new Block();
        if (!s.getSymbol().equals("{")) {
            throwParseException("missing opening brace", s);
        }
        stab.pushScope();
        n.setScope(stab.getCurrentScope());
        n.setBody(parseDeclStats());
        stab.popScope();
        s = lex.next();
        if (!s.getSymbol().equals("}")) {
            throwParseException("missing closing brace", s);
        }
        return n;
    }
    
    private DeclStats parseDeclStats() throws ParseException {
        DeclStats n = new DeclStats();
        Token s = lex.peek();
        while (!s.getSymbol().equals("}")) {
            if (SType.isType(s.getSymbol()) || s.getSymbol().equals("static")) {
                try {
                    Decl decl = parseDecl();
                    n.addDecl(decl);
                } catch (ParseException parseException) {
                    errorMessages.add(parseException.getMessage());
                    errors = true;
                    synchToToken(";", "}");
                    lex.peek();
                }
            } else {
                try {
                    Statement stat = parseStatement();
                    n.addStat(stat);
                } catch (ParseException parseException) {
                    errorMessages.add(parseException.getMessage());
                    errors = true;
                    synchToToken(";", "}");
                    lex.peek();
                }
            }
            s = lex.peek();
        }
        return n;
    }
    //Checks declared values
    private Decl parseDecl() throws ParseException {
        VarDecl n = new VarDecl();
        Token s = lex.peek();
        boolean isStatic = false;
        if(s.getSymbol().equals("static")){
            lex.next();
            isStatic = true;
        }
        n.setType(parseType());
        n.getType().setStatic(isStatic);
        n.setName(parseName());
        n.setScope(stab.getCurrentScope());
        s = lex.peek();
        if (s.getSymbol().equals("[")) {
            return parseArrayDecl(n);
        }
        if (s.getSymbol().equals("=")) {
            lex.next();
            n.setInit(parseExpression());
        }
        s = lex.next();
        if (!s.getSymbol().equals(";")) {
            throwParseException("missing ';'", s);
        }
        stab.add(n.getName(), n.getType());
        return n;
    }
    //Checks for array declaration
    public Decl parseArrayDecl(VarDecl v) throws ParseException {
        ArrayDecl n = new ArrayDecl(v);
        Token s = lex.next();
        if (!s.getSymbol().equals("[")) {
            throwParseException("expecting '['", s);
        }
        s = lex.peek();
        if (!s.getSymbol().equals("]")) {
            n.setSize(parseExpression());
        }
        s = lex.peek();
        if (!s.getSymbol().equals("]")) {
            throwParseException("expecting ']'", s);
        }
        lex.next();
        s = lex.peek();
        if (s.getSymbol().equals("=")) {
            lex.next();
            s = lex.peek();
            if (!s.getSymbol().equals("{")) {
                throwParseException("expecting '{'", s);
            }
            lex.next();
            n.setInit(parseExprList());
            s = lex.next();
            if (!s.getSymbol().equals("}")) {
                throwParseException("expecting '}'", s);
            }
        }
        s = lex.next();
        if (!s.getSymbol().equals(";")) {
            throwParseException("missing ';'", s);
        }
        stab.add(n.getName(), n.getType());
        return n;
    }
    //Checks for a valid statement
    private Statement parseStatement() throws ParseException {
        Token s = lex.peek();
        if (s.getSymbol().equals("{")) {
            return parseBlock();
        } else {
            return parseSimpleState();
        }
    }
    //Reads a valid statement and gets its parts
    private Statement parseSimpleState() throws ParseException {
        Token s = lex.peek();
        if (s.getSymbol().equals("for")) {
            return parseForState();
        } else if (s.getSymbol().equals("if")) {
            return parseIfState();
        } else if (s.getSymbol().equals("while")) {
            return parseWhileState();
        } else if (s.getSymbol().equals("return")) {
            return parseReturnState();
        } else if (s.getSymbol().equals("++") || s.getSymbol().equals("--")) {
            return parseIncDecState();
        } else if (s.getSymbol().equals("*")) {
            VarRef ident = parseVarRef();
            return parseAssignState(ident);
        } else if (s.getSType() == SType.ID) {
            Name ident = parseName();
            s = lex.peek();
            if (s.getSymbol().equals("(")) {
                if (!stab.containsFunction(ident.toString())) {
                    throwParseException("unrecognized function", ident.getSymbol());
                }
                return parseProccallState(ident);
            } else {
                //VarRef v = new VarRef();
                VarRef v = parseVarRef(ident);
//                v.setName(ident);
//                if (!stab.contains(v.getName().toString())) {
//                    throwParseException("undeclared identifier", v.getName().getSymbol());
//                } else {
//                    v.setScope(stab.getContainingScope(v.getName().toString()));
//                }
                return parseAssignState(v);
            }
        } else {
            throwParseException("not a valid statement", s);
            return null;
        }
    }
    //Read a for loop and get its parts
    private ForState parseForState() throws ParseException {
        Token s = lex.next();
        if (!s.getSymbol().equals("for")) {
            throwParseException("expecting 'for'", s);
        }
        ForState n = new ForState();
        s = lex.next();
        if (!s.getSymbol().equals("(")) {
            throwParseException("expecting '('", s);
        }
        n.setInit(parseAssignment());
        s = lex.next();
        if (!s.getSymbol().equals(";")) {
            throwParseException("expecting ';'", s);
        }
        n.setTest(parseExpression());
        s = lex.next();
        if (!s.getSymbol().equals(";")) {
            throwParseException("expecting ';'", s);
        }
        n.setIncr(parseAssignment());
        s = lex.next();
        if (!s.getSymbol().equals(")")) {
            throwParseException("expecting ')'", s);
        }
        n.setBody(parseBlock());
        return n;
    }
    //Read an if block and get its parts
    private IfState parseIfState() throws ParseException {
        Token s = lex.next();
        if (!s.getSymbol().equals("if")) {
            throwParseException("expecting 'if'", s);
        }
        IfState n = new IfState();
        s = lex.next();
        if (!s.getSymbol().equals("(")) {
            throwParseException("expecting '('", s);
        }
        n.setTest(parseExpression());
        s = lex.next();
        if (!s.getSymbol().equals(")")) {
            throwParseException("expecting '('", s);
        }
        n.setBody(parseBlock());
        s = lex.peek();
        if (s.getSymbol().equals("else")) {
            lex.next(); // consume else
            n.setElse(parseBlock());
        }
        return n;
    }
    //Read a while block and get its parts
    private WhileState parseWhileState() throws ParseException {
        Token s = lex.next();
        if (!s.getSymbol().equals("while")) {
            throwParseException("expecting 'while'", s);
        }
        WhileState n = new WhileState();
        s = lex.next();
        if (!s.getSymbol().equals("(")) {
            throwParseException("expecting '('", s);
        }
        n.setTest(parseExpression());
        s = lex.next();
        if (!s.getSymbol().equals(")")) {
            throwParseException("expecting '('", s);
        }
        n.setBody(parseBlock());
        return n;
    }
    //If a return statement is necessary, look for a return statement.
    private RetState parseReturnState() throws ParseException {
        Token s = lex.next();
        if (!s.getSymbol().equals("return")) {
            throwParseException("expecting 'return'", s);
        }
        RetState n = new RetState();
        s = lex.peek();
        if (!s.getSymbol().equals(";")) {
            n.setExpr(parseExpression());
        }
        s = lex.next();
        if (!s.getSymbol().equals(";")) {
            throwParseException("missing ';'", s);
        }
        return n;
    }
    //Look for increment or decrement symbols
    private Statement parseIncDecState() throws ParseException {
        Token s = lex.peek();
        if (!s.getSymbol().equals("++") && !s.getSymbol().equals("--")) {
            throwParseException("expecting '++' or '--'", s);
        }
        IncDecState n = new IncDecState();
        n.setOp(parseOp());
        n.setVarRef(parseVarRef());
        s = lex.next();
        if (!s.getSymbol().equals(";")) {
            throwParseException("missing ';'", s);
        }
        return n;
    }
    //Checks for calling a function
    private Proccall parseProccallState(Name ident) throws ParseException {
        Proccall n = parseProccall(ident);
        Token s = lex.next();
        if (!s.getSymbol().equals(";")) {
            throwParseException("missing ';'", s);
        }
        return n;
    }

    private Proccall parseProccall() throws ParseException {
        Name ident = parseName();
        return parseProccall(ident);
    }
    
    private Proccall parseProccall(Name ident) throws ParseException {
        Proccall n = new Proccall();
        n.setIdent(ident);
        Token s = lex.next();
        if (!s.getSymbol().equals("(")) {
            throwParseException("expecting '('", s);
        }
        s = lex.peek();
        if (!s.getSymbol().equals(")")) {
            n.setArgs(parseExprList());
        }
        s = lex.next();
        if (!s.getSymbol().equals(")")) {
            throwParseException("expecting ')'", s);
        }
        return n;
    }
    //Looks over value assignment
    private AssignState parseAssignment() throws ParseException {
        VarRef varRef = parseVarRef();
        return parseAssignment(varRef);
    }
    
    private AssignState parseAssignState(VarRef varRef) throws ParseException {
        AssignState n = parseAssignment(varRef);
        Token s = lex.next();
        if (!s.getSymbol().equals(";")) {
            throwParseException("missing ';'", s);
        }
        return n;
    }
    
    private AssignState parseAssignment(VarRef target) throws ParseException {
        AssignState n = new AssignState();
        n.setIdent(target);
        Token s = lex.next();
        if (!s.getSymbol().equals("=")) {
            throwParseException("expecting '='", s);
        }
        n.setExpression(parseExpression());
        return n;
    }

    // this should only be called if we know we have
    // at least one expression
    private ExprList parseExprList() throws ParseException {
        ExprList n = new ExprList();
        Expression e = parseExpression();
        n.addChild(e);
        Token s = lex.peek();
        while (s.getSymbol().equals(",")) {
            lex.next(); // consume ','
            e = parseExpression();
            n.addChild(e);
            s = lex.peek();
        }
        return n;
    }

    private Expression parseExpression() throws ParseException {
        Expression n = new Expression();
        n.setLhs(parseConjunction());
        Token s = lex.peek();
        if (s.getSymbol().equals("||")) {
            n.setOp(parseOp());
            n.setRhs(parseConjunction());
        }
        return n;
    }

    private Expression parseConjunction() throws ParseException {
        Expression n = new Expression();
        n.setLhs(parseEquality());
        Token s = lex.peek();
        if (s.getSymbol().equals("&&")) {
            n.setOp(parseOp());
            n.setRhs(parseEquality());
        }
        return n;
    }

    private Expression parseEquality() throws ParseException {
        Expression n = new Expression();
        n.setLhs(parseRelation());
        Token s = lex.peek();
        if (s.getSymbol().equals("==") || s.getSymbol().equals("!=")) {
            n.setOp(parseOp());
            n.setRhs(parseRelation());
        }
        return n;
    }

    private Expression parseRelation() throws ParseException {
        Expression n = new Expression();
        n.setLhs(parseAddition());
        Token s = lex.peek();
        if (s.getSymbol().equals(">=") || s.getSymbol().equals("<=")
                || s.getSymbol().equals(">") || s.getSymbol().equals("<")) {
            n.setOp(parseOp());
            n.setRhs(parseAddition());
        }
        return n;
    }

    private Expression parseAddition() throws ParseException {
        Expression n = new Expression();
        n.setLhs(parseTerm());
        Token s = lex.peek();
        if (s.getSymbol().equals("+") || s.getSymbol().equals("-")) {
            n.setOp(parseOp());
            n.setRhs(parseTerm());
        }
        return n;
    }

    private Expression parseTerm() throws ParseException {
        Expression n = new Expression();
        n.setLhs(parseFactor());
        Token s = lex.peek();
        if (s.getSymbol().equals("*") || s.getSymbol().equals("/") || s.getSymbol().equals("%")) {
            n.setOp(parseOp());
            n.setRhs(parseFactor());
        }
        return n;
    }

    private Expression parseFactor() throws ParseException {
        Expression n = new Expression();
        Token s = lex.peek();
        if (s.getSType() == SType.OP) {
            n.setOp(parseOp());
        }
        n.setLhs(parsePrimary());
        return n;
    }

    private Expression parsePrimary() throws ParseException {
        Token s = lex.peek();
        if (s.getSymbol().equals("(")) {
            lex.next();
            Expression e = parseExpression();
            s = lex.next();
            if (!s.getSymbol().equals(")")) {
                throwParseException("expecting ')'", s);
            }
            return e;
        } else if (s.getSType() == SType.ID) {
            Name ident = parseName();
            s = lex.peek();
            if (s.getSymbol().equals("(")) {
                if (!stab.containsFunction(ident.toString())) {
                    throwParseException("unrecognized function", ident.getSymbol());
                }
                return parseFuncall(ident);
            } else {
                return parseVarRef(ident);
            }
        } else if (SType.isUniOp(s.getSymbol())) {
            Operator op = parseOp();
            VarRef vr = parseVarRef();
            Expression ex = new Expression();
            ex.setOp(op);
            ex.setLhs(vr);
            return ex;
        } else if (SType.isLiteral(s.getSymbol())) {
            return parseLiteral();
        } else {
            throwParseException("bad primary value", s);
            return null;
        }
    }

    private Funcall parseFuncall(Name ident) throws ParseException {
        Funcall n = new Funcall();
        n.setName(ident);
        Token s = lex.next();
        if (!s.getSymbol().equals("(")) {
            throwParseException("expecting '('", s);
        }
        s = lex.peek();
        if (!s.getSymbol().equals(")")) {
            n.setArgs(parseExprList());
        }
        s = lex.next();
        if (!s.getSymbol().equals(")")) {
            throwParseException("expecting ')'", s);
        }
        if (!stab.containsFunction(n.getName().toString())) {
            throwParseException("undefined function", n.getName().getSymbol());
        } else {
            n.setScope(stab.getGlobalScope());
        }
        return n;
    }

    private Literal parseLiteral() throws ParseException {
        Token s = lex.next();
        if (!SType.isLiteral(s.getSymbol())) {
            throwParseException("expecting literal value", s);
        }
        Literal n = new Literal();
        n.setSymbol(s);
        return n;
    }

    private Operator parseOp() throws ParseException {
        Token s = lex.next();
        if (s.getSType() != SType.OP) {
            throwParseException("expecting operator", s);
        }
        Operator n = new Operator(s);
        return n;
    }

    private VarRef parseVarRef() throws ParseException {
        VarRef n = new VarRef();
        Token s = lex.peek();
        while (s.getSymbol().equals("*")) {
            lex.next();
            n.incIndirect();
            s = lex.peek();
        }
        n.setName(parseName());
        s = lex.peek();
        if (s.getSymbol().equals("[")) {
            lex.next();
            n.setIndex(parseExpression());
            s = lex.next();
            if (!s.getSymbol().equals("]")) {
                throwParseException("expecting ']'", s);
            }
        }
        if (!stab.contains(n.getName().toString())) {
            throwParseException("undefined identifier", n.getName().getSymbol());
        } else {
            n.setScope(stab.getContainingScope(n.getName().toString()));
        }
        return n;
    }

    private VarRef parseVarRef(Name name) throws ParseException {
        VarRef n = new VarRef();
        n.setName(name);
        Token s = lex.peek();
        if (s.getSymbol().equals("[")) {
            lex.next();
            n.setIndex(parseExpression());
            s = lex.next();
            if (!s.getSymbol().equals("]")) {
                throwParseException("expecting ']'", s);
            }
        }
        if (!stab.contains(n.getName().toString())) {
            throwParseException("undefined identifier", n.getName().getSymbol());
        } else {
            n.setScope(stab.getContainingScope(n.getName().toString()));
        }
        return n;
    }

    private Name parseName() throws ParseException {
        Token s = lex.next();
        if (s.getSType() != SType.ID) {
            throwParseException("expecting identifier", s);
        }
        Name n = new Name(s);
        return n;
    }

    private void synchToToken(String token) {
        synchToToken(token, token);
    }

    private void synchToToken(String token1, String token2) {
        Token s = lex.peek();
        if (s != null && (s.getSymbol().equals(token1) || s.getSymbol().equals(token2))) {
            s = lex.next();
        }
        while (s != null) {
            if (s.getSymbol().equals(token1) || s.getSymbol().equals(token2)) {
                return;
            } else {
                s = lex.next();
            }
        }
    }

    private void throwParseException(String cause, Token token) throws ParseException {
        throw new ParseException(
                String.format(
                        "%s; found %s at line %d, column %d",
                        cause,
                        token.getSymbol(),
                        token.getLine(),
                        token.getCol() + 1));
    }
    // private instance variables
    //
    private final Lexer lex;
    private final SymbolTable stab;
    private final ArrayList<String> errorMessages;
    private boolean errors;
}
