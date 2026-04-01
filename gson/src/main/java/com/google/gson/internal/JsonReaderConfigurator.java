/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson.internal;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;

/**
 * Encapsulates the save-configure-restore pattern for {@link JsonReader} settings.
 *
 * <p>This class eliminates the duplicated strictness configuration logic that was previously
 * present in the {@code Gson} class across multiple {@code fromJson} method overloads.
 *
 * <p>Usage follows the try-finally pattern:
 *
 * <pre>{@code
 * JsonReaderConfigurator configurator = new JsonReaderConfigurator(reader, strictness);
 * try {
 *     return typeAdapter.read(reader);
 * } finally {
 *     configurator.restore();
 * }
 * }</pre>
 *
 * @since 2.12
 */
public final class JsonReaderConfigurator {

  private final JsonReader reader;
  private final Strictness oldStrictness;

  /**
   * Saves the current strictness of the reader and applies the given Gson strictness setting.
   *
   * @param reader the JsonReader to configure
   * @param gsonStrictness the Gson-level strictness setting (may be {@code null} for default)
   */
  public JsonReaderConfigurator(JsonReader reader, Strictness gsonStrictness) {
    this.reader = reader;
    this.oldStrictness = reader.getStrictness();

    if (gsonStrictness != null) {
      reader.setStrictness(gsonStrictness);
    } else if (reader.getStrictness() == Strictness.LEGACY_STRICT) {
      // For backward compatibility change to LENIENT if reader has default strictness
      // LEGACY_STRICT
      reader.setStrictness(Strictness.LENIENT);
    }
  }

  /**
   * Restores the reader to its original strictness.
   */
  public void restore() {
    reader.setStrictness(oldStrictness);
  }
}
