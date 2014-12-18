package edu.rice.cs.drjava.model.definitions.indent.statemachine;

/** 
 * A trigger for a state transition.
 * Calls to <code>canApply</code> will decide if the current input matches any of several expected chars.
 */
public class TriggerAnyChar implements Trigger
{
  private char[] chars;
  
  /**
   * @param chars An array of possible expected input values
   */
  public TriggerAnyChar(char... chars) {
    this.chars = chars;
  }
  
  /**
   * Determine if the transition can apply to the input tape and stack.
   * This will not modify the input tape or the stack.
   * 
   * In this case, check if the tape contains any of several characters.
   * 
   * @param input The current input tape
   * @param stack The current context
   */
  public boolean canApply(InputTape input, ContextStack stack) {
    if(input.atEnd())
      return false;
    
    for(char c : chars) {
      if(input.nextMatches(c))
        return true;
    }
    return false;
  }
}