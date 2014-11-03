package edu.rice.cs.drjava.model.definitions.indent.statemachine;

/** 
 * An action for a state transition.
 * Calls to <code>apply</code> will advance the input tape by a fixed amount.
 */
public class ActionRead implements Action {
  private int size;
  
  /**
   * @param substring Expected string to read.
   */
  public ActionRead(String substring) {
    this.size = substring.length();
  }
  
  /**
   * @param size Number of character to read.
   */
  public ActionRead(int size) {
    this.size = size;
  }
  
  /**
   * Apply transition logic to the input tape and stack.
   * In this case, read a fixed number of characters from the tape.
   * 
   * @param input The current input tape
   * @param stack The current context
   */
  public void apply(InputTape input, ContextStack stack) {
    if(input.atEnd()) {
     throw new RuntimeException("Error: Attempting to read from empty tape"); 
    }
    
    input.step(this.size);
  }
}