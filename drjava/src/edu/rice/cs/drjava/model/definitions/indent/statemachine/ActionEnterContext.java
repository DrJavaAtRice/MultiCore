package edu.rice.cs.drjava.model.definitions.indent.statemachine;
  
/** 
 * An action for a state transition.
 * Calls to <code>apply</code> will add a new context element to the stack. This context element will increase
 * indentation by a fixed amount.
 */
public class ActionEnterContext implements Action {
  private int tag;
  private int extraIndent;
  
  /**
   * @param tag Tag to use to identify the new context
   * @param extraIndent Fixed additional indentation to add with the new context.
   */
  public ActionEnterContext(int tag, int extraIndent) {
    this.tag = tag;
    this.extraIndent = extraIndent;
  }
  
  /**
   * Apply transition logic to the input tape and stack.
   * In this case, push an additional context element onto the stack.
   * 
   * @param input The current input tape
   * @param stack The current context
   */
  public void apply(InputTape input, ContextStack stack) {
    int currentIndentation = stack.getIndentationLevel();
    stack.push(new ContextStack.ContextSymbol("OpenContext", tag, currentIndentation + extraIndent));
  }
}