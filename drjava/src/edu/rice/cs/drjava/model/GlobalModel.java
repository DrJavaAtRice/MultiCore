package edu.rice.cs.drjava;

import javax.swing.text.Document;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;

import java.io.*;

import java.util.Stack;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.StringTokenizer;
/**
 * Handles the bulk of DrJava's program logic.
 * The UI components interface with the GlobalModel through its public methods,
 * and GlobalModel responds via the GlobalModelListener interface.
 * This removes the dependency on the UI for the logical flow of the program's
 * features.  With the current implementation, we can finally test the compile
 * functionality of DrJava, along with many other things.
 */
public class GlobalModel {
  
  private DefinitionsEditorKit _editorKit;
  private DefinitionsDocument _definitionsDoc;
  private InteractionsDocument _interactionsDoc;
  private Document _consoleDoc;
  private CompilerError[] _compileErrors;
  private LinkedList _listeners;
  private JavaInterpreter _interpreter;
  /**
   * Constructor.
   */
  public GlobalModel() 
  {
    _editorKit = new DefinitionsEditorKit();
    _definitionsDoc = (DefinitionsDocument)(_editorKit.createDefaultDocument());
    _interactionsDoc = new InteractionsDocument();
    _consoleDoc = new DefaultStyledDocument();
    _compileErrors = new CompilerError[0];
    _listeners = new LinkedList();
    _interpreter = new DynamicJavaAdapter();
  }
  
  /**
   * Add a listener to this global model.
   * @param listener a listener that reacts on events generated by the GlobalModel
   */
  public void addListener(GlobalModelListener listener) {
    _listeners.addLast(listener);
  }
  
  /**
   * Remove a listener from this global model.
   * @param listener a listener that reacts on events generated by the GlobalModel
   */
  public void removeListener(GlobalModelListener listener) {
    _listeners.remove(listener);
  }
  
  public DefinitionsEditorKit getEditorKit() {
    return _editorKit;
  }
  
  public Document getDefinitionsDocument() {
    return _definitionsDoc;
  }
  public Document getInteractionsDocument() {
    return _interactionsDoc;
  }
  public Document getConsoleDocument() {
    return _consoleDoc;
  }
  public CompilerError[] getCompileErrors() {
    return _compileErrors;
  }
  
  
  /**
   * Determines if the document has changed since the last save.
   * @return true if the document has been modified
   */
  public boolean isModifiedSinceSave() {
    return _definitionsDoc.isModifiedSinceSave();
  }
  
  /**
   * Creates a new document in the definitions pane.
   * Checks first to make sure no changes need to be saved.
   * If the user saves the changes, or chooses to disregard any
   * changes, the creation of a new document continues, otherwise
   * it is halted and the document remains the same.
   */
  public void newFile() {
    boolean canCreateNew = canAbandonFile();
    if (canCreateNew) {
      _definitionsDoc = (DefinitionsDocument)_editorKit.createDefaultDocument();
      _definitionsDoc.setFile(null);
      _notifyListeners(new EventNotifier() {
        public void notifyListener(GlobalModelListener l) {
          l.newFileCreated();
        }
      });
    }
  }
  
  public void saveFile(FileSaveSelector com) throws IOException {
    FileSaveSelector realCommand;
    final File file = _definitionsDoc.getFile();
    if (file == null) {
      realCommand = com;
    }
    else {
      realCommand = new FileSaveSelector() {
        public File getFile() throws OperationCanceledException {
          return file;
        }
      };
    }
    saveFileAs(realCommand);
  }
  
  /**
   * Saves a file using the Writer encapsulated in the given WriterCommand.
   * @param com a command containing the Writer and name of
   * the file to save to.
   */
  public void saveFileAs(FileSaveSelector com) throws IOException {
    try {
      final File file = com.getFile();
      FileWriter writer = new FileWriter(file);
      _editorKit.write(writer, _definitionsDoc, 0, _definitionsDoc.getLength());
      writer.close();
      _definitionsDoc.resetModification();
      _definitionsDoc.setFile(file);
      _notifyListeners(new EventNotifier() {
        public void notifyListener(GlobalModelListener l) {
          l.fileSaved(file);
        }
      });
    }
    catch (OperationCanceledException oce) {
      // OK, do nothing as the user wishes.
    }
    catch (BadLocationException docFailed) {
      throw new UnexpectedException(docFailed);
    }
  }
  
  /**
   * Open a new document and read from the Reader encapsulated in the
   * ReaderCommand. Warning! This method does not check whether the
   * user wants to save changes.  Listeners must check themselves before 
   * they call this method.  Why?  Because the testing architecture asks
   * to use different Writers to expedite testing. Furthermore
   * @param com a command containing the Reader and name of
   * the file to open from. 
   */
  public void openFile(FileOpenSelector com) throws IOException {
    boolean canOpen = canAbandonFile();
    if (canOpen) {
      DefinitionsDocument tempDoc = (DefinitionsDocument)
        _editorKit.createDefaultDocument();
      try {
        final File file = com.getFile();
        _editorKit.read(new FileReader(file), tempDoc, 0);
        tempDoc.setFile(file);
        tempDoc.resetModification();
        _definitionsDoc = tempDoc;
        _notifyListeners(new EventNotifier() {
          public void notifyListener(GlobalModelListener l) {
            l.fileOpened(file);
          }
        });
      }
      catch (OperationCanceledException oce) {
        // do nothing
      }
      catch (BadLocationException docFailed) {
        throw new UnexpectedException(docFailed);
      }
    }
  }
  
  /**
   * Starts compiling the source.
   */
  public void startCompile() {
    saveBeforeProceeding(GlobalModelListener.COMPILE_REASON);

    if (isModifiedSinceSave()) {
      // if the file hasn't been saved after we told our
      // listeners to do so, don't proceed with the rest
      // of the compile.
    }
    else {
      File file = _definitionsDoc.getFile();

      try {
        String packageName = _definitionsDoc.getPackageName();
        File sourceRoot = _getSourceRoot(packageName);
        
        _notifyListeners(new EventNotifier() {
          public void notifyListener(GlobalModelListener l) {
            l.compileStarted();
          }
        });

        File[] files = new File[] { file };
        _compileErrors = DrJava.compiler.compile(sourceRoot, files);
        
        _notifyListeners(new EventNotifier() {
          public void notifyListener(GlobalModelListener l) {
            l.compileEnded();
          }
        });

        resetConsole();
        _resetInteractions(packageName, sourceRoot);
      }
      catch (InvalidPackageException e) {
        CompilerError err = new CompilerError(file.getAbsolutePath(),
                                              -1,
                                              -1,
                                              e.getMessage(),
                                              false);
        _compileErrors = new CompilerError[] { err };
      }
    }
  }
  
  /**
   * Make sure the user has a chance to save before quitting.
   */
  public void quit() {
    boolean canQuit = canAbandonFile();
    if (canQuit) {
      System.exit(0);
    }
  }
  
  /**
   * Lets the listeners know that the interactions pane has been cleared.
   */
  public void resetInteractions() {
    try {
      String packageName = _definitionsDoc.getPackageName();
      File sourceRoot = _getSourceRoot(packageName);
      _resetInteractions(packageName, sourceRoot);
    }
    catch (InvalidPackageException e) {
      // Oh well, couldn't get package. Just reset the thing
      // without adding to the classpath.
      _resetInteractions("", null);
    }
  }

 
  /**
   * Lets the listeners know that the console pane has been cleared.
   */
  public void resetConsole() {
    try {
      _consoleDoc.remove(0, _consoleDoc.getLength());
    }
    catch (BadLocationException impossible) {
    }
    _notifyListeners(new EventNotifier() {
      public void notifyListener(GlobalModelListener l) {
        l.consoleReset();
      }
    });
  }

  
  public void saveBeforeProceeding(final GlobalModelListener.SaveReason reason)
  {
    if (isModifiedSinceSave()) {
      _notifyListeners(new EventNotifier() {
        public void notifyListener(GlobalModelListener l) {
          l.saveBeforeProceeding(reason);
        }
      });
    }
  }
  
  /**
   * Asks the listeners if the GlobalModel can abandon the current document.
   * @return true if the current document may be abandoned, false if the
   * current action should be halted in its tracks (e.g., file open when
   * the document has been modified since the last save)
   */
  public boolean canAbandonFile() {
    if (isModifiedSinceSave()) {
      return _pollListeners(new EventPoller() {
        public boolean poll(GlobalModelListener l) {
          return l.canAbandonFile(_definitionsDoc.getFile());
        }
      });
    }
    else {
      return true;
    }
  }

  /**
   * Moves the definitions document to the given line, and returns
   * the character position in the document it's gotten to.
   * @param line Number of the line to go to. If line exceeds the number
   *             of lines in the document, it is interpreted as the last line.
   * @return Index into document of where it moved
   */
  public int gotoLine(int line) {
    _definitionsDoc.gotoLine(line);
    return _definitionsDoc.getCurrentLocation();
  }


  
  public FindReplaceMachine createFindReplaceMachine() {
    try {
      return new FindReplaceMachine(_definitionsDoc,
                                    _definitionsDoc.getCurrentLocation());
    }
    catch (BadLocationException e) {
      throw new UnexpectedException(e);
    }
  }
  
  private void _resetInteractions(String packageName, File sourceRoot) {
    _interactionsDoc.reset();
    _interpreter = new DynamicJavaAdapter();

    if (sourceRoot != null) {
      _interpreter.addClassPath(sourceRoot.getAbsolutePath());
    }

    _interpreter.setPackageScope(packageName);

    _notifyListeners(new EventNotifier() {
      public void notifyListener(GlobalModelListener l) {
        l.interactionsReset();
      }
    });
  }

  
  public void recallPreviousInteractionInHistory(Runnable failed) {
      if (_interactionsDoc.hasHistoryPrevious()) {
        _interactionsDoc.moveHistoryPrevious();
      } 
      else {
        failed.run();
      }
  }

  public void recallNextInteractionInHistory(Runnable failed) {
      if (_interactionsDoc.hasHistoryNext()) {
        _interactionsDoc.moveHistoryNext();
      } 
      else {
        failed.run();
      }
  }
  
  /**
   * Interprets the current given text at the prompt in the interactions
   * pane.
   */
  public void interpretCurrentInteraction() {
    try {
      String text = _interactionsDoc.getCurrentInteraction();
      _interactionsDoc.addToHistory(text);
      String toEval = text.trim();
      // Result of interpretation, or JavaInterpreter.NO_RESULT if none.
      Object result;
      // Do nothing but prompt if there's nothing to evaluate!
      if (toEval.length() == 0) {
        result = JavaInterpreter.NO_RESULT;
      } 
      else {
        if (toEval.startsWith("java ")) {
          toEval = _testClassCall(toEval);
        }
        result = _interpreter.interpret(toEval);
        String resultStr;
        try {
          resultStr = String.valueOf(result);
        } catch (Throwable t) {
          // Very weird. toString() on result must have thrown this exception!
          // Let's act like DynamicJava would have if this exception were thrown
          // and rethrow as RuntimeException
          throw  new RuntimeException(t.toString());
        }
      }
      if (result != JavaInterpreter.NO_RESULT) {
       _interactionsDoc.insertString(_interactionsDoc.getLength(),
                                     "\n" + String.valueOf(result) + "\n", null);
      } 
      else {
        _interactionsDoc.insertString(_interactionsDoc.getLength(), "\n", null);
      }
      _interactionsDoc.prompt();
    } catch (BadLocationException e) {
      throw  new InternalError("getting repl text failed");
    } catch (Throwable e) {
      String message = e.getMessage();
      // Don't let message be null. Java sadly makes getMessage() return
      // null if you construct an exception without a message.
      if (message == null) {
        message = e.toString();
        e.printStackTrace();
      }
      // Hack to prevent long syntax error messages
      try {
        if (message.startsWith("koala.dynamicjava.interpreter.InterpreterException: Encountered")) {
          _interactionsDoc.insertString(_interactionsDoc.getLength(), 
                                        "\nError in evaluation: " + 
                                        "Invalid syntax\n", 
                                        null);
        } 
        else {
          _interactionsDoc.insertString(_interactionsDoc.getLength(),
                                        "\nError in evaluation: " + message + 
                                        "\n", null);
        }
        _interactionsDoc.prompt();
      } catch (BadLocationException willNeverHappen) {}
    }
  }
  
  /**
   * Finds the root directory of the source files.
   * @return The root directory of the source files,
   *         based on the package statement.
   * @throws InvalidPackageException If the package statement is invalid,
   *                                 or if it does not match up with the
   *                                 location of the source file.
   */
  File getSourceRoot() throws InvalidPackageException
  {
    return _getSourceRoot(_definitionsDoc.getPackageName());
  }

  /**
   * Finds the root directory of the source files.
   * @param packageName Package name, already fetched from the document
   * @return The root directory of the source files,
   *         based on the package statement.
   * @throws InvalidPackageException If the package statement is invalid,
   *                                 or if it does not match up with the
   *                                 location of the source file.
   */
  private File _getSourceRoot(String packageName)
    throws InvalidPackageException
  {
    File sourceFile = _definitionsDoc.getFile();
    if (sourceFile == null) {
      throw new InvalidPackageException(-1, "Can not get source root for " +
                                            "unsaved file. Please save.");
    }
                                        
    if (packageName.equals("")) {
      return sourceFile.getParentFile();
    }

    Stack packageStack = new Stack();
    int dotIndex = packageName.indexOf('.');
    int curPartBegins = 0;

    while (dotIndex != -1)
    {
      packageStack.push(packageName.substring(curPartBegins, dotIndex));
      curPartBegins = dotIndex + 1;
      dotIndex = packageName.indexOf('.', dotIndex + 1);
    }

    // Now add the last package component
    packageStack.push(packageName.substring(curPartBegins));

    File parentDir = sourceFile;
    while (!packageStack.empty()) {
      String part = (String) packageStack.pop();
      parentDir = parentDir.getParentFile();

      if (parentDir == null) {
        throw new RuntimeException("parent dir is null?!");
      }

      // Make sure the package piece matches the directory name
      if (! part.equals(parentDir.getName())) {
        String msg = "The source file " + sourceFile.getAbsolutePath() + 
                     " is in the wrong directory or in the wrong package. " +
                     "The directory name " + parentDir.getName() +
                     " does not match the package component " + part + ".";

        throw new InvalidPackageException(-1, msg);
      }
    }

    // OK, now parentDir points to the directory of the first component of the
    // package name. The parent of that is the root.
    parentDir = parentDir.getParentFile();
    if (parentDir == null) {
      throw new RuntimeException("parent dir of first component is null?!");
    }

    return parentDir;
  }


  /**
   *Assumes a trimmed String. Returns a string of the main call that the
   *interpretor can use.
   */
  private String _testClassCall(String s) {
    LinkedList ll = new LinkedList();
    if (s.endsWith(";"))
      s = _deleteSemiColon(s);
    StringTokenizer st = new StringTokenizer(s);
    st.nextToken();             //don't want to get back java
    String argument = st.nextToken();           // must have a second Token
    while (st.hasMoreTokens())
      ll.add(st.nextToken());
    argument = argument + ".main(new String[]{";
    ListIterator li = ll.listIterator(0);
    while (li.hasNext()) {
      argument = argument + "\"" + (String)(li.next()) + "\"";
      if (li.hasNext())
        argument = argument + ",";
    }
    argument = argument + "});";
    return  argument;
  }

  /**
   * put your documentation comment here
   * @param s
   * @return 
   */
  private String _deleteSemiColon(String s) {
    return  s.substring(0, s.length() - 1);
  }

  
  
  /**
   * Allows the GlobalModel to ask its listeners a yes/no question and
   * receive a response.
   * @param the listeners' responses ANDed together, true if they all
   * agree, false if some disagree
   */
  private boolean _pollListeners(EventPoller p) {
    ListIterator i = _listeners.listIterator();
    boolean poll = true;

    while(i.hasNext()) {
      GlobalModelListener cur = (GlobalModelListener) i.next();
      poll = poll && p.poll(cur);
    }
    return poll;
  }
    
  /**
   * Lets the listeners know some event has taken place.
   */
  private void _notifyListeners(EventNotifier n) {
    ListIterator i = _listeners.listIterator();
    
    while(i.hasNext()) {
      GlobalModelListener cur = (GlobalModelListener) i.next();
      n.notifyListener(cur);
    }
  }
  
  /**
   * Class model for notifying listeners of an event.
   */
  private abstract class EventNotifier {
    public abstract void notifyListener(GlobalModelListener l);
  }
  
  /**
   * Class model for asking listeners a yes/no question.
   */
  private abstract class EventPoller {
    public abstract boolean poll(GlobalModelListener l);
  }
}
