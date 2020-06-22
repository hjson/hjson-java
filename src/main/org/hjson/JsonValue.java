/*******************************************************************************
 * Copyright (c) 2013, 2015 EclipseSource.
 * Copyright (c) 2015-2016 Christian Zangl
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
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a JSON value. This can be a JSON <strong>object</strong>, an <strong> array</strong>,
 * a <strong>number</strong>, a <strong>string</strong>, or one of the literals
 * <strong>true</strong>, <strong>false</strong>, and <strong>null</strong>.
 * <p>
 * JSON <strong>objects</strong> and <strong>arrays</strong> are represented by the subtypes
 * {@link JsonObject} and {@link JsonArray}. Instances of these types can be created using the
 * public constructors of these classes.
 * </p>
 * <p>
 * Instances that represent JSON <strong>numbers</strong>, <strong>strings</strong> and
 * <strong>boolean</strong> values can be created using the static factory methods
 * {@link #valueOf(String)}, {@link #valueOf(long)}, {@link #valueOf(double)}, etc.
 * </p>
 * <p>
 * In order to find out whether an instance of this class is of a certain type, the methods
 * {@link #isObject()}, {@link #isArray()}, {@link #isString()}, {@link #isNumber()} etc. can be
 * used.
 * </p>
 * <p>
 * If the type of a JSON value is known, the methods {@link #asObject()}, {@link #asArray()},
 * {@link #asString()}, {@link #asInt()}, etc. can be used to get this value directly in the
 * appropriate target type.
 * </p>
 * <p>
 * This class is <strong>not supposed to be extended</strong> by clients.
 * </p>
 */
@SuppressWarnings("serial") // use default serial UID
public abstract class JsonValue implements Serializable {

  static String eol=System.getProperty("line.separator");

  /**
   * Comments that will be used by each type of value.
   */
  protected String bolComment="", eolComment="", intComment="";

  /**
   * A flag indicating whether this value has been specifically called for.
   */
  protected boolean accessed;

  /**
   * Gets the newline charater(s).
   *
   * @return the eol value
   */
  public static String getEol() { return eol; }

  /**
   * Sets the newline charater(s).
   *
   * @param value the eol value
   */
  public static void setEol(String value) {
    if (value.equals("\r\n") || value.equals("\n")) eol=value;
  }

  JsonValue() {
    // prevent subclasses outside of this package
  }

  /**
   * Reads a Hjson value from the given reader.
   * <p>
   * Characters are read in chunks and buffered internally, therefore wrapping an existing reader in
   * an additional <code>BufferedReader</code> does <strong>not</strong> improve reading
   * performance.
   * </p>
   *
   * @param reader the reader to read the Hjson value from
   * @return the Hjson value that has been read
   * @throws IOException if an I/O error occurs in the reader
   * @throws ParseException if the input is not valid Hjson
   */
  public static JsonValue readHjson(Reader reader) throws IOException {
    return new HjsonParser(reader, null).parse();
  }

  /**
   * Reads a Hjson value from the given string.
   *
   * @param text the string that contains the Hjson value
   * @return the Hjson value that has been read
   * @throws ParseException if the input is not valid Hjson
   */
  public static JsonValue readHjson(String text) {
    try {
      return new HjsonParser(text, null).parse();
    } catch(IOException exception) {
      // JsonParser does not throw IOException for String
      throw new RuntimeException(exception);
    }
  }

  /**
   * Reads a Hjson value from the given reader.
   * <p>
   * Characters are read in chunks and buffered internally, therefore wrapping an existing reader in
   * an additional <code>BufferedReader</code> does <strong>not</strong> improve reading
   * performance.
   * </p>
   *
   * @param reader the reader to read the Hjson value from
   * @param options the Hjson options
   * @return the Hjson value that has been read
   * @throws IOException if an I/O error occurs in the reader
   * @throws ParseException if the input is not valid Hjson
   */
  public static JsonValue readHjson(Reader reader, HjsonOptions options) throws IOException {
    return new HjsonParser(reader, options).parse();
  }

  /**
   * Reads a Hjson value from the given string.
   *
   * @param text the string that contains the Hjson value
   * @param options the Hjson options
   * @return the Hjson value that has been read
   * @throws ParseException if the input is not valid Hjson
   */
  public static JsonValue readHjson(String text, HjsonOptions options) {
    try {
      return new HjsonParser(text, options).parse();
    } catch(IOException exception) {
      // JsonParser does not throw IOException for String
      throw new RuntimeException(exception);
    }
  }
  /**
   * Reads a JSON value from the given reader.
   * <p>
   * Characters are read in chunks and buffered internally, therefore wrapping an existing reader in
   * an additional <code>BufferedReader</code> does <strong>not</strong> improve reading
   * performance.
   * </p>
   *
   * @param reader the reader to read the JSON value from
   * @return the JSON value that has been read
   * @throws IOException if an I/O error occurs in the reader
   * @throws ParseException if the input is not valid JSON
   */
  public static JsonValue readJSON(Reader reader) throws IOException {
    return new JsonParser(reader).parse();
  }

  /**
   * Reads a JSON value from the given string.
   *
   * @param text the string that contains the JSON value
   * @return the JSON value that has been read
   * @throws ParseException if the input is not valid JSON
   */
  public static JsonValue readJSON(String text) {
    try {
      return new JsonParser(text).parse();
    } catch(IOException exception) {
      // JsonParser does not throw IOException for String
      throw new RuntimeException(exception);
    }
  }

  /**
   * Returns a JsonValue instance that represents the given <code>int</code> value.
   *
   * @param value the value to get a JSON representation for
   * @return a JSON value that represents the given value
   */
  public static JsonValue valueOf(int value) {
    return new JsonNumber(value);
  }

  /**
   * Returns a JsonValue instance that represents the given <code>long</code> value.
   *
   * @param value the value to get a JSON representation for
   * @return a JSON value that represents the given value
   */
  public static JsonValue valueOf(long value) {
    return new JsonNumber(value);
  }

  /**
   * Returns a JsonValue instance that represents the given <code>float</code> value.
   *
   * @param value the value to get a JSON representation for
   * @return a JSON value that represents the given value
   */
  public static JsonValue valueOf(float value) {
    return new JsonNumber(value);
  }

  /**
   * Returns a JsonValue instance that represents the given <code>double</code> value.
   *
   * @param value the value to get a JSON representation for
   * @return a JSON value that represents the given value
   */
  public static JsonValue valueOf(double value) {
    return new JsonNumber(value);
  }

  /**
   * Returns a JsonValue instance that represents the given string.
   *
   * @param string the string to get a JSON representation for
   * @return a JSON value that represents the given string
   */
  public static JsonValue valueOf(String string) {
    return string==null ? JsonLiteral.jsonNull() : new JsonString(string);
  }

  /**
   * Returns a JsonValue instance that represents the given <code>boolean</code> value.
   *
   * @param value the value to get a JSON representation for
   * @return a JSON value that represents the given value
   */
  public static JsonValue valueOf(boolean value) {
    return value ? JsonLiteral.jsonTrue() : JsonLiteral.jsonFalse();
  }

  /**
   * Returns a JsonValue instance that represents the given DSF value.
   *
   * @param value the value to get a JSON representation for
   * @return a JSON value that represents the given value
   */
  public static JsonValue valueOfDsf(Object value) {
    return new JsonDsf(value);
  }

  /**
   * Returns a JsonValue from an Object of unknown type.
   * 
   * @param value the value to get a JSON representation for
   * @return a new JsonValue.
   */
  @SuppressWarnings("unchecked")
  public static JsonValue valueOf(Object value) {
    if (value instanceof Number) {
      return new JsonNumber(((Number) value).doubleValue());
    } else if (value instanceof String) {
      return new JsonString((String) value);
    } else if (value instanceof Boolean) {
      return (Boolean) value ? JsonLiteral.jsonTrue() : JsonLiteral.jsonFalse();
    } else if (value instanceof List) {
      JsonArray array=new JsonArray();
      for (Object o : (List) value) {
        array.add(valueOf(o));
      }
      return array;
    } else if (value instanceof Map) {
      Set<Map.Entry> entries=((Map)value).entrySet();
      JsonObject object=new JsonObject();
      for (Map.Entry entry : entries) {
        object.set(entry.getKey().toString(), valueOf(entry.getValue()));
      }
      return object;
    } else {
      throw new UnsupportedOperationException("Unable to determine type.");
    }
  }

  /**
   * Gets the type of this JSON value.
   *
   * @return the type for this instance.
   */
  public abstract JsonType getType();

  /**
   * Detects whether this value represents a JSON object. If this is the case, this value is an
   * instance of {@link JsonObject}.
   *
   * @return <code>true</code> if this value is an instance of JsonObject
   */
  public boolean isObject() {
    return false;
  }

  /**
   * Detects whether this value represents a JSON array. If this is the case, this value is an
   * instance of {@link JsonArray}.
   *
   * @return <code>true</code> if this value is an instance of JsonArray
   */
  public boolean isArray() {
    return false;
  }

  /**
   * Detects whether this value represents a JSON number.
   *
   * @return <code>true</code> if this value represents a JSON number
   */
  public boolean isNumber() {
    return false;
  }

  /**
   * Detects whether this value represents a JSON string.
   *
   * @return <code>true</code> if this value represents a JSON string
   */
  public boolean isString() {
    return false;
  }

  /**
   * Detects whether this value represents a boolean value.
   *
   * @return <code>true</code> if this value represents either the JSON literal <code>true</code> or
   *         <code>false</code>
   */
  public boolean isBoolean() {
    return false;
  }

  /**
   * Detects whether this value represents the JSON literal <code>true</code>.
   *
   * @return <code>true</code> if this value represents the JSON literal <code>true</code>
   */
  public boolean isTrue() {
    return false;
  }

  /**
   * Detects whether this value represents the JSON literal <code>false</code>.
   *
   * @return <code>true</code> if this value represents the JSON literal <code>false</code>
   */
  public boolean isFalse() {
    return false;
  }

  /**
   * Detects whether this value represents the JSON literal <code>null</code>.
   *
   * @return <code>true</code> if this value represents the JSON literal <code>null</code>
   */
  public boolean isNull() {
    return false;
  }

  /**
   *  Detects whether this value has been accessed in-code.
   * @return true if the value has been used.
   */
  public boolean isAccessed() {
    return accessed;
  }

  /**
   * Overrides whether this field has been accessed in-code.
   * @param b The value to override with.
   * @return This, to enable chaining.
   */
  public JsonValue setAccessed(boolean b) {
    accessed=b;
    return this;
  }

  /**
   * Detects whether this value contains any comments.
   *
   * @return <code>true</code> if this value does contain comments.
   */
  public boolean hasComments() {
    return hasBOLComment() || hasEOLComment() || hasInteriorComment();
  }

  /**
   * Detects whether this value contains any beginning of line comments.
   *
   * @return <code>true</code> if this value does contain any BOL comments.
   */
  public boolean hasBOLComment() { return !bolComment.isEmpty(); }

  /**
   * Detects whether this value contains any end of line comments.
   *
   * @return <code>true</code> if this value does contain any EOL comments.
   */
  public boolean hasEOLComment() { return !eolComment.isEmpty(); }

  /**
   * Detects whether this value contains any interior comments, which may be present inside of
   * object and array types with no association to any of their members.
   *
   * @return <code>true</code> if this value does contain any interior comments.
   */
  public boolean hasInteriorComment() { return !intComment.isEmpty(); }

  /**
   * Gets any comment that exists before this value.
   *
   * @return The full contents of this comment, including any comment indicators.
   */
  public String getBOLComment() { return bolComment; }

  /**
   * Gets any comment that exists after this value.
   *
   * @return The full contents of this comment, including any comment indicators.
   */
  public String getEOLComment() { return eolComment; }

  /**
   * Gets any non-BOL or EOL comment contained within this value.
   *
   * @return The full contents of this comment, including any comment indicators.
   */
  public String getInteriorComment() { return intComment; }

  /**
   * Adds a comment to be associated with this value.
   * @param type Whether to place this comment before the line, after the line, or inside of the
   *             object or array, if applicable.
   * @param style Whether to use <code>#</code>, <code>//</code>, or another such comment style.
   * @param comment The unformatted comment to be paired with this value.
   * @return this, to enable chaining
   */
  public JsonValue setComment(CommentType type, CommentStyle style, String comment) {
    StringBuilder formatted=new StringBuilder();
    if (style.equals(CommentStyle.BLOCK)){
      formatted.append("/*");
      formatted.append(eol);
      formatted.append(comment);
      formatted.append(eol);
      formatted.append("*/");
    } else {
      String[] lines=comment.split("\r?\n");
      // Iterate through all lines in the comment.
      for (int i=0; i<lines.length; i++) {
        // Don't concatenate lines.
        if (i>0) formatted.append(eol);
        // Add the indicator and extra space.
        if (style.equals(CommentStyle.HASH)){
          formatted.append("# ");
        } else {
          formatted.append("// ");
        }
        // Add the actual line from the comment.
        formatted.append(lines[i]);
      }
    }
    setFullComment(type, formatted.toString());
    return this;
  }

  /**
   * Shorthand for {@link #setComment(CommentType, CommentStyle, String)} which defaults to sending
   * a beginning of line, single line comment, using the default indicator, <code>#</code>.
   *
   * @param comment The unformatted comment to be paired with this value.
   * @return this, to enable chaining
   */
  public JsonValue setComment(String comment) {
    return setComment(CommentType.BOL, CommentStyle.HASH, comment);
  }

  /**
   * Shorthand for calling {@link #setComment(CommentType, CommentStyle, String)} which defaults to
   * sending an end of line, single line comment, using the default indicator, <code>#</code>.
   *
   * @param comment The unformatted comment to be paired with this value.
   * @return this, to enable chaining
   */
  public JsonValue setEOLComment(String comment) {
    return setComment(CommentType.EOL, CommentStyle.HASH, comment);
  }

  /**
   * Counterpart to {@link #setComment(CommentType, CommentStyle, String)} which receives
   * the full, formatted comment to be stored by this value.
   *
   * @param type Whether to place this comment before the line, after the line, or inside of the
   *             object or array, if applicable.
   * @param comment The fully-formatted comment to be paired with this value.
   * @return this, to enable chaining
   */
  public JsonValue setFullComment(CommentType type, String comment) {
    switch (type) {
      case BOL: bolComment=comment; break;
      case EOL: eolComment=comment; break;
      case INTERIOR: intComment=comment; break;
    }
    return this;
  }

  /**
   * Returns this JSON value as {@link JsonObject}, assuming that this value represents a JSON
   * object. If this is not the case, an exception is thrown.
   *
   * @return a JSONObject for this value
   * @throws UnsupportedOperationException if this value is not a JSON object
   */
  public JsonObject asObject() {
    throw new UnsupportedOperationException("Not an object: "+toString());
  }

  /**
   * Returns this JSON value as {@link JsonArray}, assuming that this value represents a JSON array.
   * If this is not the case, an exception is thrown.
   *
   * @return a JSONArray for this value
   * @throws UnsupportedOperationException if this value is not a JSON array
   */
  public JsonArray asArray() {
    throw new UnsupportedOperationException("Not an array: "+toString());
  }

  /**
   * Returns this JSON value as an <code>int</code> value, assuming that this value represents a
   * JSON number that can be interpreted as Java <code>int</code>. If this is not the case, an
   * exception is thrown.
   * <p>
   * To be interpreted as Java <code>int</code>, the JSON number must neither contain an exponent
   * nor a fraction part. Moreover, the number must be in the <code>Integer</code> range.
   * </p>
   *
   * @return this value as <code>int</code>
   * @throws UnsupportedOperationException if this value is not a JSON number
   * @throws NumberFormatException if this JSON number can not be interpreted as <code>int</code> value
   */
  public int asInt() {
    throw new UnsupportedOperationException("Not a number: "+toString());
  }

  /**
   * Returns this JSON value as a <code>long</code> value, assuming that this value represents a
   * JSON number that can be interpreted as Java <code>long</code>. If this is not the case, an
   * exception is thrown.
   * <p>
   * To be interpreted as Java <code>long</code>, the JSON number must neither contain an exponent
   * nor a fraction part. Moreover, the number must be in the <code>Long</code> range.
   * </p>
   *
   * @return this value as <code>long</code>
   * @throws UnsupportedOperationException if this value is not a JSON number
   * @throws NumberFormatException if this JSON number can not be interpreted as <code>long</code> value
   */
  public long asLong() {
    throw new UnsupportedOperationException("Not a number: "+toString());
  }

  /**
   * Returns this JSON value as a <code>float</code> value, assuming that this value represents a
   * JSON number. If this is not the case, an exception is thrown.
   * <p>
   * If the JSON number is out of the <code>Float</code> range, {@link Float#POSITIVE_INFINITY} or
   * {@link Float#NEGATIVE_INFINITY} is returned.
   * </p>
   *
   * @return this value as <code>float</code>
   * @throws UnsupportedOperationException if this value is not a JSON number
   */
  public float asFloat() {
    throw new UnsupportedOperationException("Not a number: "+toString());
  }

  /**
   * Returns this JSON value as a <code>double</code> value, assuming that this value represents a
   * JSON number. If this is not the case, an exception is thrown.
   * <p>
   * If the JSON number is out of the <code>Double</code> range, {@link Double#POSITIVE_INFINITY} or
   * {@link Double#NEGATIVE_INFINITY} is returned.
   * </p>
   *
   * @return this value as <code>double</code>
   * @throws UnsupportedOperationException if this value is not a JSON number
   */
  public double asDouble() {
    throw new UnsupportedOperationException("Not a number: "+toString());
  }

  /**
   * Returns this JSON value as String, assuming that this value represents a JSON string. If this
   * is not the case, an exception is thrown.
   *
   * @return the string represented by this value
   * @throws UnsupportedOperationException if this value is not a JSON string
   */
  public String asString() {
    throw new UnsupportedOperationException("Not a string: "+toString());
  }

  /**
   * Returns this JSON value as a <code>boolean</code> value, assuming that this value is either
   * <code>true</code> or <code>false</code>. If this is not the case, an exception is thrown.
   *
   * @return this value as <code>boolean</code>
   * @throws UnsupportedOperationException if this value is neither <code>true</code> or <code>false</code>
   */
  public boolean asBoolean() {
    throw new UnsupportedOperationException("Not a boolean: "+toString());
  }

  /**
   * Returns this JSON value as a DSF object, assuming that this value represents a DSF. If this
   * is not the case, an exception is thrown.
   *
   * @return the object represented by this value
   * @throws UnsupportedOperationException if this value is not a DSF
   */
  public Object asDsf() {
    throw new UnsupportedOperationException("Not a DSF");
  }

  /**
   * Unsafe. Returns the raw form of this JSON value. For compatibility with other config wrappers.
   * @param <T> the type of object to be returned.
   * @return the raw object.
   * @throws ClassCastException when the type returned does not match the original value.
   */
  @SuppressWarnings("unchecked")
  public <T> T asRaw() {
    switch (getType()) {
      case STRING : return (T) asString();
      case NUMBER : return (T) Double.valueOf(asDouble());
      case OBJECT : return (T) asObject();
      case ARRAY : return (T) asArray().asRawList();
      case BOOLEAN : return (T) Boolean.valueOf(asBoolean());
      case DSF : return (T) asDsf();
      default : return null;
    }
  }

  /**
   * Writes the JSON representation of this value to the given writer in its minimal form, without
   * any additional whitespace.
   * <p>
   * Writing performance can be improved by using a {@link java.io.BufferedWriter BufferedWriter}.
   * </p>
   *
   * @param writer the writer to write this value to
   * @throws IOException if an I/O error occurs in the writer
   */
  public void writeTo(Writer writer) throws IOException {
    writeTo(writer, Stringify.PLAIN);
  }

  /**
   * Writes the JSON/Hjson representation of this value to the given writer using the given formatting.
   * <p>
   * Writing performance can be improved by using a {@link java.io.BufferedWriter BufferedWriter}.
   * </p>
   *
   * @param writer the writer to write this value to
   * @param format controls the formatting
   * @throws IOException if an I/O error occurs in the writer
   */
  public void writeTo(Writer writer, Stringify format) throws IOException {
    WritingBuffer buffer=new WritingBuffer(writer, 128);
    switch (format) {
      case PLAIN: new JsonWriter(false).save(this, buffer, 0); break;
      case FORMATTED: new JsonWriter(true).save(this, buffer, 0); break;
      case HJSON: new HjsonWriter(null, false).save(this, buffer, 0, "", true); break;
      case HJSON_COMMENTS: new HjsonWriter(null, true).save(this, buffer, 0, "", true); break;
    }
    buffer.flush();
  }

  /**
   * Writes the Hjson representation of this value to the given writer.
   * <p>
   * Writing performance can be improved by using a {@link java.io.BufferedWriter BufferedWriter}.
   * </p>
   *
   * @param writer the writer to write this value to
   * @param options options for the Hjson format
   * @throws IOException if an I/O error occurs in the writer
   */
  public void writeTo(Writer writer, HjsonOptions options) throws IOException {
    if (options==null) throw new NullPointerException("options is null");
    WritingBuffer buffer=new WritingBuffer(writer, 128);
    new HjsonWriter(options).save(this, buffer, 0, "", true);
    buffer.flush();
  }

  /**
   * Returns the JSON string for this value in its minimal form, without any additional whitespace.
   * The result is guaranteed to be a valid input for the method {@link #readJSON(String)} and to
   * create a value that is <em>equal</em> to this object.
   *
   * @return a JSON string that represents this value
   */
  @Override
  public String toString() {
    return toString(Stringify.PLAIN);
  }

  /**
   * Returns the JSON/Hjson string for this value using the given formatting.
   *
   * @param format controls the formatting
   * @return a JSON/Hjson string that represents this value
   */
  public String toString(Stringify format) {
    StringWriter writer=new StringWriter();
    try {
      writeTo(writer, format);
    } catch(IOException exception) {
      // StringWriter does not throw IOExceptions
      throw new RuntimeException(exception);
    }
    return writer.toString();
  }

  /**
   * Returns the Hjson string for this value using the given formatting.
   *
   * @param options options for the Hjson format
   * @return a Hjson string that represents this value
   */
  public String toString(HjsonOptions options) {
    StringWriter writer=new StringWriter();
    try {
      writeTo(writer, options);
    } catch(IOException exception) {
      // StringWriter does not throw IOExceptions
      throw new RuntimeException(exception);
    }
    return writer.toString();
  }
  /**
   * Indicates whether some other object is "equal to" this one according to the contract specified
   * in {@link Object#equals(Object)}.
   * <p>
   * Two JsonValues are considered equal if and only if they represent the same JSON text. As a
   * consequence, two given JsonObjects may be different even though they contain the same set of
   * names with the same values, but in a different order.
   * </p>
   *
   * @param object the reference object with which to compare
   * @return true if this object is the same as the object argument; false otherwise
   */
  @Override
  public boolean equals(Object object) {
    return super.equals(object);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  static boolean isPunctuatorChar(int c) {
    return c == '{' || c == '}' || c == '[' || c == ']' || c == ',' || c == ':';
  }
}
