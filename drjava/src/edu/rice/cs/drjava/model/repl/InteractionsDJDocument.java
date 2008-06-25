/*BEGIN_COPYRIGHT_BLOCK
 *
 * Copyright (c) 2001-2008, JavaPLT group at Rice University (drjava@rice.edu)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the names of DrJava, the JavaPLT group, Rice University, nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software is Open Source Initiative approved Open Source Software.
 * Open Source Initative Approved is a trademark of the Open Source Initiative.
 * 
 * This file is part of DrJava.  Download the current version of this project
 * from http://www.drjava.org/ or http://sourceforge.net/projects/drjava/
 * 
 * END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.model.repl;

import edu.rice.cs.drjava.model.AbstractDJDocument;
import edu.rice.cs.drjava.model.definitions.indent.Indenter;
import edu.rice.cs.drjava.model.definitions.reducedmodel.*;

import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.util.UnexpectedException;
import edu.rice.cs.util.text.EditDocumentException;
import edu.rice.cs.util.text.ConsoleDocumentInterface;
import edu.rice.cs.util.text.ConsoleDocument;

import java.io.*;
import java.awt.*;
import java.util.List;
import java.util.LinkedList;
import javax.swing.text.AbstractDocument;

import static edu.rice.cs.drjava.model.definitions.ColoringView.*;

/** Represents a Swing-based InteractionsDocument. Extends AbstractDJDocument which contains code shared by
  * the Swing interactions and definitions documents.
  */
public class InteractionsDJDocument extends AbstractDJDocument implements ConsoleDocumentInterface {
  
  /** Whether the document currently has a prompt and is ready to accept input. */
  private volatile boolean _hasPrompt;
  
  /** A flag indicating that the interpreter was recently reset, and to reset the styles list 
    * the next time a style is added. Cannot reset immediately because then the styles would be lost while 
    * the interactions pane is resetting.
    */
  private volatile boolean _toClear = false;
  
  /** Standard constructor. */
  public InteractionsDJDocument() { 
    super(); 
    _hasPrompt = false;
  } 
  
  public boolean hasPrompt() { return _hasPrompt; }
  
  /** Sets the _hasPrompt property. 
    * @param val new boolean value for _hasPrompt.
    */
  public void setHasPrompt(boolean val) { 
    acquireWriteLock();
    _hasPrompt = val;
    releaseWriteLock();
  }
  
  protected int startCompoundEdit() { return 0; /* Do nothing */ }
  protected void endCompoundEdit(int key) { /* Do nothing */ }
  protected void endLastCompoundEdit() { /* Do nothing */ }
  protected void addUndoRedo(AbstractDocument.DefaultDocumentEvent chng, Runnable undoCommand, Runnable doCommand) { }
  protected void _styleChanged() { /* Do nothing */ }
  
  /** Returns a new indenter. Eventually to be used to return an interactions indenter */
  protected Indenter makeNewIndenter(int indentLevel) { return new Indenter(indentLevel); }
  
  /** A list of styles and their locations augmenting this document.  This augmentation is NOT part of the reduced
    * model; it a separate extension that uses itself as a mutual exclusion lock.  This list holds pairs of location
    * intervals and strings (identifying styles).  In essence it maps regions to colors (??).
    * in the document and styles, which is basically a map of regions where the coloring view that is now attached to
    * the Interactions Pane.  It is not allowed to use the reduced model to determine the color settings when 
    * rendering text. (Why not? -- Corky)  We keep a list of all places where styles not considered by the reduced 
    * are being used, such as System.out, System.err, and the various return styles for Strings and other Objects.  
    * Since the LinkedList class is not thread safe,  we have to synchronized all methods that access pointers in 
    * _stylesList and the associated boolean _toClear.
    */
  private List<Pair<Pair<Integer,Integer>,String>> _stylesList = new LinkedList<Pair<Pair<Integer,Integer>,String>>();
  
  /** Adds the given coloring style to the styles list.  Assumes that the document ReadLock is already held. */
  public void addColoring(int start, int end, String style) {
    synchronized(_stylesList) {  // unnecessary since WriteLock already held
      if (_toClear) {
        _stylesList.clear();    
        _toClear = false;
      }
      if (style != null)
        _stylesList.add(0, new Pair<Pair<Integer,Integer>,String>
                        (new Pair<Integer,Integer>(Integer.valueOf(start),Integer.valueOf(end)), style));
    }
  }
  
  /** Accessor method used to copy contents of _stylesList to an array.  Used in test cases. */
  public Pair<Pair<Integer, Integer>, String>[] getStyles() { 
    acquireReadLock();
    synchronized(_stylesList) {
      try { 
        // TODO: file javac bug report concerning placement of @SuppressWarnings.  Fails if rhs of result binding is used as body of return statement.
        @SuppressWarnings("unchecked")
        Pair<Pair<Integer, Integer>, String>[] result = 
          (Pair<Pair<Integer, Integer>, String>[]) (_stylesList.toArray(new Pair[0]));
        return result;
      }
      finally { releaseReadLock(); }
    }
  }
  
  /** Attempts to set the coloring on the graphics based upon the content of the styles list
    * returns false if the point is not in the list.  Assumes that ReadLock is already held.
    */
  public boolean setColoring(int point, Graphics g) {
    synchronized(_stylesList) {
      for(Pair<Pair<Integer,Integer>,String> p :  _stylesList) {
        Pair<Integer,Integer> loc = p.first();
        if (loc.first() <= point && loc.second() >= point) {
          if (p.second().equals(InteractionsDocument.ERROR_STYLE)) {
            //DrJava.consoleErr().println("Error Style");
            g.setColor(ERROR_COLOR);   
            g.setFont(g.getFont().deriveFont(Font.BOLD));
          }
          else if (p.second().equals(InteractionsDocument.DEBUGGER_STYLE)) {
            //DrJava.consoleErr().println("Debugger Style");
            g.setColor(DEBUGGER_COLOR);
            g.setFont(g.getFont().deriveFont(Font.BOLD));
          }
          else if (p.second().equals(ConsoleDocument.SYSTEM_OUT_STYLE)) {
            //DrJava.consoleErr().println("System.out Style");
            g.setColor(INTERACTIONS_SYSTEM_OUT_COLOR);
            g.setFont(MAIN_FONT);
          }
          else if (p.second().equals(ConsoleDocument.SYSTEM_IN_STYLE)) {
            //DrJava.consoleErr().println("System.in Style");
            g.setColor(INTERACTIONS_SYSTEM_IN_COLOR);
            g.setFont(MAIN_FONT);
          }
          else if (p.second().equals(ConsoleDocument.SYSTEM_ERR_STYLE)) {
            //DrJava.consoleErr().println("System.err Style");
            g.setColor(INTERACTIONS_SYSTEM_ERR_COLOR);
            g.setFont(MAIN_FONT);
          }
          else if (p.second().equals(InteractionsDocument.OBJECT_RETURN_STYLE)) {
            g.setColor(NORMAL_COLOR);
            g.setFont(MAIN_FONT);
          }
          else if (p.second().equals(InteractionsDocument.STRING_RETURN_STYLE)) {
            g.setColor(DOUBLE_QUOTED_COLOR);
            g.setFont(MAIN_FONT);
          }
          else if (p.second().equals(InteractionsDocument.NUMBER_RETURN_STYLE)) {
            g.setColor(NUMBER_COLOR);
            g.setFont(MAIN_FONT);
          }
          else if (p.second().equals(InteractionsDocument.CHARACTER_RETURN_STYLE)) {
            g.setColor(SINGLE_QUOTED_COLOR);
            g.setFont(MAIN_FONT);
          }
          else return false; /* Normal text color */ 
          
          return true;
        }
      }
      return false;
    }
  }
  
  /** Attempts to set the font on the graphics context based upon the styles held in the styles list. Assumes that
    * ReadLock is already held. 
    */
  public void setBoldFonts(int point, Graphics g) {
    synchronized(_stylesList) {
      for(Pair<Pair<Integer,Integer>,String> p :  _stylesList) {
        Pair<Integer,Integer> loc = p.first();
        if (loc.first() <= point && loc.second() >= point) {
          if (p.second().equals(InteractionsDocument.ERROR_STYLE))
            g.setFont(g.getFont().deriveFont(Font.BOLD));
          else if (p.second().equals(InteractionsDocument.DEBUGGER_STYLE))
            g.setFont(g.getFont().deriveFont(Font.BOLD));
          else  g.setFont(MAIN_FONT);
          return;
        }
      }
    }
  }
  
  /** Called when the Interactions pane is reset.  Assumes that ReadLock is already held. */
  public void clearColoring() { synchronized(_stylesList) { _toClear = true; } }
  
  /** Returns true iff the end of the current interaction is an open comment block
    * @return true iff the end of the current interaction is an open comment block
    */
  public boolean _inBlockComment() {
    acquireReadLock();
    try {
      synchronized(_reduced) {
//        resetReducedModelLocation();
//        ReducedModelState state = stateAtRelLocation(getLength() - _currentLocation);
//        boolean toReturn = (state.equals(ReducedModelStates.INSIDE_BLOCK_COMMENT));
        boolean toReturn = _inBlockComment(getLength());
        return toReturn;
      }
    }
    finally { releaseReadLock(); }
  }
  
  /** Inserts the given exception data into the document with the given style.
    * @param message Message contained in the exception
    * @param styleName name of the style for formatting the exception
    */
  public void appendExceptionResult(String message, String styleName) {
    // Note that there is similar code in InteractionsDocument.  Something should be refactored.
    acquireWriteLock();
    try { _insertText(getLength(), message + "\n", styleName); }
    catch (EditDocumentException ble) { throw new UnexpectedException(ble); }
    finally { releaseWriteLock(); }
  } 
}
