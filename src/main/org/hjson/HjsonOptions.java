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

  public HjsonOptions() {
    dsf=new IHjsonDsfProvider[0];
    legacyRoot=true;
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
  public void setDsfProviders(IHjsonDsfProvider[] value) { dsf=value.clone(); }

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
  public void setParseLegacyRoot(boolean value) { legacyRoot=value; }

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
  public void setEmitRootBraces(boolean value) { }

}
