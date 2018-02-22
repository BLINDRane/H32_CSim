/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import java.util.ArrayList;
import java.util.Stack;
import lexer.Token;
import ptn.Name;
import ptn.Type;

/**
 *
 * @author Alan
 */
public class SymbolTable {
//Defines a new symbol table which contains a stack of scopes, as well as lists of functions
    private SymbolTable() {
        global = new Scope(0);
        scopeStack = new Stack<>();
        scopeStack.push(global);
        functionNames = new ArrayList<>();
    }
    
    public static SymbolTable getInstance(){
        return symtab;
    }
    //Adds a new piece to the symbol table, could be a scope, or piece of program
    public void add(Name name, Type type) throws ParseException {
        if (contains(name.toString(), true)) {
            throw new ParseException(String.format(
                    "duplicate identifier; found %s at line %d, column %d",
                    name.getSymbol().getSymbol(),
                    name.getSymbol().getLine(),
                    name.getSymbol().getCol()));
        }
        scopeStack.peek().add(name, type);
    }
    //Add a function to the table
    public void addFunction(String name){
        functionNames.add(name);
    }
    //Ask the table if it contains a specific function
    public boolean containsFunction(String name){
        return functionNames.contains(name);
    }
    //Check the table for a given namespace
    public boolean contains(String token){
        return contains(token, false);
    }
    //Check the table for a given scope
    public boolean contains(String token, boolean currentScopeOnly) {
        Scope scope = scopeStack.peek();
        while (scope != null) {
            if (scope.contains(token)) {
                return true;
            }
            if(currentScopeOnly){
                break;
            }
            scope = scope.getSuperScope();
        }
        return false;
    }
    //Get the space that is relevant for a particular scope
    public Scope getContainingScope(String token){
        Scope scope = scopeStack.peek();
        while(scope != null){
            if(scope.contains(token)){
                return scope;
            }
            scope = scope.getSuperScope();
        }
        return null;
    }
    
    //Retrieve the name of a scope
    public Name getName(String token){
        Scope scope = scopeStack.peek();
        while(scope!=null){
            if(scope.contains(token)){
                return scope.getName(token);
            }
            scope = scope.getSuperScope();
        }
        return null;
    }
    //Retrieve the symbol for a given scope
    public Token getSymbol(String token) {
        Scope scope = scopeStack.peek();
        while (scope != null) {
            if (scope.contains(token)) {
                return scope.getSymbol(token);
            }
            scope = scope.getSuperScope();
        }
        return null;
    }
    //Returns the type of scope (Global, local, etc.)
    public Type getType(String token) {
        Scope scope = scopeStack.peek();
        while (scope != null) {
            if (scope.contains(token)) {
                return scope.getType(token);
            }
            scope = scope.getSuperScope();
        }
        return null;
    }
    //Pushes a new scope onto the stack
    public void pushScope() {
        //Scope scope = new Scope(scopeCount++);
        Scope scope = new Scope(scopeStack.peek().getSubScopeCount());
        scope.linkSuperScope(scopeStack.peek());
        scopeStack.peek().linkSubScope(scope);
        scopeStack.push(scope);
    }
   //Pops the scope stack
    public void popScope() {
        scopeStack.pop();
    }
    //Gets the scope the program is currently in
    public Scope getCurrentScope() {
        return scopeStack.peek();
    }
    //Get the global scope
    public Scope getGlobalScope() {
        return global;
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(global.toString());
        return sb.toString();
    }

     //
    private final static SymbolTable symtab = new SymbolTable();
    //
    private final Scope global;
    private final Stack<Scope> scopeStack;
    private final ArrayList<String> functionNames;

}
