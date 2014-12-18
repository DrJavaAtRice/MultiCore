package edu.rice.cs.drjava.model.definitions.indent.statemachine;

import java.util.ArrayList;

/**
 * A class representing a stack. Each element on the stack is tagged with a context id, along with an associated
 *  indentation level. The stack represents a context for a given line of code; indentation for that line can be
 *  computed using only the top element of the stack.
 */
public class ContextStack
{
  private ArrayList<ContextSymbol> base;
  
  public ContextStack() {
   this.base = new ArrayList<ContextSymbol>(); 
  }
  
  public ContextStack(ContextStack toCopy) {
   this.base = new ArrayList<ContextSymbol>(toCopy.base); 
  }
  
  /**
   * Push an additional element to the end of the stack.
   * @param symbol The context to push
   */
  public void push(ContextSymbol symbol) {
   this.base.add(symbol); 
  }
  
  /**
   * Check is the stack is empty.
   * @return true if the stack has no elements.
   */
  public boolean isEmpty() {
    return this.base.size() == 0;
  }
  
  /**
   * Pop and return the last element of the stack.
   * @return the top context on the stack (or null if the stack is empty)
   */
  public ContextSymbol pop() {
   if(this.isEmpty())
     return null;
   return this.base.remove(this.base.size() - 1);
  }  
  
  /**
   * Return the last element of the stack.
   * @return the top context on the stack (or null if the stack is empty)
   */
  public ContextSymbol peek() {
   if(this.isEmpty())
     return null;
   return this.base.get(this.base.size() - 1);
  }
  
  /**
   * Compute the indentation level corresponding to the stack. This is the indentation level of the top element.
   * @return the associated indentation level of this stack.
   */
  public int getIndentationLevel() {
    if(this.isEmpty())
      return 0;
    return this.peek().indentation;
  }
  
  /**
   * An element on the stack. A specific type of context can be identified using its tag.
   */
  public static class ContextSymbol
  {
    private String name;
    private int tag;
    private int indentation;
    
    /**
     * @param name The name of the context
     * @param tag The identifying tag for the context
     * @param indentation The indentation level for the context.
     */
    public ContextSymbol(String name, int tag, int indentation) {
      this.name = name;
      this.tag = tag;
      this.indentation = indentation;
    }
    
    /**
     * @return the identifying tag of the context.
     */
    public int getTag() {
     return tag; 
    }
  }
}