package edu.rice.cs.drjava.model.definitions.indent.statemachine;

/**
 * A transition between two state machine states.
 * Each transition will examine the tape and stack to determine when to fire.
 * When the proper conditions are satisfied, a transition can be applied
 * to (maybe) change the state, input tape, and stack.
 */
public class StateTransition {
  private StateSymbol dest;
  private Trigger[] triggers;
  private Action[] actions;
  
  /**
   * @param dest The new state after applying this transition.
   * @param triggers All conditions which must be satisfied for this transition.
   * @param actions All actions which occur when this transition occurs.
   */
  public StateTransition(StateSymbol dest, Trigger[] triggers, Action[] actions) {
    this.triggers = triggers;
    this.actions = actions;
    this.dest = dest;
  }
  
  /**
   * @param dest The new state after applying this transition.
   * @param trigger The condition which must be satisfied for this transition.
   * @param actions All actions which occur when this transition occurs.
   */
  public StateTransition(StateSymbol dest, Trigger trigger, Action... actions) {
    this(dest, new Trigger[]{trigger}, actions);
  }
  
  /**
   * Apply transition logic to the input tape and stack.
   * This is likely to modify the input tape or the stack.
   * @param input The current input tape
   * @param stack The current context
   */
  public StateSymbol apply(InputTape input, ContextStack stack) {
    for(Action action : actions)
      action.apply(input, stack);
    return dest;
  }
  
  /**
   * Determine if the transition can apply to the input tape and stack.
   * This will not modify the input tape or the stack.
   * @param input The current input tape
   * @param stack The current context
   */
  public boolean canApply(InputTape input, ContextStack stack) {
    for(Trigger trigger : triggers) {
      if(!trigger.canApply(input, stack)) {
        return false;
      }
    }
    return true;
  }
}