/**
 * Copyright (c) 2023 Contributors to the Seime Openhab Addons project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package no.seime.openhab.binding.fyta.internal;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Factory for creating a GSON instance with the appropriate datatype converters for api calls
 * 
 * @author Arne Seime - Initial contribution
 */

public class GsonFactory {

    public static Gson create() {
        return new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new UTCStringAdapter()).setPrettyPrinting()
                .create();
    }

    public static class UTCStringAdapter extends TypeAdapter<ZonedDateTime> {

        @Override
        public void write(final JsonWriter out, final ZonedDateTime value) throws IOException {
            if (value != null) {
                out.value(value.toString());
            }
        }

        @Override
        public ZonedDateTime read(final JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                in.nextNull();
                return null;
            } else {
                return DateTimeParser.parseUTCDateTime(in.nextString());

            }
        }
    }

    public static class DateTimeParser {
        public static ZonedDateTime parseUTCDateTime(String utcDateTimeString) {
            LocalDateTime localDateTime = LocalDateTime.parse(utcDateTimeString,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return localDateTime.atZone(ZoneOffset.UTC);
        }
    }
}
