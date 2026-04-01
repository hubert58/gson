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
import static com.google.gson.internal.bind.TypeAdapters.newTypeHierarchyFactory;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

/**
 * Type adapters for network and identifier types: {@link URL}, {@link URI}, {@link InetAddress},
 * and {@link UUID}.
 *
 * <p>These adapters were extracted from the monolithic {@link TypeAdapters} class to improve
 * cohesion. Each adapter in this class handles a type related to network communication or unique
 * identification, forming a logical group.
 *
 * @since 2.12
 */
public final class NetworkTypeAdapters {
  private NetworkTypeAdapters() {
    throw new UnsupportedOperationException();
  }

  public static final TypeAdapter<URL> URL =
      new TypeAdapter<URL>() {
        @Override
        public URL read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          String nextString = in.nextString();
          return nextString.equals("null") ? null : new URL(nextString);
        }

        @Override
        public void write(JsonWriter out, URL value) throws IOException {
          out.value(value == null ? null : value.toExternalForm());
        }
      };

  public static final TypeAdapterFactory URL_FACTORY = newFactory(URL.class, URL);

  public static final TypeAdapter<URI> URI =
      new TypeAdapter<URI>() {
        @Override
        public URI read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          try {
            String nextString = in.nextString();
            return nextString.equals("null") ? null : new URI(nextString);
          } catch (URISyntaxException e) {
            throw new JsonIOException(e);
          }
        }

        @Override
        public void write(JsonWriter out, URI value) throws IOException {
          out.value(value == null ? null : value.toASCIIString());
        }
      };

  public static final TypeAdapterFactory URI_FACTORY = newFactory(URI.class, URI);

  public static final TypeAdapter<InetAddress> INET_ADDRESS =
      new TypeAdapter<InetAddress>() {
        @Override
        public InetAddress read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          // regrettably, this should have included both the host name and the host address
          // For compatibility, we use InetAddress.getByName rather than the possibly-better
          // .getAllByName
          @SuppressWarnings("AddressSelection")
          InetAddress addr = InetAddress.getByName(in.nextString());
          return addr;
        }

        @Override
        public void write(JsonWriter out, InetAddress value) throws IOException {
          out.value(value == null ? null : value.getHostAddress());
        }
      };

  public static final TypeAdapterFactory INET_ADDRESS_FACTORY =
      newTypeHierarchyFactory(InetAddress.class, INET_ADDRESS);

  public static final TypeAdapter<UUID> UUID =
      new TypeAdapter<UUID>() {
        @Override
        public UUID read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          String s = in.nextString();
          try {
            return java.util.UUID.fromString(s);
          } catch (IllegalArgumentException e) {
            throw new JsonSyntaxException(
                "Failed parsing '" + s + "' as UUID; at path " + in.getPreviousPath(), e);
          }
        }

        @Override
        public void write(JsonWriter out, UUID value) throws IOException {
          out.value(value == null ? null : value.toString());
        }
      };

  public static final TypeAdapterFactory UUID_FACTORY = newFactory(UUID.class, UUID);
}
