/*
 * Copyright (C) 2011 Google Inc.
 *
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
 */

package com.google.gson.internal.bind;

import static com.google.gson.internal.bind.TypeAdapters.newFactory;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.internal.NumberLimits;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Type adapters for advanced numeric types: {@link BigDecimal}, {@link BigInteger}, and {@link
 * LazilyParsedNumber}.
 *
 * <p>These adapters were extracted from the monolithic {@link TypeAdapters} class to improve
 * cohesion. Each adapter in this class handles types related to arbitrary-precision or lazily-parsed
 * numeric values, forming a logical group separate from primitive number types.
 *
 * @since 2.12
 */
public final class NumericTypeAdapters {
  private NumericTypeAdapters() {
    throw new UnsupportedOperationException();
  }

  public static final TypeAdapter<BigDecimal> BIG_DECIMAL =
      new TypeAdapter<BigDecimal>() {
        @Override
        public BigDecimal read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          String s = in.nextString();
          try {
            return NumberLimits.parseBigDecimal(s);
          } catch (NumberFormatException e) {
            throw new JsonSyntaxException(
                "Failed parsing '" + s + "' as BigDecimal; at path " + in.getPreviousPath(), e);
          }
        }

        @Override
        public void write(JsonWriter out, BigDecimal value) throws IOException {
          out.value(value);
        }
      };

  public static final TypeAdapterFactory BIG_DECIMAL_FACTORY =
      newFactory(BigDecimal.class, BIG_DECIMAL);

  public static final TypeAdapter<BigInteger> BIG_INTEGER =
      new TypeAdapter<BigInteger>() {
        @Override
        public BigInteger read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          String s = in.nextString();
          try {
            return NumberLimits.parseBigInteger(s);
          } catch (NumberFormatException e) {
            throw new JsonSyntaxException(
                "Failed parsing '" + s + "' as BigInteger; at path " + in.getPreviousPath(), e);
          }
        }

        @Override
        public void write(JsonWriter out, BigInteger value) throws IOException {
          out.value(value);
        }
      };

  public static final TypeAdapterFactory BIG_INTEGER_FACTORY =
      newFactory(BigInteger.class, BIG_INTEGER);

  public static final TypeAdapter<LazilyParsedNumber> LAZILY_PARSED_NUMBER =
      new TypeAdapter<LazilyParsedNumber>() {
        // Normally users should not be able to access and deserialize LazilyParsedNumber because
        // it is an internal type, but implement this nonetheless in case there are legit corner
        // cases where this is possible
        @Override
        public LazilyParsedNumber read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          return new LazilyParsedNumber(in.nextString());
        }

        @Override
        public void write(JsonWriter out, LazilyParsedNumber value) throws IOException {
          out.value(value);
        }
      };

  public static final TypeAdapterFactory LAZILY_PARSED_NUMBER_FACTORY =
      newFactory(LazilyParsedNumber.class, LAZILY_PARSED_NUMBER);
}
