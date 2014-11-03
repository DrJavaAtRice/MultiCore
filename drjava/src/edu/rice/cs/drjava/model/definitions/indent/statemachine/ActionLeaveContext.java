package edu.rice.cs.drjava.model.definitions.indent.statemachine;

/** 
 * An action for a state transition.
 * Calls to <code>apply</code> will pop a specific context element from the stack.
 */
public class ActionLeaveContext implements Action {
  private int tag;
  
  /**
   * @param tag Tag to use to identify the context to remove.
   */
  public ActionLeaveContext(int tag) {
    this.tag = tag;
  }
  
  /**
   * Apply transition logic to the input tape and stack.
   * In this case, pop an existings context element from the stack.
   * 
   * @param input The current input tape
   * @param stack The current context
   */
  public void apply(InputTape input, ContextStack stack) {
    if(stack.isEmpty()) {
     throw new RuntimeException("Error: Attempting to pop from empty stack"); 
    }
    stack.pop();
  }
}