package edu.rice.cs.drjava.model.definitions.indent.statemachine;

import java.util.ArrayList;

/**
 * A state in the state machine.
 * Each state has a series of possible transitions. On transition, it looks at all
 * applicable transitions and selects one to apply.
 */
public class StateSymbol
{
  private String name;
  private ArrayList<StateTransition> transitions;
  private int indent;
  
  /**
   * @param name An identifying name for the state.
   */
  public StateSymbol(String name) {
    this(name, 0);
  }
  
  /**
   * @param name An identifying name for the state.
   * @param indent The indentation level when in this state
   */
  public StateSymbol(String name, int indent) {
    this.name = name;
    this.indent = indent;
    this.transitions = new ArrayList<StateTransition>();
  }
  
  /**
   * Add a new transition for a state.
   * @param transition The new transition to add.
   */
  public void addTransition(StateTransition transisiton) {
    this.transitions.add(transisiton);
  }
  
  /**
   * Follow a single transition from this state. This is likely to modify the input tape or the stack.
   * Each transition on the state is checked and the first applicable transition is applied. Transitions are checked
   * in the order they were added.
   * @param input The current input tape.
   * @param stack The current context
   * @return a new current state after applying a transition.
   */
  public StateSymbol doTransition(InputTape input, ContextStack stack) {
    for(StateTransition transition : transitions) {
      if(transition.canApply(input, stack))
        return transition.apply(input, stack);
    }
    
    throw new RuntimeException("Unable to find valid transition from " + this.toString() + " with next character " + input.peek());
  }
  
  /**
   * The current state may contribute the indentation level.
   * @return The contribution to the indentation level by this state.
   */
  public int getIndentationBase() {
    return indent;
  }
  
  /**
   * Represent a state as a string for the sake of debugging.
   */
  public String toString() {
    return "$State[" + name + "]"; 
  }
}