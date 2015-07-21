/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.devops.conga.model.shared;

/**
 * Line endings for generated files.
 */
public enum LineEndings {

  /**
   * Unix line ending
   */
  unix("\n"),

  /**
   * Windows line ending
   */
  windows("\r\n"),

  /**
   * MacOS line ending (up to version 9)
   */
  macos("\r");

  private final String lineEnding;

  private LineEndings(String lineEnding) {
    this.lineEnding = lineEnding;
  }

  /**
   * @return Line ending characters
   */
  public String getLineEnding() {
    return this.lineEnding;
  }

}