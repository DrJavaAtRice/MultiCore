package edu.rice.cs.drjava.model.definitions.indent.statemachine;

import java.util.ArrayList;
import java.lang.Comparable;

/**
 * A push down automata. This is a state machine with an input tape and associated stack.
 * This machine reads the input tape from left to right, changing states and modifying the stack
 * as appropriate.
 */
public class PushDownAutomata
{
  private StateSymbol startingState;
  
  private StateSymbol currentState;
  private ContextStack stack;
  private InputTape input;
  
  /**
   * @param The starting state for this PDA. 
   */
  public PushDownAutomata(StateSymbol startingState) {
   this.startingState = startingState;
  }
  
  /**
   * Restart the machine. Begin running the machine on the provided input.
   * @param input Input tape to process.
   */
  public void start(InputTape input) {
   this.stack = new ContextStack(); 
   this.currentState = startingState;
   this.input = input;
  }
  
  /**
   * Restart the machine. Begin running the machine on the provided input, stack, and state.
   * @param input Input tape to process.
   * @param inputStack Stack to begin with
   * @param inputState State to begin on
   */
  public void start(InputTape input, ContextStack inputStack, StateSymbol inputState) {
   this.stack = inputStack;
   this.currentState = inputState;
   this.input = input;
  }
  
  /**
   * Advance the machine by a single transition.
   */
  public void advance() {
   currentState = currentState.doTransition(input, stack); 
  }
  
  /**
   * @return the underlying input tape.
   */
  protected InputTape getInput() {
   return input; 
  }
  
  /**
   * @return the underlying current state.
   */
  protected StateSymbol getCurrentState() {
   return currentState;  
  }
  
  /**
   * @return the underlying stack.
   */
  protected ContextStack getStack() {
   return stack; 
  }
}