/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is a part of DrJava. Current versions of this project are available
 * at http://sourceforge.net/projects/drjava
 *
 * Copyright (C) 2001-2002 JavaPLT group at Rice University (javaplt@rice.edu)
 *
 * DrJava is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DrJava is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * or see http://www.gnu.org/licenses/gpl.html
 *
 * In addition, as a special exception, the JavaPLT group at Rice University
 * (javaplt@rice.edu) gives permission to link the code of DrJava with
 * the classes in the gj.util package, even if they are provided in binary-only
 * form, and distribute linked combinations including the DrJava and the
 * gj.util package. You must obey the GNU General Public License in all
 * respects for all of the code used other than these classes in the gj.util
 * package: Dictionary, HashtableEntry, ValueEnumerator, Enumeration,
 * KeyEnumerator, Vector, Hashtable, Stack, VectorEnumerator.
 *
 * If you modify this file, you may extend this exception to your version of the
 * file, but you are not obligated to do so. If you do not wish to
 * do so, delete this exception statement from your version. (However, the
 * present version of DrJava depends on these classes, so you'd want to
 * remove the dependency first!)
 *
END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.ui;

import java.util.Arrays;
import java.util.Hashtable;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;

import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.drjava.model.junit.*;
import edu.rice.cs.util.UnexpectedException;
import edu.rice.cs.drjava.model.GlobalModel;
import edu.rice.cs.drjava.model.OpenDefinitionsDocument;

/**
 * The panel which displays all the testing errors.
 * In the future, it may also contain a progress bar.
 *
 * @version $Id$
 */
public class JUnitPanel extends JPanel {

  /** Highlight painter for selected list items. */
  private static final DefaultHighlighter.DefaultHighlightPainter
    _listHighlightPainter
      = new DefaultHighlighter.DefaultHighlightPainter(Color.yellow);

  private static final SimpleAttributeSet NORMAL_ATTRIBUTES = _getNormalAttributes();
  private static final SimpleAttributeSet BOLD_ATTRIBUTES = _getBoldAttributes();

  private static final SimpleAttributeSet _getBoldAttributes() {
    SimpleAttributeSet s = new SimpleAttributeSet();
    StyleConstants.setBold(s, true);
    return s;
  }

  private static final SimpleAttributeSet _getNormalAttributes() {
    SimpleAttributeSet s = new SimpleAttributeSet();
    return s;
  }


  /** The total number of errors in the list */
  private int _numErrors;

  private final SingleDisplayModel _model;
  private final MainFrame _frame;
  private final JUnitErrorListPane _errorListPane;

  /**
   * Constructor.
   * @param model SingleDisplayModel in which we are running
   * @param frame MainFrame in which we are displayed
   */
  public JUnitPanel(SingleDisplayModel model, MainFrame frame) {
    _model = model;
    _frame = frame;
    _errorListPane = new JUnitErrorListPane();


    setLayout(new BorderLayout());

    // We make the vertical scrollbar always there.
    // If we don't, when it pops up it cuts away the right edge of the
    // text. Very bad.
    JScrollPane scroller =
      new BorderlessScrollPane(_errorListPane,
                      JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);


    add(scroller, BorderLayout.CENTER);
  }

  /**
   * Returns the ErrorListPane that this panel manages.
   */
  public JUnitErrorListPane getJUnitErrorListPane() {
    return _errorListPane;
  }

  /** Changes the font of the error list. */
  public void setListFont(Font f) {
    StyleConstants.setFontFamily(NORMAL_ATTRIBUTES, f.getFamily());
    StyleConstants.setFontSize(NORMAL_ATTRIBUTES, f.getSize());

    StyleConstants.setFontFamily(BOLD_ATTRIBUTES, f.getFamily());
    StyleConstants.setFontSize(BOLD_ATTRIBUTES, f.getSize());
  }

  /** Called when compilation begins. */
  public void setJUnitInProgress() {
    _errorListPane.setJUnitInProgress();
  }

  /**
   * Reset the errors to the current error information.
   * @param errors the current error information
   */
  public void reset() {
    JUnitErrorModel juem = _model.getActiveDocument().getJUnitErrorModel();

    if (juem != null) {
      _numErrors = juem.getErrors().length;
    } else {
      _numErrors = 0;
    }

    _errorListPane.updateListPane();
    _resetEnabledStatus();
  }


  private void _showAllErrors() {
  }

  /**
   * Reset the enabled status of the "next", "previous", and "show all"
   * buttons in the compiler error panel.
   */
  private void _resetEnabledStatus() {
  }



  /**
   * A pane to show JUnit errors. It acts a bit like a listbox (clicking
   * selects an item) but items can each wrap, etc.
   */
  public class JUnitErrorListPane extends JEditorPane {

    /**
     * Index into _errorListPositions of the currently selected error.
     */
    private int _selectedIndex;

    /**
     * The start position of each error in the list. This position is the place
     * where the error starts in the error list, as opposed to the place where
     * the error exists in the source.
     */
    private Position[] _errorListPositions;

    /**
     * Table mapping Positions in the error list to JUnitErrors.
     */
    private final Hashtable _errorTable;

    /**
     * The DefinitionsPane with the current error highlight.
     * (Initialized to the current pane.)
     */
    private DefinitionsPane _lastDefPane;

    // when we create a highlight we get back a tag we can use to remove it
    private Object _listHighlightTag = null;

    // on mouse click, highlight the error in the list and also in the source
    private MouseAdapter _mouseListener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        JUnitError error = _errorAtPoint(e.getPoint());

        if (error == null) {
          selectNothing();
        }
        else {
          _errorListPane.switchToError(error);
        }
      }
    };

    /**
     * Constructs the ErrorListPane.
     */
    public JUnitErrorListPane() {
      // If we set this pane to be of type text/rtf, it wraps based on words
      // as opposed to based on characters.
      super("text/rtf", "");
      addMouseListener(_mouseListener);

      _selectedIndex = 0;
      _errorListPositions = new Position[0];
      _errorTable = new Hashtable();
      _lastDefPane = _frame.getCurrentDefPane();

      JUnitErrorListPane.this.setFont(new Font("Courier", 0, 20));

      // We set the editor pane disabled so it won't get keyboard focus,
      // which makes it uneditable, and so you can't select text inside it.
      setEnabled(false);
    }

    /**
     * Get the index of the current error in the error array.
     */
    public int getSelectedIndex() { return _selectedIndex; }

    /**
     * Allows the ErrorListPane to remember which DefinitionsPane
     * currently has an error highlight.
     */
    public void setLastDefPane(DefinitionsPane pane) {
      _lastDefPane = pane;
    }

    /**
     * Gets the last DefinitionsPane with an error highlight.
     */
    public DefinitionsPane getLastDefPane() {
      return _lastDefPane;
    }

    /**
     * Returns JUnitError associated with the given visual coordinates.
     * Returns null if none.
     */
    private JUnitError _errorAtPoint(Point p) {
      int modelPos = viewToModel(p);

      if (modelPos == -1)
        return null;

      // Find the first error whose position preceeds this model position
      int errorNum = -1;
      for (int i = 0; i < _errorListPositions.length; i++) {
        if (_errorListPositions[i].getOffset() <= modelPos) {
          errorNum = i;
        }
        else { // we've gone past the correct error; the last value was right
          break;
        }
      }

      if (errorNum >= 0) {
        return (JUnitError) _errorTable.get(_errorListPositions[errorNum]);
      }
      else {
        return null;
      }
    }

    /**
     * Returns the index into _errorListPositions corresponding
     * to the given JUnitError.
     */
    private int _getIndexForError(JUnitError error) {
      if (error == null) {
        throw new IllegalArgumentException("Couldn't find index for null error");
      }

      for (int i = 0; i < _errorListPositions.length; i++) {
        JUnitError e = (JUnitError)
          _errorTable.get(_errorListPositions[i]);

        if (error.equals(e)) {
          return i;
        }
      }

      throw new IllegalArgumentException("Couldn't find index for error " + error);
    }

    /**
     * Update the pane which holds the list of errors for the viewer.
     */
    public void updateListPane() {
      try {
        _errorListPositions = new Position[_numErrors];
        _errorTable.clear();

        if (_numErrors == 0) {
          _updateNoErrors();
        }
        else {
          _updateWithErrors();
        }
      }
      catch (BadLocationException e) {
        throw new UnexpectedException(e);
      }

      // Force UI to redraw
      revalidate();
    }

    /** Puts the error pane into "compilation in progress" state. */
    public void setJUnitInProgress() {
      _errorListPositions = new Position[0];

      DefaultStyledDocument doc = new DefaultStyledDocument();

      try {
        doc.insertString(0,
                         "Testing in progress, please wait ...",
                         NORMAL_ATTRIBUTES);
      }
      catch (BadLocationException ble) {
        throw new UnexpectedException(ble);
      }

      setDocument(doc);

      selectNothing();
    }

    /**
     * Used to show that the last compile was successful.
     */
    private void _updateNoErrors() throws BadLocationException {
      DefaultStyledDocument doc = new DefaultStyledDocument();
      doc.insertString(0,
                       "All tests completed successfully.",
                       NORMAL_ATTRIBUTES);
      setDocument(doc);

      selectNothing();
    }

    /**
     * Used to show that the last compile was unsuccessful.
     */
    private void _updateWithErrors() throws BadLocationException {
      DefaultStyledDocument doc = new DefaultStyledDocument();
      int errorNum = 0;

      // Show errors for each file
      OpenDefinitionsDocument openDoc = _model.getActiveDocument();
      JUnitErrorModel errorModel = openDoc.getJUnitErrorModel();
      JUnitError[] errors = errorModel.getErrors();

      if (errors.length > 0) {

        // Grab filename for this set of errors
        String filename = "(Untitled)";
        try {
          File file = openDoc.getFile();
          filename = file.getAbsolutePath();
        }
        catch (IllegalStateException ise) {
          // Not possible: compiled documents must have files
          throw new UnexpectedException(ise);
        }

        // Show errors
        for (int j = 0; j < errors.length; j++, errorNum++) {
          int startPos = doc.getLength();

          // Show file
          doc.insertString(doc.getLength(), "File: ", BOLD_ATTRIBUTES);
          doc.insertString(doc.getLength(), filename + "\n", NORMAL_ATTRIBUTES);

          // Show error
          _insertErrorText(errors, j, doc);
          doc.insertString(doc.getLength(), "\n", NORMAL_ATTRIBUTES);
          Position pos = doc.createPosition(startPos);
          _errorListPositions[errorNum] = pos;
          _errorTable.put(pos, errors[j]);
        }
      }

      setDocument(doc);

      // Select the first error
      _errorListPane.switchToError(0);
    }

    /**
     * Puts an error message into the array of errors at the specified index.
     * @param array the array of errors
     * @param i the index at which the message will be inserted
     * @param doc the document in the error pane
     */
    private void _insertErrorText(JUnitError[] array, int i, Document doc)
      throws BadLocationException
      {
        JUnitError error = array[i];

        doc.insertString(doc.getLength(), "Test: ", BOLD_ATTRIBUTES);
        doc.insertString(doc.getLength(), error.testName(), NORMAL_ATTRIBUTES);
        doc.insertString(doc.getLength(), "\n", NORMAL_ATTRIBUTES);

        if (error.isWarning()) {
          doc.insertString(doc.getLength(), "Warning: ", BOLD_ATTRIBUTES);
        }
        else {
          doc.insertString(doc.getLength(), "Error: ", BOLD_ATTRIBUTES);
        }

        doc.insertString(doc.getLength(), error.message(), NORMAL_ATTRIBUTES);
      }

    /**
     * When the selection of the current error changes, remove
     * the highlight in the error pane.
     */
    private void _removeListHighlight() {
      if (_listHighlightTag != null) {
        getHighlighter().removeHighlight(_listHighlightTag);
        _listHighlightTag = null;
      }
    }

    /**
     * Don't select any errors in the error pane.
     */
    public void selectNothing() {
      _selectedIndex = -1;
      _removeListHighlight();
      _resetEnabledStatus();

      // Remove highlight from the defPane that has it
      _lastDefPane.removeErrorHighlight();
    }

    /**
     * Selects the given error inside the error list pane.
     */
    public void selectItem(JUnitError error) {
      // Find corresponding index
      int i = _getIndexForError(error);

      _selectedIndex = i;
      _removeListHighlight();

      int startPos = _errorListPositions[i].getOffset();

      // end pos is either the end of the document (if this is the last error)
      // or the char where the next error starts
      int endPos;
      if (i + 1 >= (_numErrors)) {
        endPos = getDocument().getLength();
      }
      else {
        endPos = _errorListPositions[i + 1].getOffset();
      }

      try {
        _listHighlightTag =
          getHighlighter().addHighlight(startPos,
                                        endPos,
                                        _listHighlightPainter);

        // Scroll to make sure this item is visible
        Rectangle startRect = modelToView(startPos);
        Rectangle endRect = modelToView(endPos - 1);

        // Add the end rect onto the start rect to make a rectangle
        // that encompasses the entire error
        startRect.add(endRect);

        //System.err.println("scrll vis: " + startRect);

        scrollRectToVisible(startRect);

      }
      catch (BadLocationException badBadLocation) {}

      _resetEnabledStatus();
    }

    /**
     * Change all state to select a new error, including moving the
     * caret to the error, if a corresponding position exists.
     * @param doc OpenDefinitionsDocument containing this error
     * @param errorNum Error number, which is either in _errorsWithoutPositions
     * (if errorNum < _errorsWithoutPositions.length) or in _errors (otherwise).
     * If it's in _errors, we need to subtract _errorsWithoutPositions.length
     * to get the index into the array.
     */
    void switchToError(JUnitError error) {
      if (error == null) return;

      // check and see if this error is without source info, and
      // if so don't try to highlight source info!
      boolean errorHasLocation = (error.lineNumber() > -1);

      if (errorHasLocation) {
        try {
          OpenDefinitionsDocument doc = _model.getDocumentForFile(error.file());
          JUnitErrorModel errorModel = doc.getJUnitErrorModel();
          JUnitError[] errors = errorModel.getErrors();

          int index = Arrays.binarySearch(errors, error);
          if (index >= 0) {
            _gotoErrorSourceLocation(doc, index);
          }
        }
        catch (IOException ioe) {
          // Don't highlight the source if file can't be opened
        }
      }
      else {
        // Remove last highlight
        _lastDefPane.removeErrorHighlight();
      }

      // Select item wants the error, which is what we were passed
      _errorListPane.selectItem(error);
    }

    /**
     * Another interface to switchToError.
     * @param index Index into the array of positions in the ErrorListPane
     */
    void switchToError(int index) {
      if ((index >= 0) && (index < _errorListPositions.length)) {
        Position pos = _errorListPositions[index];
        JUnitError error = (JUnitError) _errorTable.get(pos);
        switchToError(error);
      }
    }

    /**
     * Jumps to error location in source
     * @param doc OpenDefinitionsDocument containing the error
     * @param idx Index into _errors array
     */
    private void _gotoErrorSourceLocation(OpenDefinitionsDocument doc,
                                          final int idx) {
      JUnitErrorModel errorModel = doc.getJUnitErrorModel();
      Position[] positions = errorModel.getPositions();

      if ((idx < 0) || (idx >= positions.length)) return;

      int errPos = positions[idx].getOffset();

      // switch to correct def pane
      _model.setActiveDocument(doc);

      // move caret to that position
      DefinitionsPane defPane = _frame.getCurrentDefPane();
      defPane.setCaretPosition(errPos);
      defPane.grabFocus();
    }

  }

}