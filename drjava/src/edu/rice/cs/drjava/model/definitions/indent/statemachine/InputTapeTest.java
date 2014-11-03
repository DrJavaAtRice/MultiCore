package edu.rice.cs.drjava.model.definitions.indent.statemachine;

import junit.framework.TestCase;

/**
 * Simple functional test of the Input Tape.
 */
public class InputTapeTest extends TestCase
{
  /**
   * A simple test to ensure InputTape reads in the correct order.
   */
  public void testInputTape() {
    String text = "3.14159";
    InputTape tape = new InputTape(text);
    assert(!tape.atEnd());
    assert(tape.peek() == '3');
    tape.step();
    assert(!tape.atEnd());
    assert(tape.peek() == '.');
    tape.step();
    assert(!tape.atEnd());
    assert(tape.peek() == '1');
    tape.step();
    assert(!tape.atEnd());
    assert(tape.peek() == '4');
    tape.step();
    assert(!tape.atEnd());
    assert(tape.peek() == '1');
    tape.step();
    assert(!tape.atEnd());
    assert(tape.peek() == '5');
    tape.step();
    assert(!tape.atEnd());
    assert(tape.peek() == '9');
    tape.step();
    assert(tape.atEnd());
  }
}