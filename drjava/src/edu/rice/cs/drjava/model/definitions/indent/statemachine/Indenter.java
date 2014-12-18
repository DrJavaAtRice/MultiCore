package edu.rice.cs.drjava.model.definitions.indent.statemachine;

import javax.swing.text.*;
import edu.rice.cs.drjava.model.AbstractDJDocument;
/**
 * A class which can indent a single line of an AbstractDJDocument at a time.
 * 
 * TODO: Cache indendation computation on lines; when indenting many lines sequentially,
 *  most of the computation can be reused.
 */
public class Indenter
{
  /** Enumeration of reasons why indentation may be preformed. */
  public enum IndentReason {
    /** Indicates that an enter key press caused the indentation.  This is important for some rules dealing with stars
      * at the line start in multiline comments
      */
    ENTER_KEY_PRESS,
      /** Indicates that indentation was started for some other reason.  This is important for some rules dealing with stars
        * at the line start in multiline comments
        */
    OTHER
  }
  
  private PushDownAutomata machine;
  private StateSymbol startingState;
  
  private static int SQUIGGLY = 0;
  private static int PAREN = 1;
  private static int SQUARE = 2;
  
  private static int ARRAY = 3;
  private static int COMMENT_TAG = 4;
  
  private static int ANNOTATION = 5;
  private static int IN_CASE = 6;
  
  /**
   * Construct a new indentation engine.
   * 
   * @param indentLevel The number of spaces to use for a tab.
   */
  public Indenter(int indentLevel) {
    this.build(indentLevel);
  }
  
  public void build(int indentLevel) {
    StateSymbol start = new StateSymbol("start_statement");
    StateSymbol normal = new StateSymbol("inside_statement", indentLevel);
    StateSymbol end = new StateSymbol("end_statement");   
    
    StateSymbol openingbrace = new StateSymbol("opening_brace");
    StateSymbol equals = new StateSymbol("after_equals");
    StateSymbol colon = new StateSymbol("after_colon");
    
    
    setupCommentStates(start, "start");
    start.addTransition(new StateTransition(start, new TriggerSubstring(" "), new ActionRead(1)));
    start.addTransition(new StateTransition(start, new TriggerSubstring("\n"), new ActionRead(1)));   
    start.addTransition(new StateTransition(normal, new TriggerChar('@'), new ActionRead(1), new ActionEnterContext(ANNOTATION, 0)));
    start.addTransition(new StateTransition(normal, Trigger.ALWAYS));
    
    setupCommentStates(end, "end");
    end.addTransition(new StateTransition(end, new TriggerSubstring(" "), new ActionRead(1)));
    end.addTransition(new StateTransition(start, new TriggerSubstring("\n"), new ActionRead("\n")));
    end.addTransition(new StateTransition(start, Trigger.ALWAYS));
    
    setupCommentStates(normal, "normal");
    setupStringStates(normal, "normal");
    setupBraceTransitions(start, normal, end, '(', ')', PAREN);
    setupBraceTransitions(start, normal, end, '[', ']', SQUARE);
    
    normal.addTransition(new StateTransition(end, new TriggerSubstring(";"), new ActionRead(";")));
    normal.addTransition(new StateTransition(end, new Trigger[]{new TriggerSubstring("\n"), new TriggerContext(ANNOTATION)}, new Action[]{new ActionLeaveContext(ANNOTATION)})); 
    
    //Detect { and }. Note { might open a new code context OR open an array, while } can close both
    normal.addTransition(new StateTransition(normal, new Trigger[]{new TriggerChar('{'), new TriggerContext(ARRAY)}, new Action[]{new ActionRead(1), new ActionEnterContext(ARRAY, indentLevel)}));
    normal.addTransition(new StateTransition(openingbrace, new TriggerChar('{'), new ActionRead(1)));
    normal.addTransition(new StateTransition(normal, new Trigger[]{new TriggerChar('}'), new TriggerContext(IN_CASE)}, new Action[]{new ActionLeaveContext(IN_CASE)}));
    normal.addTransition(new StateTransition(end, new Trigger[]{new TriggerChar('}'), new TriggerContext(SQUIGGLY)}, new Action[]{new ActionRead(1), new ActionLeaveContext(SQUIGGLY)}));
    normal.addTransition(new StateTransition(end, new Trigger[]{new TriggerChar('}'), new TriggerContext(ARRAY)}, new Action[]{new ActionRead(1), new ActionLeaveContext(ARRAY)}));
    
    //Determine if ':' ends the line
    normal.addTransition(new StateTransition(colon, new TriggerChar(':'), new ActionRead(1)));
    
    setupCommentStates(colon, "after_color");
    //If we are already directly in a case context, there is no need to enter another one.
    colon.addTransition(new StateTransition(end, new TriggerContext(IN_CASE))); 
    colon.addTransition(new StateTransition(equals, new TriggerChar(' '), new ActionRead(1)));
    colon.addTransition(new StateTransition(start, new TriggerChar('\n'), new ActionRead(1), new ActionEnterContext(IN_CASE, indentLevel)));
    colon.addTransition(new StateTransition(normal, Trigger.ALWAYS));  
    
    //={ opens an array (with any amount of spaces inbetween
    normal.addTransition(new StateTransition(equals, new TriggerChar('='), new ActionRead(1)));
    equals.addTransition(new StateTransition(equals, new TriggerChar(' '), new ActionRead(1)));
    equals.addTransition(new StateTransition(normal, new TriggerChar('{'), new ActionRead(1), new ActionEnterContext(ARRAY, 0)));
    equals.addTransition(new StateTransition(normal, Trigger.ALWAYS));
    
    //If there is text on the line opening a { context, it counts as an array initialization
    openingbrace.addTransition(new StateTransition(openingbrace, new TriggerChar(' '), new ActionRead(1)));
    
    openingbrace.addTransition(new StateTransition(openingbrace, new Trigger[]{new TriggerChar('\n'), new TriggerContext(PAREN)}, new Action[]{new ActionLeaveContext(PAREN)}));
    openingbrace.addTransition(new StateTransition(openingbrace, new Trigger[]{new TriggerChar('\n'), new TriggerContext(SQUARE)}, new Action[]{new ActionLeaveContext(SQUARE)}));
    
    openingbrace.addTransition(new StateTransition(start, new TriggerChar('\n'), new ActionRead(1), new ActionEnterContext(SQUIGGLY, indentLevel)));
    openingbrace.addTransition(new StateTransition(start, Trigger.ALWAYS, new ActionEnterContext(ARRAY, 0)));    
    
    //Ignore all other characters
    normal.addTransition(new StateTransition(normal, Trigger.ALWAYS, new ActionRead(1)));
    
    this.startingState = start;
    this.machine = new PushDownAutomata(this.startingState);
  }
  
  private void setupBraceTransitions(StateSymbol start, StateSymbol inside, StateSymbol end, char open, char close, int contextId) {
    inside.addTransition(new StateTransition(start, new TriggerChar(open), new ActionEnterParenContext(contextId), new ActionRead(1)));
    inside.addTransition(new StateTransition(inside, new Trigger[]{new TriggerChar(close), new TriggerContext(contextId)}, new Action[]{new ActionRead(1), new ActionLeaveContext(contextId)})); 
    
    Trigger onOperation = new TriggerAnyChar(',', '+', '-', '*', '/', '&', '%', '|');
    inside.addTransition(new StateTransition(end, new Trigger[]{new TriggerContext(contextId), onOperation}, new Action[]{new ActionRead(1)}));
  }
  
  private void setupCommentStates(StateSymbol base, String name) {
    StateSymbol linecomment = new StateSymbol(name + "_linecomment");
    StateSymbol blockcomment = new StateSymbol(name + "_blockcomment", 1);
    
    base.addTransition(new StateTransition(linecomment, new TriggerSubstring("//"), new ActionRead(2)));
    base.addTransition(new StateTransition(blockcomment, new TriggerSubstring("/*"), new ActionRead(2), new ActionEnterContext(COMMENT_TAG, 0)));  
    
    linecomment.addTransition(new StateTransition(base, new TriggerSubstring("\n"))); //exit the comment on newline
    linecomment.addTransition(new StateTransition(linecomment, Trigger.ALWAYS, new ActionRead(1)));
    
    blockcomment.addTransition(new StateTransition(base, new TriggerSubstring("*/"), new ActionRead(2), new ActionLeaveContext(COMMENT_TAG)));
    blockcomment.addTransition(new StateTransition(blockcomment, Trigger.ALWAYS, new ActionRead(1))); 
  }
  
  private void setupStringStates(StateSymbol base, String name) {
    StateSymbol instring = new StateSymbol(name + "_string");
    StateSymbol inchar = new StateSymbol(name + "_char");
    
    base.addTransition(new StateTransition(instring, new TriggerSubstring("\""), new ActionRead(1))); 
    base.addTransition(new StateTransition(inchar, new TriggerSubstring("'"), new ActionRead(1))); 
    
    instring.addTransition(new StateTransition(instring, new TriggerSubstring("\\\\"), new ActionRead(2)));
    instring.addTransition(new StateTransition(instring, new TriggerSubstring("\\\""), new ActionRead(2))); //Handle escaped double-quote
    instring.addTransition(new StateTransition(base, new TriggerSubstring("\""), new ActionRead(1)));
    instring.addTransition(new StateTransition(base, new TriggerSubstring("\n"))); //exit the string on newline
    instring.addTransition(new StateTransition(instring, Trigger.ALWAYS, new ActionRead(1)));
    
    inchar.addTransition(new StateTransition(inchar, new TriggerSubstring("\\\\"), new ActionRead(2)));
    inchar.addTransition(new StateTransition(inchar, new TriggerSubstring("\\\'"), new ActionRead(2))); //Handle escaped quote
    inchar.addTransition(new StateTransition(base, new TriggerSubstring("'"), new ActionRead(1)));
    inchar.addTransition(new StateTransition(base, new TriggerSubstring("\n"))); //exit the char on newline
    inchar.addTransition(new StateTransition(inchar, Trigger.ALWAYS, new ActionRead(1)));
  }
  
  private static String getFirstChar(AbstractDJDocument doc, boolean acceptComments) {
    int here = doc.getCurrentLocation();
    
    try {
      int loc = doc.getFirstNonWSCharPos(here, acceptComments);
      if(loc != -1 && loc <= doc._getLineEndPos(here))
        return doc.getText(loc, 1);
    } catch(BadLocationException e) {
      //fallthrough 
    }
    return "";
  }
  
  private static String getLastChar(AbstractDJDocument doc) {
    int here = doc.getCurrentLocation();
    int end = doc._getLineEndPos(here);
    
    try {
      int loc = doc._findPrevNonWSCharPos(end);
      if(loc != -1 && loc <= doc._getLineEndPos(here))
        return doc.getText(loc, 1);
    } catch(BadLocationException e) {
      //fallthrough 
    }
    return "";
  }
  
  /**
   * Indent a single line of an AbstractDJDocument.
   * @param doc The document to indent. The current line is specified by the document position.
   * @param stack The context state of the current line.
   @param currentState The state of the current document location.
   */
  private void indentLine(AbstractDJDocument doc, ContextStack stack, StateSymbol currentState, Indenter.IndentReason reason) {
    int here = doc.getCurrentLocation();
    int end = doc._getLineEndPos(here);
    
    //Special case: If // is fully left-justified, do not indent the line
    try {
      if(doc.getText(here, 2).equals("//")) {
        return;
      }
    }catch(BadLocationException e) {
      
    }
    
    String first = getFirstChar(doc, false);
    String firstReal = getFirstChar(doc, true);
    
    if(reason == Indenter.IndentReason.ENTER_KEY_PRESS || here == end) {
      //Special case: If a block comment line does not already include a *, add it.
      if(!firstReal.equals("*") && !stack.isEmpty() && stack.peek().getTag() == COMMENT_TAG) {
        doc.setTab("* ", here);
      }
    }
    
    //Special case: } is not included in its indentation level.
    if(first.equals("}") && !stack.isEmpty()) {
      if(stack.peek().getTag() == IN_CASE)
        stack.pop();
      
      if(!stack.isEmpty() && (stack.peek().getTag() == SQUIGGLY || stack.peek().getTag() == ARRAY))
        stack.pop();
    }
    
    //Special case: a line ending in : is unindented
    if(!stack.isEmpty() && stack.peek().getTag() == IN_CASE) {
      if(getLastChar(doc).equals(":"))
        stack.pop();
    }
    
    int indentation = stack.getIndentationLevel() + currentState.getIndentationBase();
    
    //Special case: Do not include line-continue indentation with { and }
    if(first.equals("{") || first.equals("}")) {
      if(stack.isEmpty() || stack.peek().getTag() != ARRAY)
        indentation -= currentState.getIndentationBase(); 
    }
    
    doc.setTab(indentation, here);
  }
  
  /**
   * Indent a single line of an AbstractDJDocument.
   * @param doc The document to indent. The current line is specified by the document position.
   * @param reason The reason this line is being indented
   * @returns The running state at the end of the computation, to be returned if the next line is indented.
   */
  public RunningState indent(AbstractDJDocument doc, Indenter.IndentReason reason) {
    return indent(doc, reason, new RunningState());
  }
  
  /**
   * Indent a single line of an AbstractDJDocument.
   * @param doc The document to indent. The current line is specified by the document position.
   * @param reason The reason this line is being indented
   * @param cache The previous running state on this document
   * @returns The running state at the end of the computation, to be returned if the next line is indented.
   */
  public RunningState indent(AbstractDJDocument doc, Indenter.IndentReason reason, RunningState cache) {
    int here = doc.getCurrentLocation();
    int startLine = doc._getLineStartPos(here);
    
    if(cache == null || cache.position > here) {
      cache = new RunningState();
    }
    
    String text = doc.getText();
    machine.start(new InputTape(text.substring(cache.position, startLine)), cache.stack, cache.state);
    while(!machine.getInput().atEnd())
      machine.advance();
    
    indentLine(doc, new ContextStack(machine.getStack()), machine.getCurrentState(), reason);
    
    return new RunningState(doc.getCurrentLocation(), machine);
  }
  
  public class RunningState
  {
    private final int position;
    private final ContextStack stack;
    private final StateSymbol state;
    
    public RunningState() {
      this.position = 0;
      this.stack = new ContextStack();
      this.state = startingState;
    }
    
    public RunningState(int fixedPosition, PushDownAutomata machine) {
      this.position = fixedPosition;
      this.stack = machine.getStack();
      this.state = machine.getCurrentState();
    }
  }
}