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

import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.Until;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class selects which fields and types to omit. It is configurable, supporting version
 * attributes {@link Since} and {@link Until}, modifiers, synthetic fields, anonymous and local
 * classes, inner classes, and fields with the {@link Expose} annotation.
 *
 * <p>This class is a type adapter factory; types that are excluded will be adapted to null. It may
 * delegate to another type adapter if only one direction is excluded.
 *
 * <p>The exclusion decision logic itself is delegated to {@link ExclusionRules}, which encapsulates
 * the filtering rules independently from the TypeAdapterFactory plumbing.
 *
 * @author Joel Leitch
 * @author Jesse Wilson
 */
public final class Excluder implements TypeAdapterFactory, Cloneable {
  private static final double IGNORE_VERSIONS = -1.0d;
  public static final Excluder DEFAULT = new Excluder();

  private double version = IGNORE_VERSIONS;
  private int modifiers = Modifier.TRANSIENT | Modifier.STATIC;
  private boolean serializeInnerClasses = true;
  private boolean requireExpose;
  private List<ExclusionStrategy> serializationStrategies = Collections.emptyList();
  private List<ExclusionStrategy> deserializationStrategies = Collections.emptyList();

  /** Lazily created exclusion rules instance; invalidated on clone. */
  private transient ExclusionRules exclusionRules;

  @Override
  protected Excluder clone() {
    try {
      Excluder result = (Excluder) super.clone();
      result.exclusionRules = null; // Invalidate cached rules
      return result;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Returns the {@link ExclusionRules} instance containing the exclusion decision logic. The
   * instance is lazily created and cached.
   */
  private ExclusionRules getExclusionRules() {
    ExclusionRules rules = this.exclusionRules;
    if (rules == null) {
      rules =
          new ExclusionRules(
              version,
              modifiers,
              serializeInnerClasses,
              requireExpose,
              serializationStrategies,
              deserializationStrategies);
      this.exclusionRules = rules;
    }
    return rules;
  }

  public Excluder withVersion(double ignoreVersionsAfter) {
    Excluder result = clone();
    result.version = ignoreVersionsAfter;
    return result;
  }

  public Excluder withModifiers(int... modifiers) {
    Excluder result = clone();
    result.modifiers = 0;
    for (int modifier : modifiers) {
      result.modifiers |= modifier;
    }
    return result;
  }

  public Excluder disableInnerClassSerialization() {
    Excluder result = clone();
    result.serializeInnerClasses = false;
    return result;
  }

  public Excluder excludeFieldsWithoutExposeAnnotation() {
    Excluder result = clone();
    result.requireExpose = true;
    return result;
  }

  public Excluder withExclusionStrategy(
      ExclusionStrategy exclusionStrategy, boolean serialization, boolean deserialization) {
    Excluder result = clone();
    if (serialization) {
      result.serializationStrategies = new ArrayList<>(serializationStrategies);
      result.serializationStrategies.add(exclusionStrategy);
    }
    if (deserialization) {
      result.deserializationStrategies = new ArrayList<>(deserializationStrategies);
      result.deserializationStrategies.add(exclusionStrategy);
    }
    return result;
  }

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    Class<?> rawType = type.getRawType();

    boolean skipSerialize = excludeClass(rawType, true);
    boolean skipDeserialize = excludeClass(rawType, false);

    if (!skipSerialize && !skipDeserialize) {
      return null;
    }

    return new TypeAdapter<T>() {
      /**
       * The delegate is lazily created because it may not be needed, and creating it may fail.
       * Field has to be {@code volatile} because {@link Gson} guarantees to be thread-safe.
       */
      private volatile TypeAdapter<T> delegate;

      @Override
      public T read(JsonReader in) throws IOException {
        if (skipDeserialize) {
          in.skipValue();
          return null;
        }
        return delegate().read(in);
      }

      @Override
      public void write(JsonWriter out, T value) throws IOException {
        if (skipSerialize) {
          out.nullValue();
          return;
        }
        delegate().write(out, value);
      }

      private TypeAdapter<T> delegate() {
        // A race might lead to `delegate` being assigned by multiple threads but the last
        // assignment will stick
        TypeAdapter<T> d = delegate;
        if (d == null) {
          d = delegate = gson.getDelegateAdapter(Excluder.this, type);
        }
        return d;
      }
    };
  }

  public boolean excludeField(Field field, boolean serialize) {
    return getExclusionRules().shouldExcludeField(field, serialize);
  }

  // public for unit tests; can otherwise be private
  public boolean excludeClass(Class<?> clazz, boolean serialize) {
    return getExclusionRules().shouldExcludeClass(clazz, serialize);
  }

}
