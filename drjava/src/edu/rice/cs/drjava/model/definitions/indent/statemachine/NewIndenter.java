package edu.rice.cs.drjava.model.definitions.indent.statemachine;

import javax.swing.text.*;
import edu.rice.cs.drjava.model.AbstractDJDocument;

/**
 * A class which can indent a single line of an AbstractDJDocument at a time.
 * 
 * TODO: Cache indendation computation on lines; when indenting many lines sequentially,
 *  most of the computation can be reused.
 */
public class NewIndenter
{
  private PushDownAutomata machine;
  
  private static int SQUIGGLY = 0;
  private static int PAREN = 1;
  private static int SQUARE = 2;
  
  private static int COMMENT_TAG = 4;
  
  /**
   * Construct a new indentation engine.
   * 
   * @param indentLevel The number of spaces to use for a tab.
   */
  public NewIndenter(int indentLevel) {
    StateSymbol normal = new StateSymbol("default");
    StateSymbol linecomment = new StateSymbol("linecomment");
    StateSymbol blockcomment = new StateSymbol("blockcomment");
    StateSymbol instring = new StateSymbol("string");
    StateSymbol inchar = new StateSymbol("char");
    
    normal.addTransition(new StateTransition(normal, new TriggerSubstring("("), new ActionRead(1), new ActionEnterParenContext(PAREN)));
    normal.addTransition(new StateTransition(normal, new Trigger[]{new TriggerSubstring(")"), new TriggerContext(PAREN)}, new Action[]{new ActionRead(1), new ActionLeaveContext(PAREN)})); 
    
    normal.addTransition(new StateTransition(normal, new TriggerSubstring("{"), new ActionRead(1), new ActionEnterContext(SQUIGGLY, indentLevel)));
    normal.addTransition(new StateTransition(normal, new Trigger[]{new TriggerSubstring("}"), new TriggerContext(SQUIGGLY)}, new Action[]{new ActionRead(1), new ActionLeaveContext(SQUIGGLY)}));
    
    normal.addTransition(new StateTransition(linecomment, new TriggerSubstring("//"), new ActionRead(2)));
    normal.addTransition(new StateTransition(blockcomment, new TriggerSubstring("/*"), new ActionRead(2), new ActionEnterContext(COMMENT_TAG, 0)));  
    normal.addTransition(new StateTransition(instring, new TriggerSubstring("\""), new ActionRead(1))); 
    normal.addTransition(new StateTransition(inchar, new TriggerSubstring("'"), new ActionRead(1))); 
    
    normal.addTransition(new StateTransition(normal, Trigger.ALWAYS, new ActionRead(1)));
    
    linecomment.addTransition(new StateTransition(normal, new TriggerSubstring("\n"), new ActionRead(1)));
    linecomment.addTransition(new StateTransition(linecomment, Trigger.ALWAYS, new ActionRead(1)));
    
    blockcomment.addTransition(new StateTransition(normal, new TriggerSubstring("*/"), new ActionRead(1), new ActionLeaveContext(COMMENT_TAG)));
    blockcomment.addTransition(new StateTransition(blockcomment, Trigger.ALWAYS, new ActionRead(1))); 
    
    instring.addTransition(new StateTransition(instring, new TriggerSubstring("\\\\"), new ActionRead(2)));
    instring.addTransition(new StateTransition(instring, new TriggerSubstring("\\\""), new ActionRead(2))); //Handle escaped double-quote
    instring.addTransition(new StateTransition(normal, new TriggerSubstring("\""), new ActionRead(1)));
    instring.addTransition(new StateTransition(instring, Trigger.ALWAYS, new ActionRead(1)));
    
    inchar.addTransition(new StateTransition(inchar, new TriggerSubstring("\\\\"), new ActionRead(2)));
    inchar.addTransition(new StateTransition(inchar, new TriggerSubstring("\\\'"), new ActionRead(2))); //Handle escaped quote
    inchar.addTransition(new StateTransition(normal, new TriggerSubstring("'"), new ActionRead(1)));
    inchar.addTransition(new StateTransition(inchar, Trigger.ALWAYS, new ActionRead(1)));
    
    this.machine = new PushDownAutomata(normal);
  }
  
  /**
   * Indent a single line of an AbstractDJDocument.
   * @param doc The document to indent. The current line is specified by the document position.
   * @param stack The context state of the current line.
   */
  private void indentLine(AbstractDJDocument doc, ContextStack stack) {
    int here = doc.getCurrentLocation();
    
    //EDGE CASE: If } is the first character of a line, the line is indented after removing that brace's context
    //TODO: This actually happens for other types of braces
    try {
      int firstCharPos = doc._getLineFirstCharPos(here);
      if (firstCharPos + 1 <= doc._getLineEndPos(here)) {
        if(doc.getText(firstCharPos, 1).equals("}") && !stack.isEmpty() && stack.peek().getTag() == SQUIGGLY)
          stack.pop();
      }
    }catch(BadLocationException e) {
      //?
    }
    
    int indentation = stack.getIndentationLevel();
    doc.setTab(indentation, here);
  }
  
  /**
   * Indent a single line of an AbstractDJDocument.
   * @param doc The document to indent. The current line is specified by the document position.
   */
  public boolean indent(AbstractDJDocument doc) {
    int here = doc.getCurrentLocation();
    int startLine = doc._getLineStartPos(here);
    
    String text = doc.getText();
    machine.start(new InputTape(text.substring(0, startLine)));
    while(!machine.getInput().atEnd())
      machine.advance();
    
    indentLine(doc, machine.getStack());
    return true;
  }
}