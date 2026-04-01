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
import com.google.gson.stream.JsonWriter;

/**
 * Encapsulates the save-configure-restore pattern for {@link JsonWriter} settings.
 *
 * <p>This class eliminates the duplicated code that was previously present in the {@code Gson}
 * class, where the same strictness/htmlSafe/serializeNulls save-configure-restore logic was
 * copy-pasted across multiple {@code toJson} method overloads.
 *
 * <p>Usage follows the try-finally pattern:
 *
 * <pre>{@code
 * JsonWriterConfigurator configurator = new JsonWriterConfigurator(writer, strictness, htmlSafe, serializeNulls);
 * try {
 *     adapter.write(writer, src);
 * } finally {
 *     configurator.restore();
 * }
 * }</pre>
 *
 * @since 2.12
 */
public final class JsonWriterConfigurator {

  private final JsonWriter writer;
  private final Strictness oldStrictness;
  private final boolean oldHtmlSafe;
  private final boolean oldSerializeNulls;

  /**
   * Saves the current state of the writer and applies the given Gson settings.
   *
   * @param writer the JsonWriter to configure
   * @param gsonStrictness the Gson-level strictness setting (may be {@code null} for default)
   * @param htmlSafe whether to enable HTML-safe output
   * @param serializeNulls whether to serialize null values
   */
  public JsonWriterConfigurator(
      JsonWriter writer, Strictness gsonStrictness, boolean htmlSafe, boolean serializeNulls) {
    this.writer = writer;
    this.oldStrictness = writer.getStrictness();
    this.oldHtmlSafe = writer.isHtmlSafe();
    this.oldSerializeNulls = writer.getSerializeNulls();

    if (gsonStrictness != null) {
      writer.setStrictness(gsonStrictness);
    } else if (writer.getStrictness() == Strictness.LEGACY_STRICT) {
      // For backward compatibility change to LENIENT if writer has default strictness
      // LEGACY_STRICT
      writer.setStrictness(Strictness.LENIENT);
    }

    writer.setHtmlSafe(htmlSafe);
    writer.setSerializeNulls(serializeNulls);
  }

  /**
   * Restores the writer to its original configuration.
   */
  public void restore() {
    writer.setStrictness(oldStrictness);
    writer.setHtmlSafe(oldHtmlSafe);
    writer.setSerializeNulls(oldSerializeNulls);
  }
}
