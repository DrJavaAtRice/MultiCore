package edu.rice.cs.drjava.model.definitions.indent.statemachine;

/** 
 * A trigger for a state transition.
 * Calls to <code>canApply</code> on a Trigger will decide based on input and stack context
 * if the transition logic holds.
 */
public interface Trigger
{
  /**
   * Determine if the transition can apply to the input tape and stack.
   * This will not modify the input tape or the stack.
   * @param input The current input tape
   * @param stack The current context
   */
  public boolean canApply(InputTape input, ContextStack stack);
  
  public static Trigger ALWAYS = new Trigger() {
    public boolean canApply(InputTape input, ContextStack stack) {
      return true; 
    }
  };
}