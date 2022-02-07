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

@SuppressWarnings("serial") // use default serial UID
class JsonString extends JsonValue {

  private final String string;

  JsonString(String string) {
    if (string==null) {
      throw new NullPointerException("string is null");
    }
    this.string=string;
  }

  @Override
  public JsonType getType() {
    return JsonType.STRING;
  }

  @Override
  public boolean isString() {
    return true;
  }

  @Override
  public String asString() {
    return string;
  }

  @Override
  public JsonValue deepCopy(boolean trackAccess) {
    JsonValue clone=new JsonString(string).copyComments(this);
    return trackAccess ? clone.setAccessed(accessed) : clone;
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 59 + string.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (this==object) {
      return true;
    }
    if (object instanceof JsonString) {
      JsonString other=(JsonString)object;
      return string.equals(other.string) && commentsMatch(other);
    }
    return false;
  }
}
