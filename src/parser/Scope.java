/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import lexer.Token;
import ptn.Name;
import ptn.Type;

/**
 *
 * @author Alan
 */
public class Scope {

    //Define a scope
    public Scope(int scopeNum) {
        this.scopeNum = scopeNum;
        table = new HashMap<>();
        parent = null;
        subScope = new ArrayList<>();
    }
    //add a new scope to the table
    public void add(Name name, Type type) {
        STE ste = new STE(name, type);
        table.put(name.toString(), ste);
    }
    
    
    public boolean contains(String symbol) {
        return table.containsKey(symbol);
    }

    public Name getName(String token) {
        if(contains(token)){
            return table.get(token).name;
        }
        if(parent!=null){
            return parent.getName(token);
        }
        return null;
    }

    public Token getSymbol(String token) {
        if(contains(token)){
            return table.get(token).name.getSymbol();
        }
        if(parent!=null){
            return parent.getSymbol(token);
        }
        return null;
    }
    //Check to see if scope is global or other
    public Type getType(String token) {
        if(contains(token)){
            return table.get(token).type;
        }
        if(parent!=null){
            return parent.getType(token);
        }
        return null;
    }
    //Get the relative offset of the scope in relation to the table
    public int getRelOffset(String token){
        if(contains(token)){
            return table.get(token).relOffset;
        }
        if(parent!=null){
            return parent.getRelOffset(token);
        }
        return 0;
    }
    
    //Set the relative offset of the scope
    public void setRelOffset(String token, int value){
        if(contains(token)){
            table.get(token).relOffset = value;
        } else if(parent!=null){
            parent.setRelOffset(token, value);
        } else {
            throw new RuntimeException("Uable to set relOffset " + value + " for symbol " + token);
        }
    }
    
    //Add a subscope to a scope
    public void linkSubScope(Scope scope) {
        subScope.add(scope);
    }
    
    //Parent a scope to another scope
    public void linkSuperScope(Scope scope) {
        this.parent = scope;
    }
    
    //Get the superscope of a given scope
    public Scope getSuperScope() {
        return parent;
    }
    
    //Get the number of subscopes for a scope
    public int getSubScopeCount(){
        return this.subScope.size();
    }
    
    //Get the label of this scope
    public String label() {
        return label(this);
    }
    //Defines the label of the scope as a unique ID
    private String label(Scope scope) {
        if (scope == null) {
            return "";
        }
        return label(scope.parent) + scope.scopeNum + "_";
    }
    
    public Iterator<String> getKeyIterator(){
        return table.keySet().iterator();
    }
    
    public Iterator<Scope> getSubscopeIterator(){
        return subScope.iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("   Scope ");
        sb.append(label());
        sb.append(":\n");
        Iterator<String> keys = getKeyIterator();
        while (keys.hasNext()) {
            sb.append("      ");
            sb.append(table.get(keys.next()).toString());
            sb.append("\n");
        }
        Iterator<Scope> subScopes = getSubscopeIterator();
        while (subScopes.hasNext()) {
            sb.append("\n");
            sb.append(subScopes.next().toString());
        }
        return sb.toString();
    }

    class STE {
        
        public STE(Name name, Type type) {
            this.name = name;
            this.type = type;
            relOffset = 0;

        }

        public int getRelOffset() {
            return relOffset;
        }

        public void setRelOffset(int relOffset) {
            this.relOffset = relOffset;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<Name: ");
            sb.append(name);
            sb.append(", Type: ");
            sb.append(type);
            sb.append(">");
            return sb.toString();
        }
        //
        private final Name name;
        private final Type type;
        private int relOffset;
    }

    //
    private final int scopeNum;
    private final HashMap<String, STE> table;
    private Scope parent;
    private final ArrayList<Scope> subScope;

}
