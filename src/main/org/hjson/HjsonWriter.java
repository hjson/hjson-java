/*******************************************************************************
 * Copyright (c) 2015 Christian Zangl
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HjsonWriter {

  boolean emitRootBraces;
  static Pattern needsEscapeName=Pattern.compile("[,\\{\\[\\}\\]\\s:#\"]|//|/\\*|'''");

  public HjsonWriter(HjsonOptions options) {
    if (options!=null) emitRootBraces=options.emitRootBraces;
    else emitRootBraces=true;
  }

  void nl(Writer tw, int level) throws IOException {
    tw.write(JsonValue.eol);
    for (int i=0; i<level; i++) tw.write("  ");
  }

  public void save(JsonValue value, Writer tw, int level, String separator, boolean noIndent, boolean isRootObject) throws IOException {
    if (value==null) {
      tw.write(separator);
      tw.write("null");
      return;
    }
    switch (value.getType()) {
      case OBJECT:
        JsonObject obj=value.asObject();
        boolean showBraces=!isRootObject || emitRootBraces;
        if (!noIndent) { if (obj.size()>0) nl(tw, level); else tw.write(separator); }
        if (showBraces) tw.write('{');
        else level--; // reduce level for root

        boolean skipFirst=!showBraces;
        for (JsonObject.Member pair : obj) {
          if (!skipFirst) nl(tw, level+1); else skipFirst=false;
          tw.write(escapeName(pair.getName()));
          tw.write(":");
          save(pair.getValue(), tw, level+1, " ", false, false);
        }

        if (showBraces) {
          if (obj.size()>0) nl(tw, level);
          tw.write('}');
        }
        break;
      case ARRAY:
        JsonArray arr=value.asArray();
        int n=arr.size();
        if (!noIndent) { if (n>0) nl(tw, level); else tw.write(separator); }
        tw.write('[');
        for (int i=0; i<n; i++) {
          nl(tw, level+1);
          save(arr.get(i), tw, level+1, "", true, false);
        }
        if (n>0) nl(tw, level);
        tw.write(']');
        break;
      case BOOLEAN:
        tw.write(separator);
        tw.write(value.isTrue()?"true":"false");
        break;
      case STRING:
        writeString(value.asString(), tw, level, separator);
        break;
      default:
        tw.write(separator);
        tw.write(value.toString());
        break;
    }
  }

  static String escapeName(String name) {
    if (name.length()==0 || needsEscapeName.matcher(name).find())
      return "\""+JsonWriter.escapeString(name)+"\"";
    else
      return name;
  }

  void writeString(String value, Writer tw, int level, String separator) throws IOException {
    if (value.length()==0) { tw.write(separator+"\"\""); return; }

    char left=value.charAt(0), right=value.charAt(value.length()-1);
    char left1=value.length()>1?value.charAt(1):'\0', left2=value.length()>2?value.charAt(2):'\0';
    boolean doEscape=false;
    char[] valuec=value.toCharArray();
    for(char ch : valuec) {
      if (needsQuotes(ch)) { doEscape=true; break; }
    }

    if (doEscape ||
      HjsonParser.isWhiteSpace(left) ||
      left=='"' ||
      left=='\'' && left1=='\'' && left2=='\'' ||
      left=='#' ||
      left=='/' && (left1=='*' || left1=='/') ||
      left=='{' ||
      left=='[' ||
      HjsonParser.isWhiteSpace(right) ||
      HjsonParser.tryParseNumber(value, true)!=null ||
      startsWithKeyword(value)) {
      // If the String contains no control characters, no quote characters, and no
      // backslash characters, then we can safely slap some quotes around it.
      // Otherwise we first check if the String can be expressed in multiline
      // format or we must replace the offending characters with safe escape
      // sequences.

      boolean test=false;
      for(char ch : valuec) { if (needsEscape(ch)) { test=true; break; } }
      if (!test) { tw.write(separator+"\""+value+"\""); return; }

      test=false;
      for(char ch : valuec) { if (needsEscapeML(ch)) { test=true; break; } }
      if (!test && !value.contains("'''")) writeMLString(value, tw, level, separator);
      else tw.write(separator+"\""+JsonWriter.escapeString(value)+"\"");
    }
    else tw.write(separator+value);
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
        return false;
      default:
        return needsQuotes(c);
    }
  }
}
