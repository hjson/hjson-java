/*******************************************************************************
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

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

class HjsonWriter {

  private IHjsonDsfProvider[] dsfProviders;
  private boolean bracesSameLine;
  private boolean allowCondense;
  private boolean allowMultiVal;
  private String space, commentSpace;

  static Pattern needsEscapeName=Pattern.compile("[,\\{\\[\\}\\]\\s:#\"']|//|/\\*");

  public HjsonWriter(HjsonOptions options) {
    if (options!=null) {
      dsfProviders=options.getDsfProviders();
      bracesSameLine=options.bracesSameLine();
      allowCondense=options.getAllowCondense();
      allowMultiVal=options.getAllowMultiVal();
      space=options.getSpace();
      commentSpace=options.getCommentSpace();
    } else {
      dsfProviders=new IHjsonDsfProvider[0];
      bracesSameLine=true;
      allowCondense=true;
      allowMultiVal=true;
      space="  ";
      commentSpace="";
    }
  }

  void nl(Writer tw, int level) throws IOException {
    tw.write(JsonValue.eol);
    indent(tw, level);
  }

  void indent(Writer tw, int level) throws IOException {
    for (int i=0; i<level; i++)
      tw.write(space);
  }

  void indentComment(Writer tw) throws IOException {
    tw.write(commentSpace);
  }

  // Backwards compatibility. Could be removed.
  public void save(JsonValue value, Writer tw, int level, String separator, boolean noIndent) throws IOException {
    save(value, tw, level, separator, noIndent, false);
  }

  public void save(JsonValue value, Writer tw, int level, String separator, boolean noIndent, boolean forceQuotes) throws IOException {
    if (value==null) {
      tw.write(separator);
      tw.write("null");
      return;
    }

    // check for DSF
    String dsfValue=HjsonDsf.stringify(dsfProviders, value);
    if (dsfValue!=null) {
      tw.write(separator);
      tw.write(dsfValue);
      return;
    }

    // Write the header, if applicable.
    if (level==0 && value.hasBOLComment()) {
      writeHeader(tw, value, level);
    }

    switch (value.getType()) {
      case OBJECT:
        JsonObject obj=value.asObject();
        writeObject(obj, tw, level, separator, noIndent, forceQuotes);
        break;
      case ARRAY:
        JsonArray arr=value.asArray();
        writeArray(arr, tw, level, separator, noIndent, forceQuotes);
        break;
      case BOOLEAN:
        tw.write(separator);
        tw.write(value.isTrue()?"true":"false");
        break;
      case STRING:
        tw.write(separator);
        if (forceQuotes) tw.write('"');
        writeString(value.asString(), tw, level, separator);
        if (forceQuotes) tw.write('"');
        break;
      default:
        tw.write(separator);
        if (forceQuotes) tw.write('"');
        tw.write(value.toString());
        if (forceQuotes) tw.write('"');
        break;
    }

    // Write any following comments.
    if (value.hasEOLComment()) {
      writeEOLComment(tw, value, level);
    }
  }

  void writeObject(JsonObject obj, Writer tw, int level, String separator, boolean noIndent, boolean forceQuotes) throws IOException {
    // Start the beginning of the container.
    openContainer(tw, noIndent, level, separator, '{');

    int index=0;
    for (JsonObject.Member pair : obj) {
      if (pair.getValue().hasBOLComment()) {
        writeBOLComment(tw, pair.getValue(), level);
      }

      handleContainerLines(tw, obj.isCondensed(), index, level, obj.getLineLength());
      tw.write(escapeName(pair.getName(), forceQuoteObject(obj)));
      tw.write(":");
      boolean forceQuoteValue = forceQuoteValue(pair.getValue(), obj);
      save(pair.getValue(), tw, level+1, " ", false, forceQuoteValue);
      index++;
    }
    // Put interior comments at the bottom.
    if (obj.hasInteriorComment()) {
      writeInteriorComment(tw, obj, level);
    }
    // We've reached the end of the container. Close it off.
    closeContainer(tw, obj.isCondensed(), obj.size(), level, '}');
  }

  void writeArray(JsonArray arr, Writer tw, int level, String separator, boolean noIndent, boolean forceQuotes) throws IOException {
    // Start the beginning of the container.
    openContainer(tw, noIndent, level, separator, '[');

    int n=arr.size();
    for (int i=0; i<n; i++) {
      JsonValue element = arr.get(i);
      if (element.hasBOLComment()) {
        writeBOLComment(tw, element, level+1);
      }
      handleContainerLines(tw, arr.isCondensed(), i, level, arr.getLineLength());
      // Multiple strings in an array would require quotes.
      boolean forceQuoteArray = forceQuoteArray(element, arr);
      save(element, tw, level+1, "", true, forceQuoteArray);
    }
    // Put the interior comments at the bottom.
    if (arr.hasInteriorComment()) {
      writeInteriorComment(tw, arr, level);
    }
    // We've reached the end of the container. Close it off.
    closeContainer(tw, arr.isCondensed(), n, level, ']');
  }

  void openContainer(Writer tw, boolean noIndent, int level, String separator, char openWith) throws IOException {
    if (!noIndent) { if (bracesSameLine) tw.write(separator); else nl(tw, level); }
    tw.write(openWith);
  }

  void writeHeader(Writer tw, JsonValue value, int level) throws IOException {
    writeComment(value.getBOLComment(), tw, level);
    nl(tw, level);
  }

  void writeBOLComment(Writer tw, JsonValue value, int level) throws IOException {
    nl(tw, level+1);
    writeComment(value.getBOLComment(), tw, level+1);
  }

  void writeInteriorComment(Writer tw, JsonValue value, int level) throws IOException {
    nl(tw, level+1);
    writeComment(value.getInteriorComment(), tw, level+1);
    nl(tw, level);
  }

  void writeEOLComment(Writer tw, JsonValue value, int level) throws IOException {
    if (level==0) {
      // if level==0, this is a footer.
      nl(tw, level);
      writeComment(value.getEOLComment(), tw, level);
    } else {
      // At EOL; no need to space more or less than once.
      tw.write(' ');
      tw.write(value.getEOLComment());
    }
  }

  void handleContainerLines(Writer tw, boolean compact, int index, int level, int lineLength) throws IOException {
    if (!allowMultiVal) {
      nl(tw, level+1);
    } else if (index%lineLength==0) {
      // NL every (lineLength) # lines.
      if (!(compact && allowCondense)) {
        nl(tw, level+1);
      } else { // Manually separate.
        tw.write(' ');
      }
    } else {
      tw.write(", ");
    }
  }

  void closeContainer(Writer tw, boolean compact, int size, int level, char closeWith) throws IOException {
    if (compact && allowCondense && allowMultiVal) tw.write(' ');
    else if (size>0) nl(tw, level);
    tw.write(closeWith);
  }

  static String escapeName(String name) {
    return escapeName(name, false);
  }

  static String escapeName(String name, boolean force) {
    if (force || name.length()==0 || needsEscapeName.matcher(name).find())
      return "\""+JsonWriter.escapeString(name)+"\"";
    else
      return name;
  }

  void writeString(String value, Writer tw, int level, String separator) throws IOException {
    if (value.length()==0) { tw.write("\"\""); return; }

    char left=value.charAt(0), right=value.charAt(value.length()-1);
    char left1=value.length()>1?value.charAt(1):'\0', left2=value.length()>2?value.charAt(2):'\0';
    boolean doEscape=false;
    char[] valuec=value.toCharArray();
    for(char ch : valuec) {
      if (needsQuotes(ch)) { doEscape=true; break; }
    }

    if (doEscape ||
      HjsonParser.isWhiteSpace(left) || HjsonParser.isWhiteSpace(right) ||
      left=='"' ||
      left=='\'' ||
      left=='#' ||
      left=='/' && (left1=='*' || left1=='/') ||
      JsonValue.isPunctuatorChar(left) ||
      HjsonParser.tryParseNumber(value, true)!=null ||
      startsWithKeyword(value)) {
      // If the String contains no control characters, no quote characters, and no
      // backslash characters, then we can safely slap some quotes around it.
      // Otherwise we first check if the String can be expressed in multiline
      // format or we must replace the offending characters with safe escape
      // sequences.

      boolean noEscape=true;
      for(char ch : valuec) { if (needsEscape(ch)) { noEscape=false; break; } }
      if (noEscape) { tw.write("\""+value+"\""); return; }

      boolean noEscapeML=true, allWhite=true;
      for(char ch : valuec) {
        if (needsEscapeML(ch)) { noEscapeML=false; break; }
        else if (!HjsonParser.isWhiteSpace(ch)) allWhite=false;
      }
      if (noEscapeML && !allWhite && !value.contains("'''")) writeMLString(value, tw, level, separator);
      else tw.write("\""+JsonWriter.escapeString(value)+"\"");
    }
    else tw.write(value);
  }

  void writeMLString(String value, Writer tw, int level, String separator) throws IOException {
    String[] lines=value.replace("\r", "").split("\n", -1);

    if (lines.length==1) {
      tw.write(separator+"'''");
      tw.write(lines[0]);
      tw.write("'''");
    }
    else {
      level++;
      nl(tw, level);
      tw.write("'''");

      for (String line : lines) {
        nl(tw, line.length()>0?level:0);
        tw.write(line);
      }
      nl(tw, level);
      tw.write("'''");
    }
  }

  void writeComment(String comment, Writer tw, int level) throws IOException {
    String[] lines = comment.split("\r?\n");
    // The first line is already indented. No nl() needed.
    indentComment(tw);
    tw.write(lines[0]);

    // The rest of the lines are not.
    for (int i=1; i<lines.length; i++) {
      nl(tw, level);
      indentComment(tw);
      tw.write(lines[i]);
    }
  }

  static boolean startsWithKeyword(String text) {
    int p;
    if (text.startsWith("true") || text.startsWith("null")) p=4;
    else if (text.startsWith("false")) p=5;
    else return false;
    while (p<text.length() && HjsonParser.isWhiteSpace(text.charAt(p))) p++;
    if (p==text.length()) return true;
    char ch=text.charAt(p);
    return ch==',' || ch=='}' || ch==']' || ch=='#' || ch=='/' && (text.length()>p+1 && (text.charAt(p+1)=='/' || text.charAt(p+1)=='*'));
  }

  static boolean forceQuoteArray(JsonValue value, JsonArray array) {
    return value.isString() && (array.isCondensed() || array.getLineLength()>1 || value.hasEOLComment());
  }

  // Technically different methods.
  static boolean forceQuoteValue(JsonValue value, JsonObject object) {
    return value.isString() && (object.isCondensed() || object.getLineLength()>1 || value.hasEOLComment());
  }

  static boolean forceQuoteObject(JsonObject object) {
    return object.isCondensed() || object.getLineLength() > 1;
  }

  static boolean needsQuotes(char c) {
    switch (c) {
      case '\t':
      case '\f':
      case '\b':
      case '\n':
      case '\r':
        return true;
      default:
        return false;
    }
  }

  static boolean needsEscape(char c) {
    switch (c) {
      case '\"':
      case '\\':
        return true;
      default:
        return needsQuotes(c);
    }
  }

  static boolean needsEscapeML(char c) {
    switch (c) {
      case '\n':
      case '\r':
      case '\t':
        return false;
      default:
        return needsQuotes(c);
    }
  }
}
