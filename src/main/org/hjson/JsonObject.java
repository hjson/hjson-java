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
import java.io.ObjectInputStream;
import java.util.*;

import org.hjson.JsonObject.Member;

/**
 * Represents a JSON object, a set of name/value pairs, where the names are strings and the values
 * are JSON values.
 * <p>
 * Members can be added using the <code>add(String, ...)</code> methods which accept instances of
 * {@link JsonValue}, strings, primitive numbers, and boolean values. To modify certain values of an
 * object, use the <code>set(String, ...)</code> methods. Please note that the <code>add</code>
 * methods are faster than <code>set</code> as they do not search for existing members. On the other
 * hand, the <code>add</code> methods do not prevent adding multiple members with the same name.
 * Duplicate names are discouraged but not prohibited by JSON.
 * </p>
 * <p>
 * Members can be accessed by their name using {@link #get(String)}. A list of all names can be
 * obtained from the method {@link #names()}. This class also supports iterating over the members in
 * document order using an {@link #iterator()} or an enhanced for loop:
 * </p>
 * <pre>
 * for (Member member : jsonObject) {
 *   String name=member.getName();
 *   JsonValue value=member.getValue();
 *   ...
 * }
 * </pre>
 * <p>
 * Even though JSON objects are unordered by definition, instances of this class preserve the order
 * of members to allow processing in document order and to guarantee a predictable output.
 * </p>
 * <p>
 * Note that this class is <strong>not thread-safe</strong>. If multiple threads access a
 * <code>JsonObject</code> instance concurrently, while at least one of these threads modifies the
 * contents of this object, access to the instance must be synchronized externally. Failure to do so
 * may lead to an inconsistent state.
 * </p>
 * <p>
 * This class is <strong>not supposed to be extended</strong> by clients.
 * </p>
 */
@SuppressWarnings("serial")
// use default serial UID
public class JsonObject extends JsonValue implements Iterable<Member> {

  private final List<String> names;
  private final List<JsonValue> values;
  private transient HashIndexTable table;
  private transient boolean condensed;
  private transient int lineLength;

  /**
   * Creates a new empty JsonObject.
   */
  public JsonObject() {
    names=new ArrayList<String>();
    values=new ArrayList<JsonValue>();
    table=new HashIndexTable();
    condensed=false;
    lineLength=1;
  }

  /**
   * Creates a new JsonObject, initialized with the contents of the specified JSON object.
   *
   * @param object
   *          the JSON object to get the initial contents from, must not be <code>null</code>
   */
  public JsonObject(JsonObject object) {
    this(object, false);
  }

  private JsonObject(JsonObject object, boolean unmodifiable) {
    if (object==null) throw new NullPointerException("object is null");
    if (unmodifiable) {
      names=Collections.unmodifiableList(object.names);
      values=Collections.unmodifiableList(object.values);
    } else {
      names=new ArrayList<String>(object.names);
      values=new ArrayList<JsonValue>(object.values);
    }
    table=new HashIndexTable();
    condensed=object.condensed;
    lineLength=object.lineLength;
    updateHashIndex();
  }

  /**
   * Returns an unmodifiable JsonObject for the specified one. This method allows to provide
   * read-only access to a JsonObject.
   * <p>
   * The returned JsonObject is backed by the given object and reflect changes that happen to it.
   * Attempts to modify the returned JsonObject result in an
   * <code>UnsupportedOperationException</code>.
   * </p>
   *
   * @param object
   *          the JsonObject for which an unmodifiable JsonObject is to be returned
   * @return an unmodifiable view of the specified JsonObject
   */
  public static JsonObject unmodifiableObject(JsonObject object) {
    return new JsonObject(object, true);
  }

  /**
   * Returns whether the input key is contained within this object.
   *
   * @param name The name of the key to search for.
   * @return whether the key exists.
   */
  public boolean has(String name) {
    return indexOf(name)!=-1;
  }

  /**
   * Appends a new member to the end of this object, with the specified name and the JSON
   * representation of the specified <code>int</code> value.
   * <p>
   * This method <strong>does not prevent duplicate names</strong>. Calling this method with a name
   * that already exists in the object will append another member with the same name. In order to
   * replace existing members, use the method <code>set(name, value)</code> instead. However,
   * <strong> <em>add</em> is much faster than <em>set</em></strong> (because it does not need to
   * search for existing members). Therefore <em>add</em> should be preferred when constructing new
   * objects.
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, int value) {
    return add(name, valueOf(value));
  }

  /**
   * Variant of {@link #add(String, int)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, int value, String comment) {
    return add(name, valueOf(value).setComment(comment));
  }

  /**
   * Appends a new member to the end of this object, with the specified name and the JSON
   * representation of the specified <code>long</code> value.
   * <p>
   * This method <strong>does not prevent duplicate names</strong>. Calling this method with a name
   * that already exists in the object will append another member with the same name. In order to
   * replace existing members, use the method <code>set(name, value)</code> instead. However,
   * <strong> <em>add</em> is much faster than <em>set</em></strong> (because it does not need to
   * search for existing members). Therefore <em>add</em> should be preferred when constructing new
   * objects.
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, long value) {
    return add(name, valueOf(value));
  }

  /**
   * Variant of {@link #add(String, long)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, long value, String comment) {
    return add(name, valueOf(value).setComment(comment));
  }

  /**
   * Appends a new member to the end of this object, with the specified name and the JSON
   * representation of the specified <code>float</code> value.
   * <p>
   * This method <strong>does not prevent duplicate names</strong>. Calling this method with a name
   * that already exists in the object will append another member with the same name. In order to
   * replace existing members, use the method <code>set(name, value)</code> instead. However,
   * <strong> <em>add</em> is much faster than <em>set</em></strong> (because it does not need to
   * search for existing members). Therefore <em>add</em> should be preferred when constructing new
   * objects.
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, float value) {
    return add(name, valueOf(value));
  }

  /**
   * Variant of {@link #add(String, float)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, float value, String comment) {
    return add(name, valueOf(value).setComment(comment));
  }

  /**
   * Appends a new member to the end of this object, with the specified name and the JSON
   * representation of the specified <code>double</code> value.
   * <p>
   * This method <strong>does not prevent duplicate names</strong>. Calling this method with a name
   * that already exists in the object will append another member with the same name. In order to
   * replace existing members, use the method <code>set(name, value)</code> instead. However,
   * <strong> <em>add</em> is much faster than <em>set</em></strong> (because it does not need to
   * search for existing members). Therefore <em>add</em> should be preferred when constructing new
   * objects.
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, double value) {
    return add(name, valueOf(value));
  }

  /**
   * Variant of {@link #add(String, double)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, double value, String comment) {
    return add(name, valueOf(value).setComment(comment));
  }

  /**
   * Appends a new member to the end of this object, with the specified name and the JSON
   * representation of the specified <code>boolean</code> value.
   * <p>
   * This method <strong>does not prevent duplicate names</strong>. Calling this method with a name
   * that already exists in the object will append another member with the same name. In order to
   * replace existing members, use the method <code>set(name, value)</code> instead. However,
   * <strong> <em>add</em> is much faster than <em>set</em></strong> (because it does not need to
   * search for existing members). Therefore <em>add</em> should be preferred when constructing new
   * objects.
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, boolean value) {
    return add(name, valueOf(value));
  }

  /**
   * Variant of {@link #add(String, boolean)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, boolean value, String comment) {
    return add(name, valueOf(value).setComment(comment));
  }

  /**
   * Appends a new member to the end of this object, with the specified name and the JSON
   * representation of the specified string.
   * <p>
   * This method <strong>does not prevent duplicate names</strong>. Calling this method with a name
   * that already exists in the object will append another member with the same name. In order to
   * replace existing members, use the method <code>set(name, value)</code> instead. However,
   * <strong> <em>add</em> is much faster than <em>set</em></strong> (because it does not need to
   * search for existing members). Therefore <em>add</em> should be preferred when constructing new
   * objects.
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, String value) {
    return add(name, valueOf(value));
  }

  /**
   * Variant of {@link #add(String, int)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, String value, String comment) {
    return add(name, valueOf(value).setComment(comment));
  }

  /**
   * Appends a new member to the end of this object, with the specified name and the specified JSON
   * value.
   * <p>
   * This method <strong>does not prevent duplicate names</strong>. Calling this method with a name
   * that already exists in the object will append another member with the same name. In order to
   * replace existing members, use the method <code>set(name, value)</code> instead. However,
   * <strong> <em>add</em> is much faster than <em>set</em></strong> (because it does not need to
   * search for existing members). Therefore <em>add</em> should be preferred when constructing new
   * objects.
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add, must not be <code>null</code>
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, JsonValue value) {
    if (name==null) {
      throw new NullPointerException("name is null");
    }
    if (value==null) {
      throw new NullPointerException("value is null");
    }
    table.add(name, names.size());
    names.add(name);
    values.add(value);
    return this;
  }

  /**
   * Variant of {@link #add(String, JsonValue)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject add(String name, JsonValue value, String comment) {
    if (value==null) {
      throw new NullPointerException("value is null");
    }
    value.setComment(comment);
    return add(name, value);
  }

  /**
   * Sets the value of the member with the specified name to the JSON representation of the
   * specified <code>int</code> value. If this object does not contain a member with this name, a
   * new member is added at the end of the object. If this object contains multiple members with
   * this name, only the last one is changed.
   * <p>
   * This method should <strong>only be used to modify existing objects</strong>. To fill a new
   * object with members, the method <code>add(name, value)</code> should be preferred which is much
   * faster (as it does not need to search for existing members).
   * </p>
   *
   * @param name
   *          the name of the member to replace
   * @param value
   *          the value to set to the member
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, int value) {
    return set(name, valueOf(value));
  }

  /**
   * Variant of {@link #set(String, int)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, int value, String comment) {
    return set(name, valueOf(value).setComment(comment));
  }

  /**
   * Sets the value of the member with the specified name to the JSON representation of the
   * specified <code>long</code> value. If this object does not contain a member with this name, a
   * new member is added at the end of the object. If this object contains multiple members with
   * this name, only the last one is changed.
   * <p>
   * This method should <strong>only be used to modify existing objects</strong>. To fill a new
   * object with members, the method <code>add(name, value)</code> should be preferred which is much
   * faster (as it does not need to search for existing members).
   * </p>
   *
   * @param name
   *          the name of the member to replace
   * @param value
   *          the value to set to the member
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, long value) {
    return set(name, valueOf(value));
  }
  /**
   * Variant of {@link #set(String, long)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, long value, String comment) {
    return set(name, valueOf(value).setComment(comment));
  }

  /**
   * Sets the value of the member with the specified name to the JSON representation of the
   * specified <code>float</code> value. If this object does not contain a member with this name, a
   * new member is added at the end of the object. If this object contains multiple members with
   * this name, only the last one is changed.
   * <p>
   * This method should <strong>only be used to modify existing objects</strong>. To fill a new
   * object with members, the method <code>add(name, value)</code> should be preferred which is much
   * faster (as it does not need to search for existing members).
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, float value) {
    return set(name, valueOf(value));
  }

  /**
   * Variant of {@link #set(String, float)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, float value, String comment) {
    return set(name, valueOf(value).setComment(comment));
  }

  /**
   * Sets the value of the member with the specified name to the JSON representation of the
   * specified <code>double</code> value. If this object does not contain a member with this name, a
   * new member is added at the end of the object. If this object contains multiple members with
   * this name, only the last one is changed.
   * <p>
   * This method should <strong>only be used to modify existing objects</strong>. To fill a new
   * object with members, the method <code>add(name, value)</code> should be preferred which is much
   * faster (as it does not need to search for existing members).
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, double value) {
    return set(name, valueOf(value));
  }

  /**
   * Variant of {@link #set(String, double)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, double value, String comment) {
    return set(name, valueOf(value).setComment(comment));
  }

  /**
   * Sets the value of the member with the specified name to the JSON representation of the
   * specified <code>boolean</code> value. If this object does not contain a member with this name,
   * a new member is added at the end of the object. If this object contains multiple members with
   * this name, only the last one is changed.
   * <p>
   * This method should <strong>only be used to modify existing objects</strong>. To fill a new
   * object with members, the method <code>add(name, value)</code> should be preferred which is much
   * faster (as it does not need to search for existing members).
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, boolean value) {
    return set(name, valueOf(value));
  }

  /**
   * Variant of {@link #set(String, boolean)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, boolean value, String comment) {
    return set(name, valueOf(value).setComment(comment));
  }

  /**
   * Sets the value of the member with the specified name to the JSON representation of the
   * specified string. If this object does not contain a member with this name, a new member is
   * added at the end of the object. If this object contains multiple members with this name, only
   * the last one is changed.
   * <p>
   * This method should <strong>only be used to modify existing objects</strong>. To fill a new
   * object with members, the method <code>add(name, value)</code> should be preferred which is much
   * faster (as it does not need to search for existing members).
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, String value) {
    return set(name, valueOf(value));
  }

  /**
   * Variant of {@link #set(String, String)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, String value, String comment) {
    return set(name, valueOf(value).setComment(comment));
  }

  /**
   * Sets the value of the member with the specified name to the specified JSON value. If this
   * object does not contain a member with this name, a new member is added at the end of the
   * object. If this object contains multiple members with this name, only the last one is changed.
   * <p>
   * This method should <strong>only be used to modify existing objects</strong>. To fill a new
   * object with members, the method <code>add(name, value)</code> should be preferred which is much
   * faster (as it does not need to search for existing members).
   * </p>
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add, must not be <code>null</code>
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, JsonValue value) {
    if (name==null) {
      throw new NullPointerException("name is null");
    }
    if (value==null) {
      throw new NullPointerException("value is null");
    }
    value.setAccessed(true);
    int index=indexOf(name);
    if (index!=-1) {
      values.set(index, value);
    } else {
      table.add(name, names.size());
      names.add(name);
      values.add(value);
    }
    return this;
  }

  /**
   * Variant of {@link #set(String, JsonValue)} which appends a standard comment before the beginning of
   * this value.
   *
   * @param name
   *          the name of the member to add
   * @param value
   *          the value of the member to add
   * @param comment
   *          the string to be used as this value's comment
   * @return the object itself, to enable method chaining
   */
  public JsonObject set(String name, JsonValue value, String comment) {
    if (value==null) {
      throw new NullPointerException("value is null");
    }
    return set(name, value.setComment(comment));
  }

  /**
   * Locates a member of this object according to the key <code>name</code> and appends a standard, BOL
   * comment.
   *
   * @param name
   *          the name of the member to be altered.
   * @param comment
   *          the value to set as this member's comment.
   * @return the object itself to enable chaining.
   */
  public JsonObject setComment(String name, String comment) {
    get(name).setComment(comment);
    return this;
  }

  /**
   * Locates a member of this object according to the key <code>name</code> and appends a new comment
   * according to the input parameters.
   *
   * @param name
   *          The name of the member to be altered.
   * @param type
   *          The where the comment should be placed relative to its value.
   * @param style
   *          The style to use, i.e. <code>#</code>, <code>//</code>, etc.
   * @param comment
   *          the value to set as this member's comment.
   * @return the object itself to enable chaining.
   */
  public JsonObject setComment(String name, CommentType type, CommentStyle style, String comment) {
    get(name).setComment(type, style, comment);
    return this;
  }

  /**
   * Removes a member with the specified name from this object. If this object contains multiple
   * members with the given name, only the last one is removed. If this object does not contain a
   * member with the specified name, the object is not modified.
   *
   * @param name
   *          the name of the member to remove
   * @return the object itself, to enable method chaining
   */
  public JsonObject remove(String name) {
    if (name==null) {
      throw new NullPointerException("name is null");
    }
    int index=indexOf(name);
    if (index!=-1) {
      table.remove(index);
      names.remove(index);
      values.remove(index);
    }
    return this;
  }

  /**
   * Returns the value of the member with the specified name in this object. If this object contains
   * multiple members with the given name, this method will return the last one.
   *
   * @param name
   *          the name of the member whose value is to be returned
   * @return the value of the last member with the specified name, or <code>null</code> if this
   *         object does not contain a member with that name
   */
  public JsonValue get(String name) {
    if (name==null) {
      throw new NullPointerException("name is null");
    }
    int index=indexOf(name);
    return index!=-1 ? values.get(index).setAccessed(true) : null;
  }

  /**
   * Returns the <code>int</code> value of the member with the specified name in this object. If
   * this object does not contain a member with this name, the given default value is returned. If
   * this object contains multiple members with the given name, the last one will be picked. If this
   * member's value does not represent a JSON number or if it cannot be interpreted as Java
   * <code>int</code>, an exception is thrown.
   *
   * @param name
   *          the name of the member whose value is to be returned
   * @param defaultValue
   *          the value to be returned if the requested member is missing
   * @return the value of the last member with the specified name, or the given default value if
   *         this object does not contain a member with that name
   */
  public int getInt(String name, int defaultValue) {
    JsonValue value=get(name);
    return value!=null ? value.asInt() : defaultValue;
  }

  /**
   * Returns the <code>long</code> value of the member with the specified name in this object. If
   * this object does not contain a member with this name, the given default value is returned. If
   * this object contains multiple members with the given name, the last one will be picked. If this
   * member's value does not represent a JSON number or if it cannot be interpreted as Java
   * <code>long</code>, an exception is thrown.
   *
   * @param name
   *          the name of the member whose value is to be returned
   * @param defaultValue
   *          the value to be returned if the requested member is missing
   * @return the value of the last member with the specified name, or the given default value if
   *         this object does not contain a member with that name
   */
  public long getLong(String name, long defaultValue) {
    JsonValue value=get(name);
    return value!=null ? value.asLong() : defaultValue;
  }

  /**
   * Returns the <code>float</code> value of the member with the specified name in this object. If
   * this object does not contain a member with this name, the given default value is returned. If
   * this object contains multiple members with the given name, the last one will be picked. If this
   * member's value does not represent a JSON number or if it cannot be interpreted as Java
   * <code>float</code>, an exception is thrown.
   *
   * @param name
   *          the name of the member whose value is to be returned
   * @param defaultValue
   *          the value to be returned if the requested member is missing
   * @return the value of the last member with the specified name, or the given default value if
   *         this object does not contain a member with that name
   */
  public float getFloat(String name, float defaultValue) {
    JsonValue value=get(name);
    return value!=null ? value.asFloat() : defaultValue;
  }

  /**
   * Returns the <code>double</code> value of the member with the specified name in this object. If
   * this object does not contain a member with this name, the given default value is returned. If
   * this object contains multiple members with the given name, the last one will be picked. If this
   * member's value does not represent a JSON number or if it cannot be interpreted as Java
   * <code>double</code>, an exception is thrown.
   *
   * @param name
   *          the name of the member whose value is to be returned
   * @param defaultValue
   *          the value to be returned if the requested member is missing
   * @return the value of the last member with the specified name, or the given default value if
   *         this object does not contain a member with that name
   */
  public double getDouble(String name, double defaultValue) {
    JsonValue value=get(name);
    return value!=null ? value.asDouble() : defaultValue;
  }

  /**
   * Returns the <code>boolean</code> value of the member with the specified name in this object. If
   * this object does not contain a member with this name, the given default value is returned. If
   * this object contains multiple members with the given name, the last one will be picked. If this
   * member's value does not represent a JSON <code>true</code> or <code>false</code> value, an
   * exception is thrown.
   *
   * @param name
   *          the name of the member whose value is to be returned
   * @param defaultValue
   *          the value to be returned if the requested member is missing
   * @return the value of the last member with the specified name, or the given default value if
   *         this object does not contain a member with that name
   */
  public boolean getBoolean(String name, boolean defaultValue) {
    JsonValue value=get(name);
    return value!=null ? value.asBoolean() : defaultValue;
  }

  /**
   * Returns the <code>String</code> value of the member with the specified name in this object. If
   * this object does not contain a member with this name, the given default value is returned. If
   * this object contains multiple members with the given name, the last one is picked. If this
   * member's value does not represent a JSON string, an exception is thrown.
   *
   * @param name
   *          the name of the member whose value is to be returned
   * @param defaultValue
   *          the value to be returned if the requested member is missing
   * @return the value of the last member with the specified name, or the given default value if
   *         this object does not contain a member with that name
   */
  public String getString(String name, String defaultValue) {
    JsonValue value=get(name);
    return value!=null ? value.asString() : defaultValue;
  }

  /**
   * Returns the number of members (name/value pairs) in this object.
   *
   * @return the number of members in this object
   */
  public int size() {
    return names.size();
  }

  /**
   * Returns <code>true</code> if this object contains no members.
   *
   * @return <code>true</code> if this object contains no members
   */
  public boolean isEmpty() {
    return names.isEmpty();
  }

  /**
   * Returns a list of the names in this object in document order. The returned list is backed by
   * this object and will reflect subsequent changes. It cannot be used to modify this object.
   * Attempts to modify the returned list will result in an exception.
   *
   * @return a list of the names in this object
   */
  public List<String> names() {
    return Collections.unmodifiableList(names);
  }

  /**
   * Gets the number of elements to be displayed on a single line when this array is serialized.
   *
   * @return the number of elements per-line.
   */
  public int getLineLength() { return lineLength; }

  /**
   * Sets the number of elements to be displayed on a single line when this array is serialized.
   * This does not check whether an incorrect comment syntax is used. As a result, you may wind
   * up breaking your file when any element contains a single line comment.
   *
   * @param value
   *           the number of elements to be displayed per-line.
   * @return the object itself, to enable method chaining
   */
  public JsonObject setLineLength(int value) { lineLength=value; return this; }

  /**
   * Detects whether this object is "condensed" i.e. whether it should be displayed entirely on
   * one line.
   *
   * @return whether this object is condensed.
   */
  public boolean isCondensed() { return condensed; }

  /**
   * Sets whether this object should be "condensed," i.e. whether it should be displayed entirely on
   * one line.
   *
   * @param value
   *           whether this object should be condensed.
   * @return the object itself, to enable method chaining
   */
  public JsonObject setCondensed(boolean value) { condensed=value; return this; }

  /**
   * Generates a list of paths that have not yet been accessed in-code.
   * @return the list of unused paths.
   */
  public List<String> getUnusedPaths() {
    List<String> paths=new ArrayList<String>();
    for (Member m : this) {
      if (!m.value.isAccessed()) {
        paths.add(m.name);
      } //else {paths.add("*"+m.name);}
      if (m.value.isObject()) {
        for (String s : m.value.asObject().getUnusedPaths()) {
          paths.add(m.name+"."+s);
        }
      } else if (m.value.isArray()) {
        for (String s : m.value.asArray().getUnusedPaths()) {
          paths.add(m.name+s);
        }
      }
    }
    return paths;
  }

  /**
   * Sorts all members of this object according to their keys, in alphabetical order.
   *
   * @return the object itself, to enable chaining.
   */
  public JsonObject sort() {
    // Collect all members into an array.
    List<Member> members=new ArrayList<Member>();
    for (Member m : this) {
      members.add(m);
    }
    // Get the underlying array so it can be sorted.
    Member[] membersArray=members.toArray(new Member[members.size()]);

    // Sort the new array.
    Arrays.sort(membersArray, new MemberComparator());

    // Clear the original values.
    names.clear();
    values.clear();

    // Re-add the values, now in order.
    for (Member m : membersArray) {
      add(m.name, m.value);
    }

    return this;
  }

  /**
   * Returns an iterator over the members of this object in document order. The returned iterator
   * cannot be used to modify this object.
   *
   * @return an iterator over the members of this object
   */
  public Iterator<Member> iterator() {
    final Iterator<String> namesIterator=names.iterator();
    final Iterator<JsonValue> valuesIterator=values.iterator();
    return new Iterator<JsonObject.Member>() {

      public boolean hasNext() {
        return namesIterator.hasNext();
      }

      public Member next() {
        String name=namesIterator.next();
        JsonValue value=valuesIterator.next();
        return new Member(name, value);
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }

    };
  }

  @Override
  public JsonType getType() {
    return JsonType.OBJECT;
  }

  @Override
  public boolean isObject() {
    return true;
  }

  @Override
  public JsonObject asObject() {
    return this;
  }

  @Override
  public int hashCode() {
    int result=1;
    result=31 * result+names.hashCode();
    result=31 * result+values.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this==obj) {
      return true;
    }
    if (obj==null) {
      return false;
    }
    if (getClass()!=obj.getClass()) {
      return false;
    }
    JsonObject other=(JsonObject)obj;
    return names.equals(other.names) && values.equals(other.values);
  }

  int indexOf(String name) {
    int index=table.get(name);
    if (index!=-1 && name.equals(names.get(index))) {
      return index;
    }
    return names.lastIndexOf(name);
  }

  private synchronized void readObject(ObjectInputStream inputStream) throws IOException,
      ClassNotFoundException
  {
    inputStream.defaultReadObject();
    table=new HashIndexTable();
    updateHashIndex();
  }

  private void updateHashIndex() {
    int size=names.size();
    for (int i=0; i < size; i++) {
      table.add(names.get(i), i);
    }
  }

  /**
   * Represents a member of a JSON object, a pair of a name and a value.
   */
  public static class Member {

    private final String name;
    private final JsonValue value;

    Member(String name, JsonValue value) {
      this.name=name;
      this.value=value;
    }

    /**
     * Returns the name of this member.
     *
     * @return the name of this member, never <code>null</code>
     */
    public String getName() {
      return name;
    }

    /**
     * Returns the value of this member.
     *
     * @return the value of this member, never <code>null</code>
     */
    public JsonValue getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      int result=1;
      result=31 * result+name.hashCode();
      result=31 * result+value.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this==obj) {
        return true;
      }
      if (obj==null) {
        return false;
      }
      if (getClass()!=obj.getClass()) {
        return false;
      }
      Member other=(Member)obj;
      return name.equals(other.name) && value.equals(other.value);
    }

  }

  static class HashIndexTable {

    private final byte[] hashTable=new byte[32]; // must be a power of two

    public HashIndexTable() {
    }

    public HashIndexTable(HashIndexTable original) {
      System.arraycopy(original.hashTable, 0, hashTable, 0, hashTable.length);
    }

    void add(String name, int index) {
      int slot=hashSlotfor (name);
      if (index < 0xff) {
        // increment by 1, 0 stands for empty
        hashTable[slot]=(byte)(index+1);
      } else {
        hashTable[slot]=0;
      }
    }

    void remove(int index) {
      for (int i=0; i < hashTable.length; i++) {
        if (hashTable[i]==index+1) {
          hashTable[i]=0;
        } else if (hashTable[i]>index+1) {
          hashTable[i]--;
        }
      }
    }

    int get(Object name) {
      int slot=hashSlotfor (name);
      // subtract 1, 0 stands for empty
      return (hashTable[slot] & 0xff)-1;
    }

    private int hashSlotfor (Object element) {
      return element.hashCode() & hashTable.length-1;
    }
  }

  public static class MemberComparator implements Comparator<Member> {
    @Override
    public int compare(Member m1, Member m2) {
      return m1.name.compareToIgnoreCase(m2.name);
    }
  }
}
