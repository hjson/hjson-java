/*******************************************************************************
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

/**
 * Defines options for Hjson
 */
public class HjsonOptions {

  private IHjsonDsfProvider[] dsf;
  private boolean legacyRoot;
  private boolean nlBraces;
  private boolean allowCompact;
  private boolean allowMultiVal;
  private int indent, commentIndent;

  public HjsonOptions() {
    dsf=new IHjsonDsfProvider[0];
    legacyRoot=true;
    nlBraces=false;
    allowCompact=true;
    allowMultiVal=true;
    indent=2;
    commentIndent=0;
  }

  /**
   * Returns the DSF providers.
   *
   * @return providers.
   */
  public IHjsonDsfProvider[] getDsfProviders() { return dsf.clone(); }

  /**
   * Sets the DSF providers.
   *
   * @param value value
   */
  public HjsonOptions setDsfProviders(IHjsonDsfProvider... value) { dsf=value.clone(); return this; }

  /**
   * Detects whether objects without root braces are supported.
   *
   * @return <code>true</code> if this feature is enabled.
   */
  public boolean getParseLegacyRoot() { return legacyRoot; }

  /**
   * Sets whether root braces should be emitted.
   *
   * @param value value
   */
  public HjsonOptions setParseLegacyRoot(boolean value) { legacyRoot=value; return this; }

  /**
   * Detects whether root braces should be emitted.
   *
   * @deprecated will always return true.
   * @return <code>true</code> if this feature is enabled.
   */
  @Deprecated
  public boolean getEmitRootBraces() { return true; }

  /**
   * Sets whether root braces should be emitted.
   *
   * @deprecated root braces are always emitted.
   * @param value value
   */
  @Deprecated
  public HjsonOptions setEmitRootBraces(boolean value) { return this; }

  /**
   * Detects whether braces and brackets should be placed on new lines.
   *
   * @return whether braces and brackets follow the K&R / Java syntax.
   */
  public boolean useNlBraces() { return nlBraces; }

  /**
   * Sets whether braces and brackets should be placed on new lines.
   *
   * @param value value
   */
  public HjsonOptions setNlBraces(boolean value) { nlBraces=value; return this; }

  /**
   * Detects whether more than one value is ever allowed on a single line.
   *
   * @return <code>true</code> if more than one value is allowed.
   */
  public boolean allowMultiVal() { return allowMultiVal; }

  /**
   * Sets whether more than one value is ever allowed to be placed on a single line.
   *
   * @param value value
   */
  public HjsonOptions setAllowMultiVal(boolean value) { allowMultiVal=value; return this; }

  /**
   * Detects whether objects an arrays are allowed to be displayed on a single line.
   *
   * @return <code>true</code> if objects and arrays can be displayed on a single line.
   */
  public boolean allowCompact() { return allowCompact; }

  /**
   * Sets whether objects and arrays can be displayed on a single line.
   *
   * @param value value
   */
  public HjsonOptions setAllowCompact(boolean value) { allowCompact=value; return this; }

  /**
   * Gets the number of spaces to be placed per-level on each new line.
   *
   * @return the number of spaces.
   */
  public int getIndent() { return indent; }

  /**
   * Sets the number of spaces to be placed per-level on each new line.
   *
   * @param value value
   */
  public HjsonOptions setIndent(int value) { indent=value; return this; }

  /**
   * Gets the number of spaces to be placed before comments on new lines.
   *
   * @return the number of spaces.
   */
  public int getCommentIndent() { return commentIndent; }

  /**
   * Sets the number of spaces to be placed before comments on new lines.
   *
   * @param value value
   */
  public HjsonOptions setCommentIndent(int value) { commentIndent=value; return this; }

}
