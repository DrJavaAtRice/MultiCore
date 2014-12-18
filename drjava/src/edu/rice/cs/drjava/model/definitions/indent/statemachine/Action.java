package edu.rice.cs.drjava.model.definitions.indent.statemachine;

/** 
 * An action for a state transition.
 * Calls to <code>apply</code> will modify the input tape and stack to apply the transition logic.
 */
public interface Action 
{
  /**
   * Apply transition logic to the input tape and stack.
   * This is likely to modify the input tape or the stack.
   * @param input The current input tape
   * @param stack The current context
   */
  public void apply(InputTape input, ContextStack stack);
}