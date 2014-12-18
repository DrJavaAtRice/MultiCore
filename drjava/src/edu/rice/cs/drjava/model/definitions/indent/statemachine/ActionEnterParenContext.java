package edu.rice.cs.drjava.model.definitions.indent.statemachine;

/** 
 * An action for a state transition.
 * Calls to <code>apply</code> will add a new context element to the stack. This context element will increase
 * indentation by an amount based on the current tape location.
 */
public class ActionEnterParenContext implements Action {
  private int tag;
  
  /**
   * @param tag Tag to use to identify the new context
   */
  public ActionEnterParenContext(int tag) {
    this.tag = tag;
  }
  
  /**
   * Apply transition logic to the input tape and stack.
   * In this case, push an additional context element onto the stack.
   * 
   * @param input The current input tape
   * @param stack The current context
   */
  public void apply(InputTape input, ContextStack stack) {
    int line = input.getLinePosition();
    int current = input.getPosition();
    int currentIndentation = current - line;
    stack.push(new ContextStack.ContextSymbol("OpenContext", tag, currentIndentation));
  }
}