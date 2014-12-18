package edu.rice.cs.drjava.model.definitions.indent.statemachine;

import junit.framework.TestCase;

/**
 * Run several simple functional tests on PushDownAutomata
 */
public class PushDownAutomataTest extends TestCase
{
  /**
   * Generate a simple transition between states.
   */
  private static StateTransition simpleTrans(StateSymbol dest, String trigger) {
    return new StateTransition(dest, new TriggerSubstring(trigger), new ActionRead(trigger));
  }
  
  /**
   * Test the state transitions of a PDA.
   */
  public void testMachineBasic() {    
    StateSymbol s0 = new StateSymbol("0A");
    StateSymbol s1 = new StateSymbol("1A");
    StateSymbol s2 = new StateSymbol("2A");
    
    s0.addTransition(simpleTrans(s1, "A"));
    s0.addTransition(simpleTrans(s0, "B"));
    s1.addTransition(simpleTrans(s2, "A"));
    s1.addTransition(simpleTrans(s0, "B"));
    s2.addTransition(simpleTrans(s2, "A"));
    s2.addTransition(simpleTrans(s0, "B"));
    
    PushDownAutomata machine = new PushDownAutomata(s0);
    
    machine.start(new InputTape("AAABA"));
    assert(machine.getCurrentState() == s0);
    machine.advance();
    assert(machine.getCurrentState() == s1);
    machine.advance();
    assert(machine.getCurrentState() == s2);
    machine.advance();
    assert(machine.getCurrentState() == s2);
    machine.advance();
    assert(machine.getCurrentState() == s0);
    machine.advance();
    assert(machine.getCurrentState() == s1);
  }
  
  /**
   * Test the stack transitions of a PDA.
   */
  public void testMachineContext() {    
    StateSymbol s = new StateSymbol("state");
    
    s.addTransition(new StateTransition(s, new TriggerSubstring("("), new ActionRead("("), new ActionEnterContext(0, 2)));
    
    
    s.addTransition(new StateTransition(s, new Trigger[]{new TriggerSubstring(")")}, new Action[]{new ActionRead(")"), new ActionLeaveContext(0)}));
    
    PushDownAutomata machine = new PushDownAutomata(s);
    
    machine.start(new InputTape("(()())"));
    assert(machine.getStack().getIndentationLevel() == 0);
    machine.advance();
    assert(machine.getStack().getIndentationLevel() == 2);
    machine.advance();
    assert(machine.getStack().getIndentationLevel() == 4);
    machine.advance();
    assert(machine.getStack().getIndentationLevel() == 2);
    machine.advance();
    assert(machine.getStack().getIndentationLevel() == 4);
    machine.advance();
    assert(machine.getStack().getIndentationLevel() == 2);
    machine.advance();
    assert(machine.getStack().getIndentationLevel() == 0);
  }
}