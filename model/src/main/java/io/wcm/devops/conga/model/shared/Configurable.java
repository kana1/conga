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

import java.util.Map;

/**
 * Configurable definition.
 */
public interface Configurable {

  /**
   * Defines a map of configuration parameters.
   * They are merged with the configuration parameters from the configuration inheritance tree.
   * @return Configuration parameter map
   */
  Map<String, Object> getConfig();

  /**
   * @param config Configuration parameter map
   */
  void setConfig(Map<String, Object> config);

}
