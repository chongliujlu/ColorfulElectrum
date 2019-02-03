/* Alloy Analyzer 4 -- Copyright (c) 2006-2009, Felix Chang
 * Electrum -- Copyright (c) 2015-present, Nuno Macedo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.mit.csail.sdg.alloy4;

import static edu.mit.csail.sdg.alloy4.OurConsole.style;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

/** Graphical syntax-highlighting StyledDocument.
 *
 * <p><b>Thread Safety:</b> Can be called only by the AWT event thread
 *
 * @modified: Nuno Macedo, Eduardo Pessoa // [HASLab] electrum-temporal
 */

class OurSyntaxDocument extends DefaultStyledDocument {

	/** This ensures the class can be serialized reliably. */
	private static final long serialVersionUID = 0;

	/** The style mode at the start of each line.
	 * First field is "comment mode" (0 = no comment) (1 = block comment) (2 = javadoc comment) (-1 = unknown).
	 * Remainder are colorful features (0 = not marked) (1 = positive mark) (2 = negative mark) (-1 = unknown). */
	// [HASLab] colorful electrum, adapted to also consider features
	private final List<List<Integer>> comments = new ArrayList<List<Integer>>();

	/** Whether syntax highlighting is currently enabled or not. */
	private boolean enabled = true;

	/** The current font name is. */
	private String font = "Monospaced";

	/** The current font size. */
	private int fontSize = 14;

	/** The current tab size. */
	private int tabSize = 4;

	/** The list of font+color styles (eg. regular text, symbols, keywords, comments, etc). */
	private final List<MutableAttributeSet> all = new ArrayList<MutableAttributeSet>();

	/** The character style for regular text. */
	private final MutableAttributeSet styleNormal = style(font, fontSize, false, Color.BLACK, 0);
	private final MutableAttributeSet styleNormal(List<Integer> n) { return style(font, fontSize, false, Color.BLACK, getPos(n), getNeg(n), 0); } // [HASLab] colorful electrum
	
	/** The character style for symbols. */
	private final MutableAttributeSet styleSymbol  = style(font, fontSize, true, Color.BLACK, 0);           { all.add(styleSymbol); }
	private final MutableAttributeSet styleSymbol(List<Integer> n) { return style(font, fontSize, true, Color.BLACK, getPos(n), getNeg(n), 0);          } // [HASLab] colorful electrum

	/** The character style for integer constants. */
	private final MutableAttributeSet styleNumber  = style(font, fontSize, true, new Color(0xA80A0A), 0);   { all.add(styleNumber); }
	private final MutableAttributeSet styleNumber(List<Integer> n)  {return style(font, fontSize, true, new Color(0xA80A0A), getPos(n), getNeg(n), 0); } // [HASLab] colorful electrum

	/** The character style for keywords. */
	private final MutableAttributeSet styleKeyword = style(font, fontSize, true, new Color(0x1E1EA8), 0);   { all.add(styleKeyword); }
	private final MutableAttributeSet styleKeyword(List<Integer> n) { return style(font, fontSize, true, new Color(0x1E1EA8), getPos(n), getNeg(n), 0);} // [HASLab] colorful electrum

	/** The character style for string literals. */
	private final MutableAttributeSet styleString  = style(font, fontSize, false, new Color(0xA80AA8), 0);  { all.add(styleString); }
	private final MutableAttributeSet styleString(List<Integer> n) { return style(font, fontSize, false, new Color(0xA80AA8), getPos(n), getNeg(n), 0); } // [HASLab] colorful electrum

	/** The character style for featured text. */
	// [HASLab] colorful electrum
	private final MutableAttributeSet styleColor(List<Integer> n, Color c) { return style(font, fontSize, true, new Color(c.getRed()-41,c.getGreen()-41,c.getBlue()-41), getPos(n), getNeg(n), 0); }

	/** The character style for up-to-end-of-line-style comment. */
	private final MutableAttributeSet styleComment = style(font, fontSize, false, new Color(0x0A940A), 0);  { all.add(styleComment); }

	/** The character style for non-javadoc-style block comment. */
	private final MutableAttributeSet styleBlock   = style(font, fontSize, false, new Color(0x0A940A), 0);  { all.add(styleBlock); }

	/** The character style for javadoc-style block comment. */
	private final MutableAttributeSet styleJavadoc = style(font, fontSize, true, new Color(0x0A940A), 0);   { all.add(styleJavadoc); }

	/** The paragraph style for indentation. */
	private final MutableAttributeSet tabset = new SimpleAttributeSet();

	/** The colors of each of the features. */
	// [HASLab] colorful electrum
	static Color C[] =  {new Color(255,225,205),new Color(255,205,225),new Color(205,255,225),new Color(225,255,205),new Color(205,225,255),new Color(225,205,255)};
	
	/** Convert the list of positive features (1) into a list of colors. */
	// [HASLab] colorful electrum
	private static Set<Color> getPos(List<Integer> n) {
		Set<Color> res = new HashSet<Color>();
		for (int i = 1; i <= 6; i++)
			if (n.get(i) == 1) res.add(C[i-1]);
		return res;
	}
	
	/** Convert the list of negative features (2) into a list of colors. */
	// [HASLab] colorful electrum
	private static Set<Color> getNeg(List<Integer> n) {
		Set<Color> res = new HashSet<Color>();
		for (int i = 1; i <= 6; i++)
			if (n.get(i) == 2) res.add(C[i-1]);
		return res;	}
	
	/** This stores the currently recognized set of reserved keywords. */
	private static final String[] keywords = new String[] {"abstract", "var", "all", "and", "as", "assert", "but", "check", "disj",
		"disjoint", "else", "enum", "exactly", "exh", "exhaustive", "expect", "extends", "fact", "for", "fun", "iden",
		"iff", "implies", "in", "Int", "int", "let", "lone", "module", "no", "none", "not", "one", "open", "or", "part",
		"partition", "pred", "private", "run", "seq", "set", "sig", "some", "String", "sum", "this", "univ", 
		"eventually", "always", "after", "once", "historically", "since", "trigger", "previous", "until", "release", "Time" // [HASLab] temporal keywords
	};

	/** Returns true if array[start .. start+len-1] matches one of the reserved keyword. */
	private static final boolean do_keyword(String array, int start, int len) {
		if (len >= 2 && len <= 12) for(int i = keywords.length - 1; i >= 0; i--) { // [HASLab] historically is larger
			String str = keywords[i];
			if (str.length()==len) for(int j=0; ;j++) if (j==len) return true; else if (str.charAt(j) != array.charAt(start+j)) break;
		}
		return false;
	}

	/** Returns true if "c" can be in the start or middle or end of an identifier. */
	private static final boolean do_iden(char c) {
		return (c>='A' && c<='Z') || (c>='a' && c<='z') || c=='$' || (c>='0' && c<='9') || c=='_' /*|| c=='\''*/ || c=='\"'; // [HASLab] primed expressions
	}
	
	/** The first positive/negative color feature delimiters. */
	// [HASLab] colorful electrum
	public static char O1 = '\u2780', E1 = '\u278A';
	
	/** Whether a positive color feature delimiter. */
	// [HASLab] colorful electrum
	private static final boolean isPositiveColor(char c) {
		return (c>=O1 && c<=(char)(O1+5));
	}

	/** Whether a negative color feature delimiter. */
	// [HASLab] colorful electrum
	private static final boolean isNegativeColor(char c) {
		return (c>=E1 && c<=(char)(E1+5));
	}
	
	/** Constructor. */
	public OurSyntaxDocument(String fontName, int fontSize) {
		putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
		tabSize++;
		do_setFont(fontName, fontSize, tabSize - 1); // assigns the given font, and also forces recomputation of the tab size
	}

	/** Enables or disables syntax highlighting. */
	public final void do_enableSyntax (boolean flag) {
		if (enabled == flag) return; else { enabled = flag;  comments.clear(); }
		if (flag) do_reapplyAll(); else setCharacterAttributes(0, getLength(), styleNormal, false);
	}

	/** Return the number of lines represented by the current text (where partial line counts as a line).
	 * <p> For example: count("")==1, count("x")==1, count("x\n")==2, and count("x\ny")==2
	 */
	public final int do_getLineCount() {
		String txt = toString();
		for(int n=txt.length(), ans=1, i=0; ; i++) if (i>=n) return ans; else if (txt.charAt(i)=='\n') ans++;
	}

	/** Return the starting offset of the given line (If "line" argument is too large, it will return the last line's starting offset)
	 * <p> For example: given "ab\ncd\n", start(0)==0, start(1)==3, start(2...)==6.  Same thing when given "ab\ncd\ne".
	 */
	public final int do_getLineStartOffset(int line) {
		String txt = toString();
		for(int n=txt.length(), ans=0, i=0, y=0; ; i++) if (i>=n || y>=line) return ans; else if (txt.charAt(i)=='\n') {ans=i+1; y++;}
	}

	/** Return the line number that the offset is in (If "offset" argument is too large, it will just return do_getLineCount()-1).
	 * <p> For example: given "ab\ncd\n", offset(0..2)==0, offset(3..5)==1, offset(6..)==2.  Same thing when given "ab\ncd\ne".
	 */
	public final int do_getLineOfOffset(int offset) {
		String txt = toString();
		for(int n=txt.length(), ans=0, i=0; ; i++) if (i>=n || i>=offset) return ans; else if (txt.charAt(i)=='\n') ans++;
	}

	/** This method is called by Swing to insert a String into this document.
	 * We intentionally ignore "attr" and instead use our own coloring.
	 */
	@Override public void insertString(int offset, String string, AttributeSet attr) throws BadLocationException {
		if (string.indexOf('\r')>=0) string = Util.convertLineBreak(string); // we don't want '\r'
		if (!enabled) { super.insertString(offset, string, styleNormal); return; }
		int startLine = do_getLineOfOffset(offset);
		// [HASLab] color modes
		for(int i = 0; i < string.length(); i++) { // For each inserted '\n' we need to shift the values in "comments" array down
			if (string.charAt(i)=='\n') { if (startLine < comments.size()-1) comments.add(startLine+1, Arrays.asList(-1,-1,-1,-1,-1,-1,-1)); }
		}
		super.insertString(offset, string, styleNormal);
		try { do_update(startLine); } catch(Exception ex) { comments.clear(); }
	}

	/** This method is called by Swing to delete text from this document. */
	@Override public void remove(int offset, int length) throws BadLocationException {
		if (!enabled) { super.remove(offset, length); return; }
		int i = 0, startLine = do_getLineOfOffset(offset);
		for(String oldText = toString(); i<length; i++) { // For each deleted '\n' we need to shift the values in "comments" array up
			if (oldText.charAt(offset+i)=='\n') { if (startLine < comments.size()-1) comments.remove(startLine+1); }
		}
		super.remove(offset, length);
		try { do_update(startLine); } catch(Exception ex) { comments.clear(); }
	}

	/** This method is called by Swing to replace text in this document. */
	@Override public void replace(int offset, int length, String string, AttributeSet attrs) throws BadLocationException {
		if (length > 0) this.remove(offset, length);
		if (string != null && string.length() > 0) this.insertString(offset, string, styleNormal);
	}

	/** Reapply styles assuming the given line has just been modified */
	private final void do_update(int line) throws BadLocationException  {
		String content = toString();
		int lineCount = do_getLineCount();
		// [HASLab] color modes
		while(line>0 && (line>=comments.size() || comments.get(line).get(0)<0)) line--; // "-1" in comments array are always contiguous
		List<Integer> comment = do_reapply(line==0 ? Arrays.asList(0,0,0,0,0,0,0) : new ArrayList<Integer>(comments.get(line)), content, line);
		for (line++; line < lineCount; line++) { // update each subsequent line until it already starts with its expected comment mode
			if (line < comments.size() && comments.get(line).equals(comment)) break; else { comment = do_reapply(comment, content, line);}
		}
	}

	/** Re-color the given line assuming it starts with a given style mode, then return the style mode for start of next line. */
	// [HASLab] colorful electrum, list of color modes rather than single comment mode
	private final List<Integer> do_reapply(List<Integer> comment, final String txt, final int line) {
		// [HASLab] color modes
		while (line >= comments.size()) comments.add(Arrays.asList(-1,-1,-1,-1,-1,-1,-1)); // enlarge array if needed
		comments.set(line, new ArrayList<Integer>(comment));                               // record the fact that this line starts with the given comment mode
		for(int n = txt.length(), i = do_getLineStartOffset(line); i < n;) {
			final int oldi = i;
			final char c = txt.charAt(i);
			if (c=='\n') break;
			// [HASLab] comment mode is at position 0
			if (comment.get(0)==0 && c=='/' && i<n-3 && txt.charAt(i+1)=='*' && txt.charAt(i+2)=='*' && txt.charAt(i+3)!='/') comment.set(0,2);
			if (comment.get(0)==0 && c=='/' && i==n-3 && txt.charAt(i+1)=='*' && txt.charAt(i+2)=='*') comment.set(0,2);
			if (comment.get(0)==0 && c=='/' && i<n-1 && txt.charAt(i+1)=='*') { comment.set(0,1); i = i + 2; }
			if (comment.get(0)>0) {
				AttributeSet style = (comment.get(0)==1 ? styleBlock : styleJavadoc);
				while(i<n && txt.charAt(i)!='\n' && (txt.charAt(i)!='*' || i+1==n || txt.charAt(i+1)!='/')) i = i + 1;
				if (i<n-1 && txt.charAt(i)=='*' && txt.charAt(i+1)=='/') { i = i + 2; comment.set(0,0); }
				setCharacterAttributes(oldi, i-oldi, style, false);
			} else if ((c=='/' || c=='-') && i<n-1 && txt.charAt(i+1)==c) {
				i = txt.indexOf('\n', i);
				setCharacterAttributes(oldi, i<0 ? (n-oldi) : (i-oldi), styleComment, false);
				break;
			} else if (c=='\"') {
				for(i++; i<n; i++) {
					if (txt.charAt(i)=='\n') break;
					if (txt.charAt(i)=='\"') {i++; break;}
					if (txt.charAt(i)=='\\' && i+1<n && txt.charAt(i+1)!='\n') i++;
				}
				setCharacterAttributes(oldi, i-oldi, styleString(comment), false);
			} else if (isNegativeColor(c) || isPositiveColor(c)) { // [HASLab] colorful electrum, check for delimiters and change style mode
				i++;
				boolean opens = true;
				// if already with style, invert
				if (isPositiveColor(c) && comment.get(c-O1+1) != 0) {comment.set(c-O1+1,0);opens=false;}
				else if (isNegativeColor(c) && comment.get(c-E1+1) != 0) {comment.set(c-E1+1,0);opens=false;}
				for (int k = 0; k < 6; k++) // paint the delimiters
					if (c == (char) (O1+k) || c == (char) (E1+k)) setCharacterAttributes(oldi, i-oldi, styleColor(comment,C[k]), false);
				// if not in style, apply
				if (opens && isPositiveColor(c) && comment.get(c-O1+1) == 0) {comment.set(c-O1+1,1);}	
				else if (opens && isNegativeColor(c) && comment.get(c-E1+1) == 0) {comment.set(c-E1+1,2);}
			}
			else if(do_iden(c)) {
				for(i++; i<n && do_iden(txt.charAt(i)); i++) { }
				AttributeSet style = (c>='0' && c<='9') ? styleNumber(comment) : (do_keyword(txt, oldi, i-oldi) ? styleKeyword(comment) : styleNormal(comment));
				setCharacterAttributes(oldi, i-oldi, style, true);
			} else {
				for(i++; i<n && !do_iden(txt.charAt(i)) && txt.charAt(i)!='\n' && txt.charAt(i)!='-' && txt.charAt(i)!='/' && !isPositiveColor(txt.charAt(i)) && !isNegativeColor(txt.charAt(i)); i++) { }
				setCharacterAttributes(oldi, i-oldi, styleSymbol(comment), true);
			}
		}
		return comment;
	}

	/** Reapply the appropriate style to the entire document. */
	private final void do_reapplyAll() {
		setCharacterAttributes(0, getLength(), styleNormal, true);
		comments.clear();
		String content = toString();
		// [HASLab] color modes
		List<Integer> comment = Arrays.asList(0,0,0,0,0,0,0);
		for(int i = 0, n = do_getLineCount(); i < n; i++)  { comment = new ArrayList<Integer>(do_reapply(comment, content, i)); }
	}

	/** Changes the font and tabsize for the document. */
	public final void do_setFont(String fontName, int fontSize, int tabSize) {
		if (tabSize < 1) tabSize = 1; else if (tabSize > 100) tabSize = 100;
		if (fontName.equals(this.font) && fontSize == this.fontSize && tabSize == this.tabSize) return;
		this.font = fontName;
		this.fontSize = fontSize;
		this.tabSize = tabSize;
		for(MutableAttributeSet s: all) { StyleConstants.setFontFamily(s, fontName);  StyleConstants.setFontSize(s, fontSize); }
		do_reapplyAll();
		BufferedImage im = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB); // this is used to derive the tab width
		int gap = tabSize * im.createGraphics().getFontMetrics(new Font(fontName, Font.PLAIN, fontSize)).charWidth('X');
		TabStop[] pos = new TabStop[100];
		for(int i=0; i<100; i++) { pos[i] = new TabStop(i*gap + gap); }
		StyleConstants.setTabSet(tabset, new TabSet(pos));
		setParagraphAttributes(0, getLength(), tabset, false);
	}

	/** Overriden to return the full text of the document.
	 * @return the entire text
	 */
	@Override public String toString() {
		try { return getText(0, getLength()); } catch(BadLocationException ex) { return ""; }
	}
}
