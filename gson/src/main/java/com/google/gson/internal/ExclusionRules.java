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
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.Until;
import com.google.gson.internal.reflect.ReflectionHelper;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Encapsulates the rules that determine whether a field or class should be excluded from JSON
 * serialization/deserialization.
 *
 * <p>This class was extracted from {@link Excluder} to separate the <em>decision logic</em> (should
 * this field/class be excluded?) from the <em>TypeAdapterFactory</em> behavior (what happens when a
 * type is excluded?). This follows the <b>Single Responsibility Principle</b>: the rules engine is
 * independent of the adapter factory plumbing.
 *
 * <p>The class is immutable after construction. The evaluation methods are stateless and
 * thread-safe.
 *
 * @since 2.12
 */
public final class ExclusionRules {

  private static final double IGNORE_VERSIONS = -1.0d;

  private final double version;
  private final int modifiers;
  private final boolean serializeInnerClasses;
  private final boolean requireExpose;
  private final List<ExclusionStrategy> serializationStrategies;
  private final List<ExclusionStrategy> deserializationStrategies;

  /**
   * Creates a new ExclusionRules instance.
   *
   * @param version the version threshold for {@link Since}/{@link Until} annotations, or {@code
   *     -1.0} to ignore version filters
   * @param modifiers the field modifier bitmask to exclude (e.g., {@code Modifier.TRANSIENT |
   *     Modifier.STATIC})
   * @param serializeInnerClasses whether to allow serialization of inner (non-static member)
   *     classes
   * @param requireExpose whether fields without {@link Expose} annotation should be excluded
   * @param serializationStrategies custom exclusion strategies for serialization
   * @param deserializationStrategies custom exclusion strategies for deserialization
   */
  public ExclusionRules(
      double version,
      int modifiers,
      boolean serializeInnerClasses,
      boolean requireExpose,
      List<ExclusionStrategy> serializationStrategies,
      List<ExclusionStrategy> deserializationStrategies) {
    this.version = version;
    this.modifiers = modifiers;
    this.serializeInnerClasses = serializeInnerClasses;
    this.requireExpose = requireExpose;
    this.serializationStrategies = serializationStrategies;
    this.deserializationStrategies = deserializationStrategies;
  }

  /**
   * Determines whether the given field should be excluded from serialization or deserialization.
   *
   * @param field the field to evaluate
   * @param serialize {@code true} for serialization exclusion check, {@code false} for
   *     deserialization
   * @return {@code true} if the field should be excluded
   */
  public boolean shouldExcludeField(Field field, boolean serialize) {
    if ((modifiers & field.getModifiers()) != 0) {
      return true;
    }

    if (version != IGNORE_VERSIONS
        && !isValidVersion(field.getAnnotation(Since.class), field.getAnnotation(Until.class))) {
      return true;
    }

    if (field.isSynthetic()) {
      return true;
    }

    if (requireExpose) {
      Expose annotation = field.getAnnotation(Expose.class);
      if (annotation == null || (serialize ? !annotation.serialize() : !annotation.deserialize())) {
        return true;
      }
    }

    if (shouldExcludeClass(field.getType(), serialize)) {
      return true;
    }

    List<ExclusionStrategy> list = serialize ? serializationStrategies : deserializationStrategies;
    if (!list.isEmpty()) {
      FieldAttributes fieldAttributes = new FieldAttributes(field);
      for (ExclusionStrategy exclusionStrategy : list) {
        if (exclusionStrategy.shouldSkipField(fieldAttributes)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Determines whether the given class should be excluded from serialization or deserialization.
   *
   * @param clazz the class to evaluate
   * @param serialize {@code true} for serialization exclusion check, {@code false} for
   *     deserialization
   * @return {@code true} if the class should be excluded
   */
  public boolean shouldExcludeClass(Class<?> clazz, boolean serialize) {
    if (version != IGNORE_VERSIONS
        && !isValidVersion(clazz.getAnnotation(Since.class), clazz.getAnnotation(Until.class))) {
      return true;
    }

    if (!serializeInnerClasses && isInnerClass(clazz)) {
      return true;
    }

    if (!serialize
        && !Enum.class.isAssignableFrom(clazz)
        && ReflectionHelper.isAnonymousOrNonStaticLocal(clazz)) {
      return true;
    }

    List<ExclusionStrategy> list = serialize ? serializationStrategies : deserializationStrategies;
    for (ExclusionStrategy exclusionStrategy : list) {
      if (exclusionStrategy.shouldSkipClass(clazz)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isInnerClass(Class<?> clazz) {
    return clazz.isMemberClass() && !ReflectionHelper.isStatic(clazz);
  }

  private boolean isValidVersion(Since since, Until until) {
    return isValidSince(since) && isValidUntil(until);
  }

  private boolean isValidSince(Since annotation) {
    if (annotation != null) {
      double annotationVersion = annotation.value();
      return version >= annotationVersion;
    }
    return true;
  }

  private boolean isValidUntil(Until annotation) {
    if (annotation != null) {
      double annotationVersion = annotation.value();
      return version < annotationVersion;
    }
    return true;
  }
}
