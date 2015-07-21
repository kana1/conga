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
package io.wcm.devops.conga.resource;

/**
 * Abstraction for resource information.
 */
public interface ResourceInfo extends Comparable<Resource> {

  /**
   * @return true if resource exists
   */
  boolean exists();

  /**
   * @return File name (without path)
   */
  String getName();

  /**
   * @return Full path for file
   */
  String getPath();

  /**
   * @return Canonical path for file that is absolute and unique.
   */
  String getCanonicalPath();

}
