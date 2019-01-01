/*******************************************************************************
 * Copyright (c) 2013, 2015 EclipseSource.
 * Copyright (c) 2015-2017 Christian Zangl
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.hjson;

import java.io.*;

class HjsonParser {

  private static final int MIN_BUFFER_SIZE=10;
  private static final int DEFAULT_BUFFER_SIZE=1024;

  private final String buffer;
  private Reader reader;
  private int index;
  private int line;
  private int lineOffset;
  private int current;
  private StringBuilder captureBuffer, peek;
  private boolean capture;
  private boolean legacyRoot;

  private IHjsonDsfProvider[] dsfProviders;

  HjsonParser(String string, HjsonOptions options) {
    buffer=string;
    reset();
    if (options!=null) {
      dsfProviders=options.getDsfProviders();
      legacyRoot=options.getParseLegacyRoot();
    } else {
      dsfProviders=new IHjsonDsfProvider[0];
      legacyRoot=true;
    }
  }

  HjsonParser(Reader reader, HjsonOptions options) throws IOException {
    this(readToEnd(reader), options);
  }

  static String readToEnd(Reader reader) throws IOException {
    // read everything into a buffer
    int n;
    char[] part=new char[8*1024];
    StringBuilder sb=new StringBuilder();
    while ((n=reader.read(part, 0, part.length))!=-1) sb.append(part, 0, n);
    return sb.toString();
  }

  void reset() {
    index=lineOffset=current=0;
    line=1;
    peek=new StringBuilder();
    // Don't lose the final character.
    reader=new StringReader(buffer + ' ');
    capture=false;
    captureBuffer=null;
  }

  JsonValue parse() throws IOException {
    read();
    String header=readBetweenVals();
    JsonValue value = tryParse();
    value.setFullComment(CommentType.BOL, header);

    return value;
  }

  JsonValue tryParse() throws IOException {
    // Braces for the root object are optional
    if (legacyRoot) {
      switch (current) {
        case '[':
        case '{':
          return checkTrailing(readValue());
        default:
          try {
            // assume we have a root object without braces
            return checkTrailing(readObject(true));
          } catch (Exception exception) {
            // test if we are dealing with a single JSON value instead (true/false/null/num/"")
            reset();
            read();
            System.out.println("101: " + readBetweenVals());
            try { return checkTrailing(readValue()); }
            catch (Exception exception2) { }
            // throw original error
            throw exception;
          }
      }
    } else {
      return checkTrailing(readValue());
    }
  }

  JsonValue checkTrailing(JsonValue v) throws ParseException, IOException {
    v.setFullComment(CommentType.EOL, readBetweenVals());
    if (!isEndOfText()) throw error("Extra characters in input: "+current);
    return v;
  }

  private JsonValue readValue() throws IOException {
    switch(current) {
      case '\'':
      case '"': return readString();
      case '[': return readArray();
      case '{': return readObject(false);
      default: return readTfnns();
    }
  }

  private JsonValue readTfnns() throws IOException {
    // Hjson strings can be quoteless
    // returns string, true, false, or null.
    StringBuilder value=new StringBuilder();
    int first=current;
    if (JsonValue.isPunctuatorChar(first))
      throw error("Found a punctuator character '" + (char)first + "' when expecting a quoteless string (check your syntax)");
    value.append((char)current);
    for (;;) {
      read();
      boolean isEol=current<0 || current=='\r' || current=='\n';
      if (isEol || current==',' ||
        current=='}' || current==']' ||
        current=='#' ||
        current=='/' && (peek()=='/' || peek()=='*')
        ) {
        switch (first) {
          case 'f':
          case 'n':
          case 't':
            String svalue=value.toString().trim();
            if (svalue.equals("false")) return JsonValue.FALSE;
            else if (svalue.equals("null")) return JsonValue.NULL;
            else if (svalue.equals("true")) return JsonValue.TRUE;
            break;
          default:
            if (first=='-' || first>='0' && first<='9') {
              JsonValue n=tryParseNumber(value, false);
              if (n!=null) return n;
            }
        }
        if (isEol) {
          // remove any whitespace at the end (ignored in quoteless strings)
          return HjsonDsf.parse(dsfProviders, value.toString().trim());
        }
      }
      value.append((char)current);
    }
  }

  private JsonArray readArray() throws IOException {
    read();
    JsonArray array=new JsonArray();
    boolean compact=isContainerCompact();
    array.setCompact(compact);
    int sumLineLength=0;
    int lineLength=1;
    int numLines=0;

    while (true) {
      // Comment above / before value.
      String bol=readBetweenVals();

      // If readBetweenVals() brought us to the
      // end, it must be time to stop.
      if (readIf(']')) {
        // Because we reached the end, we learned
        // that this was an interior comment.
        array.setFullComment(CommentType.INTERIOR, bol);
        break;
      } else if (isEndOfText()) {
        throw error("End of input while parsing an array (did you forget a closing ']'?)");
      }
      // Value comes next.
      JsonValue value=readValue();
      value.setFullComment(CommentType.BOL, bol);

      // Skip whitespace surrounding a comma.
      skipToNL();
      if (readIf(',')) { // , is optional
        skipToNL();
      }
      // Make sure we back up all the way to a
      // new line character or non-whitespace.
      readIf('\r');

      // If we hit a new line, whatever character
      // comes next is either a value or BOL #.
      if (current!='\n') {
        // There was something else on this line.
        // See if it was an EOL #.
        String eol = readBetweenVals(true);

        if (!eol.isEmpty()) { // This is an EOL #.
          value.setFullComment(CommentType.EOL, eol);
        } else { // There's another value on this line.
          lineLength++;
        }
      } else {
        sumLineLength+=lineLength;
        lineLength=1;
        // Tally the lines.
        numLines++;
      }
      array.add(value);
    }
    if (sumLineLength>0) {
      int avgLineLength=sumLineLength/numLines;
      if (avgLineLength<1) avgLineLength=1;
      array.setLineLength(avgLineLength);
    } else if (compact) {
      array.setLineLength(array.size());
    }
    return array;
  }

  private JsonObject readObject(boolean objectWithoutBraces) throws IOException {
    // Skip the opening brace.
    if (!objectWithoutBraces) read();
    JsonObject object=new JsonObject();
    object.setCompact(isContainerCompact());
    int sumLineLength=1;
    int lineLength=1;
    int numLines=0;

    while (true) {
      // Comment above / before name.
      String bol=readBetweenVals();

      // If readBetweenVals() brought us to the
      // end, it must be time to stop.
      if (objectWithoutBraces) {
        if (isEndOfText()) {
          // Because we reached the end, we learned
          // that this was an interior comment.
          object.setFullComment(CommentType.INTERIOR, bol);
          break;
        }
      } else {
        if (isEndOfText()) throw error("End of input while parsing an object (did you forget a closing '}'?)");
        if (readIf('}')) {
          object.setFullComment(CommentType.INTERIOR, bol);
          break;
        }
      }
      // Name comes next.
      String name=readName();

      // Colon and potential surrounding spaces.
      // Comments will be fully ignored, here.
      readBetweenVals();
      if (!readIf(':')) {
        throw expected("':'");
      }
      readBetweenVals();

      // The value itself.
      JsonValue value = readValue();

      // Skip whitespace surrounding a comma.
      skipToNL();
      if (readIf(',')) { // , is optional
        skipToNL();
      }
      // Make sure we back up all the way to a
      // new line character or non-whitespace.
      readIf('\r');

      // If we hit a new line, whatever character
      // comes next is either a key or BOL #.
      if (current!='\n') {
        // There was something else on this line.
        // See if it was an EOL #.
        String eol = readBetweenVals(true);

        if (!eol.isEmpty()) { // This is an EOL #.
          value.setFullComment(CommentType.EOL, eol);
        } else { // There's another value on this line.
          lineLength++;
        }
      } else {
        sumLineLength+=lineLength;
        lineLength=1;
        // Tally the lines.
        numLines++;
      }
      // Set comments and add.
      value.setFullComment(CommentType.BOL, bol);
      object.add(name, value);
    }
    if (sumLineLength>0) {
      int avgLineLength=sumLineLength/numLines;
      if (avgLineLength<1) avgLineLength=1;
      object.setLineLength(avgLineLength);
    }
    return object;
  }

  private boolean isContainerCompact() throws IOException {
    skipToNL();
    readIf('\r');
    // The object is compact if there is non-whitespace
    // on the same line. If any further values are placed
    // on subsequent lines, they will most likely look
    // better this way, anyway.
    return current!='\n';
  }

  private String readName() throws IOException {
    if (current=='"' || current=='\'') return readStringInternal(false);

    StringBuilder name=new StringBuilder();
    int space=-1, start=index;
    while (true) {
      if (current==':') {
        if (name.length()==0) throw error("Found ':' but no key name (for an empty key name use quotes)");
        else if (space>=0 && space!=name.length()) { index=start+space; throw error("Found whitespace in your key name (use quotes to include)"); }
        return name.toString();
      } else if (isWhiteSpace(current)) {
        if (space<0) space=name.length();
      } else if (current<' ') {
        throw error("Name is not closed");
      } else if (JsonValue.isPunctuatorChar(current)) {
        throw error("Found '" + (char)current + "' where a key name was expected (check your syntax or use quotes if the key name includes {}[],: or whitespace)");
      } else name.append((char)current);
      read();
    }
  }

  private String readMlString() throws IOException {

    // Parse a multiline string value.
    StringBuilder sb=new StringBuilder();
    int triple=0;

    // we are at '''
    int indent=index-lineOffset-4;

    // skip white/to (newline)
    for (; ; ) {
      if (isWhiteSpace(current) && current!='\n') read();
      else break;
    }
    if (current=='\n') { read(); skipIndent(indent); }

    // When parsing for string values, we must look for " and \ characters.
    while (true) {
      if (current<0) throw error("Bad multiline string");
      else if (current=='\'') {
        triple++;
        read();
        if (triple==3) {
          if (sb.charAt(sb.length()-1)=='\n') sb.deleteCharAt(sb.length()-1);

          return sb.toString();
        }
        else continue;
      }
      else {
        while (triple>0) {
          sb.append('\'');
          triple--;
        }
      }
      if (current=='\n') {
        sb.append('\n');
        read();
        skipIndent(indent);
      }
      else {
        if (current!='\r') sb.append((char)current);
        read();
      }
    }
  }

  private void skipIndent(int indent) throws IOException {
    while (indent-->0) {
      if (isWhiteSpace(current) && current!='\n') read();
      else break;
    }
  }

  private JsonValue readString() throws IOException {
    return new JsonString(readStringInternal(true));
  }

  private String readStringInternal(boolean allowML) throws IOException {
    // callers make sure that (current=='"' || current=='\'')
    int exitCh = current;
    read();
    startCapture();
    while (current!=exitCh) {
      if (current=='\\') readEscape();
      else if (current<0x20) throw expected("valid string character");
      else read();
    }
    String string=endCapture();
    read();

    if (allowML && exitCh=='\'' && current=='\'' && string.length()==0) {
      // ''' indicates a multiline string
      read();
      return readMlString();
    } else return string;
  }

  private void readEscape() throws IOException {
    pauseCapture();
    read();
    switch(current) {
      case '"':
      case '\'':
      case '/':
      case '\\':
        captureBuffer.append((char)current);
        break;
      case 'b':
        captureBuffer.append('\b');
        break;
      case 'f':
        captureBuffer.append('\f');
        break;
      case 'n':
        captureBuffer.append('\n');
        break;
      case 'r':
        captureBuffer.append('\r');
        break;
      case 't':
        captureBuffer.append('\t');
        break;
      case 'u':
        char[] hexChars=new char[4];
        for (int i=0; i<4; i++) {
          read();
          if (!isHexDigit()) {
            throw expected("hexadecimal digit");
          }
          hexChars[i]=(char)current;
        }
        captureBuffer.append((char)Integer.parseInt(new String(hexChars), 16));
        break;
      default:
        throw expected("valid escape sequence");
    }
//    capture=CaptureState.CAPTURE;
    capture=true;
    read();
  }

  private static boolean isDigit(char ch) {
    return ch>='0' && ch<='9';
  }

  static JsonValue tryParseNumber(StringBuilder value, boolean stopAtNext) throws IOException {
    int idx=0, len=value.length();
    if (idx<len && value.charAt(idx)=='-') idx++;

    if (idx>=len) return null;
    char first=value.charAt(idx++);
    if (!isDigit(first)) return null;

    if (first=='0' && idx<len && isDigit(value.charAt(idx)))
      return null; // leading zero is not allowed

    while (idx<len && isDigit(value.charAt(idx))) idx++;

    // frac
    if (idx<len && value.charAt(idx)=='.') {
      idx++;
      if (idx>=len || !isDigit(value.charAt(idx++))) return null;
      while (idx<len && isDigit(value.charAt(idx))) idx++;
    }

    // exp
    if (idx<len && Character.toLowerCase(value.charAt(idx))=='e') {
      idx++;
      if (idx<len && (value.charAt(idx)=='+' || value.charAt(idx)=='-')) idx++;

      if (idx>=len || !isDigit(value.charAt(idx++))) return null;
      while (idx<len && isDigit(value.charAt(idx))) idx++;
    }

    int last=idx;
    while (idx<len && isWhiteSpace(value.charAt(idx))) idx++;

    boolean foundStop=false;
    if (idx<len && stopAtNext) {
      // end scan if we find a control character like ,}] or a comment
      char ch=value.charAt(idx);
      if (ch==',' || ch=='}' || ch==']' || ch=='#' || ch=='/' && (len>idx+1 && (value.charAt(idx+1)=='/' || value.charAt(idx+1)=='*')))
        foundStop=true;
    }

    if (idx<len && !foundStop) return null;

    return new JsonNumber(Double.parseDouble(value.substring(0, last)));
  }

  static JsonValue tryParseNumber(String value, boolean stopAtNext) throws IOException {
    return tryParseNumber(new StringBuilder(value), stopAtNext);
  }

  private boolean readIf(char ch) throws IOException {
    if (current!=ch) {
      return false;
    }
    read();
    return true;
  }

  private String readBetweenVals() throws IOException {
    return readBetweenVals(false);
  }

  private String readBetweenVals(boolean toEOL) throws IOException {
    int indent=0;
    startCapture();
    while (!isEndOfText()) {
      pauseCapture();
      indent=skipWhiteSpace();
      startCapture();
      if (current=='#' || current=='/' && peek()=='/') {
        do {
          read();
        } while (current>=0 && current!='\n');
        if (toEOL) break;
        else read();
      }
      else if (current=='/' && peek()=='*') {
        read();
        do {
          read();
          if (current=='\n') {
            read();
            skip(indent-1);
          }
        } while (current>=0 && !(current=='*' && peek()=='/'));
        read(); read();
        // Don't cut these values apart.
        while (current=='\r' || current=='\n') {
          read();
        }
      }
      else break;
    }
    // trim() should be removed.
    return endCapture().trim();
  }

  private int skipWhiteSpace() throws IOException {
    int numSkipped=0;
    while (isWhiteSpace()) {
      read();
      numSkipped++;
    }
    return numSkipped;
  }

  private void skipToNL() throws IOException {
    while (current==' ' || current=='\t') read();
  }

  private int peek(int idx) throws IOException {
    while (idx>=peek.length()) {
      int c=reader.read();
      if (c<0) return c;
      peek.append((char)c);
    }
    return peek.charAt(idx);
  }

  private int peek() throws IOException {
    return peek(0);
  }

  private boolean read() throws IOException {
    if (current=='\n') {
      line++;
      lineOffset=index;
    }

    if (peek.length()>0)
    {
      // normally peek will only hold not more than one character so this should not matter for performance
      current=peek.charAt(0);
      peek.deleteCharAt(0);
    }
    else current=reader.read();

    if (current<0) return false;

    index++;

    if (capture) captureBuffer.append((char) current);

    return true;
  }

  private void skip(int num) throws IOException {
    pauseCapture();
    for (int i=0; i<num; i++) read();
    startCapture();
  }

  private void startCapture() {
    if (captureBuffer==null)
      captureBuffer=new StringBuilder();
    capture=true;
    captureBuffer.append((char)current);
  }

  private void pauseCapture() {
    int len=captureBuffer.length();
    if (len>0) captureBuffer.deleteCharAt(len-1);
    capture=false;
  }

  private String endCapture() {
    pauseCapture();
    String captured;
    if (captureBuffer.length()>0) {
      captured=captureBuffer.toString();
      captureBuffer.setLength(0);
    } else {
      captured="";
    }
    capture=false;
    return captured;
  }

  private ParseException expected(String expected) {
    if (isEndOfText()) {
      return error("Unexpected end of input");
    }
    return error("Expected "+expected);
  }

  private ParseException error(String message) {
    int column=index-lineOffset;
    int offset=isEndOfText()?index:index-1;
    return new ParseException(message, offset, line, column-1);
  }

  static boolean isWhiteSpace(int ch) {
    return ch==' ' || ch=='\t' || ch=='\n' || ch=='\r';
  }

  private boolean isWhiteSpace() {
    return isWhiteSpace((char)current);
  }

  private boolean isHexDigit() {
    return current>='0' && current<='9'
        || current>='a' && current<='f'
        || current>='A' && current<='F';
  }

  private boolean isEndOfText() {
    return current==-1;
  }
}