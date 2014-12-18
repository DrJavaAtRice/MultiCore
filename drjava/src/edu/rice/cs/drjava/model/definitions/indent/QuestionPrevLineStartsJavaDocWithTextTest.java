/*BEGIN_COPYRIGHT_BLOCK
 *
 * Copyright (c) 2001-2010, JavaPLT group at Rice University (drjava@rice.edu)
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

package edu.rice.cs.drjava.model.definitions.indent;

import javax.swing.text.BadLocationException;

/**
 * Tests the indention rule which detects whether the immediately previous line
 * starts with a /** followed by non-white space text.
 * @version $Id$
 */
public final class QuestionPrevLineStartsJavaDocWithTextTest extends IndentRulesTestCase {

  /** Tests not having the /** prefix. */
  public void testNoPrefix() throws BadLocationException {
    IndentRuleQuestion rule = new QuestionPrevLineStartsJavaDocWithText(null, null);
    
    _setDocText("\nfoo();\nbar();\n");
    assertTrue("line after text (no java doc)", !rule.testApplyRule(_doc, 8, Indenter.IndentReason.OTHER));
    assertTrue("line after text (no java doc)", !rule.testApplyRule(_doc, 15, Indenter.IndentReason.OTHER));    
  }
  
  /** Tests hitting start of document. */
  public void testStartOfDocument() throws BadLocationException {
    IndentRuleQuestion rule = new QuestionPrevLineStartsJavaDocWithText(null, null);
    
    // Hits docstart
    _setDocText("/** bar\nfoo();");
    assertTrue("first line", !rule.testApplyRule(_doc, 0, Indenter.IndentReason.OTHER));
    assertTrue("second line", rule.testApplyRule(_doc, 10, Indenter.IndentReason.OTHER));
  }
  
  /** Tests prefix on current line. */
  public void testJavaDocOnCurrLine() throws BadLocationException {
    IndentRuleQuestion rule = new QuestionPrevLineStartsJavaDocWithText(null, null);
    
    // Prefix at start of current line
    _setDocText("/** foo();");
    assertTrue("before brace", !rule.testApplyRule(_doc, 0, Indenter.IndentReason.OTHER));
    assertTrue("after brace", !rule.testApplyRule(_doc, 4, Indenter.IndentReason.OTHER));
    
    // Java doc in middle of current line
    _setDocText("bar(); /** foo();");
    assertTrue("before java doc", !rule.testApplyRule(_doc, 0, Indenter.IndentReason.OTHER));
    assertTrue("after java doc", !rule.testApplyRule(_doc, 12, Indenter.IndentReason.OTHER));
  }
  
  /** Tests having prev line start with java doc, with text following */
  public void testStartsWithJavaDocWithText() throws BadLocationException {
    IndentRuleQuestion rule = new QuestionPrevLineStartsJavaDocWithText(null, null);
        
    // Java doc plus text (no space)
    _setDocText("/**bar();\nfoo();\nbar();");
    assertTrue("line of java doc (no space)", !rule.testApplyRule(_doc, 3, Indenter.IndentReason.OTHER));
    assertTrue("line after java doc (no space)", rule.testApplyRule(_doc, 12, Indenter.IndentReason.OTHER));
    assertTrue("two lines after java doc (no space)", !rule.testApplyRule(_doc, 18, Indenter.IndentReason.OTHER));
    
    // Java doc plus text (with space)
    _setDocText("/** \tfoo\n  bar\nzip");
    assertTrue("just before java doc (with space)", !rule.testApplyRule(_doc, 0, Indenter.IndentReason.OTHER));
    assertTrue("just after java doc (with space)", !rule.testApplyRule(_doc, 6, Indenter.IndentReason.OTHER));
    assertTrue("line after java doc (with space)", rule.testApplyRule(_doc, 10, Indenter.IndentReason.OTHER));
    assertTrue("second line after java doc (with space)", !rule.testApplyRule(_doc, 17, Indenter.IndentReason.OTHER));
  }
  
  /** Tests having prev line start with prefix, with no text following */
  public void testStartsWithJavaDocNoText() throws BadLocationException {
    IndentRuleQuestion rule = new QuestionPrevLineStartsJavaDocWithText(null, null);
    
    // Java doc plus no text (no space)
    _setDocText("foo();\n/**\nbar();\nzip");
    assertTrue("line of java doc (no space)", !rule.testApplyRule(_doc, 8, Indenter.IndentReason.OTHER));
    assertTrue("line after java doc (no space)", !rule.testApplyRule(_doc, 13, Indenter.IndentReason.OTHER));
    assertTrue("two lines after java doc (no space)", !rule.testApplyRule(_doc, 19, Indenter.IndentReason.OTHER));
  }

}
