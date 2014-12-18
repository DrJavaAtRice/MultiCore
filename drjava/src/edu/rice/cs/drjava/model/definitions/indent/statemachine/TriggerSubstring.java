package edu.rice.cs.drjava.model.definitions.indent.statemachine;

/** 
 * A trigger for a state transition.
 * Calls to <code>canApply</code> will decide if the current input matches some expected value.
 */
public class TriggerSubstring implements Trigger
{
  private String substring;
  
  /**
   * @param substring a string to check for on the tape.
   */
  public TriggerSubstring(String substring) {
    this.substring = substring;
  }
  
  /**
   * Determine if the transition can apply to the input tape and stack.
   * This will not modify the input tape or the stack.
   * 
   * In this case, check if the tape contains a certain string.
   * 
   * @param input The current input tape
   * @param stack The current context
   */
  public boolean canApply(InputTape input, ContextStack stack) {
    if(input.atEnd())
      return false;
    
    return input.nextMatches(substring);
  }
}