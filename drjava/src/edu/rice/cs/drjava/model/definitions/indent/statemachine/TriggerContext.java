package edu.rice.cs.drjava.model.definitions.indent.statemachine;

/** 
 * A trigger for a state transition.
 * Calls to <code>canApply</code> will decide if the stack is in the proper context.
 */
public class TriggerContext implements Trigger
{
  private int tag;
  
  /**
   * @param tag The context identifier to check for.
   */
  public TriggerContext(int tag) {
    this.tag = tag;
  }
  
  /**
   * Determine if the transition can apply to the input tape and stack.
   * This will not modify the input tape or the stack.
   * 
   * In this case, check if we are currently in a specific context.
   * 
   * @param input The current input tape
   * @param stack The current context
   */
  public boolean canApply(InputTape input, ContextStack stack) {
    if(stack.isEmpty())
      return false;
    
    ContextStack.ContextSymbol top = stack.peek();
    return top.getTag() == tag;
  }
}