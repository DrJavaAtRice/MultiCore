package edu.rice.cs.drjava.model.definitions.indent.statemachine;

/**
 * A class representing an input tape. It reads a string from left to right, allowing reads only from the head.
 */
public class InputTape
{
  private String content;
  private int position;
  private int lineStart;
  
  /**
   * Create a new tape which reads the specified string from left to right.
   * 
   * @param Contents to use for the tape.
   */ 
  public InputTape(String content) {
    this.content = content;
    this.position = 0;
    this.lineStart = 0;
  }
  
  /**
   * @return The character currently selected by the tape.
   */
  public char peek() {
    return this.content.charAt(position);
  }
  
  /**
   * Determine if the future characters on a tape matches a specified substring.
   * @param substring The string to search for
   * @return true if the input matches the contents of the tape at the head.
   */
  public boolean nextMatches(String substring) {
    if((substring.length() + position) > content.length())
      return false;
    
    return content.substring(position, position + substring.length()).equals(substring);
  }
  
  /**
   * Determine if the future character on a tape matches a specified character.
   * @param check The character to check for
   * @return true if the input matches the contents of the tape at the head.
   */
  public boolean nextMatches(char check) {
    if(position >= content.length())
      return false;
    
    return content.charAt(position) == check;
  }
  
  /**
   * @param length The number of steps to advance the head.
   */
  public void step(int length) {
    this.position += length;
    if(this.position > this.content.length())
      this.position = this.content.length();
  }
  
  /**
   * Advance the head by a single step.
   */
  public void step() {
    step(1);
  }
  
  /**
   * @return true if the tape has read the entire base string.
   */
  public boolean atEnd() {
    return this.content.length() == this.position; 
  }
  
  /**
   * @return the current position of the tape head in the string.
   */
  public int getPosition() {
    return position;
  }
  
  /**
   * @return the most recent new line in the string.
   * //TODO: Cache this more often? For now, it is only called when the PDA encounters a (
   */
  public int getLinePosition() {
    return this.content.lastIndexOf("\n", position);
  }
  
  /**
   * Represent a tape as a string for the sake of debugging.
   */
  public String toString() {
    if(this.content.length() > 10) {
      return "$InputTape[" + content.substring(0, 10) + "..., " + position + "]";
    } else {
      return "$InputTape[" + content + ", " + position + "]";
    }
  }
}