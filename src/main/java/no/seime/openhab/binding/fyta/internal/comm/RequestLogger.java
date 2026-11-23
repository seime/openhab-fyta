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
package no.seime.openhab.binding.fyta.internal.comm;

import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Logs HttpClient request/response traffic at debug level.
 *
 * @author Gili Tzabari - Initial contribution https://stackoverflow.com/users/14731/gili
 *         https://stackoverflow.com/questions/50318736/how-to-log-httpclient-requests-response-including-body
 * @author Arne Seime - adapted for openHAB binding
 */
@NonNullByDefault
public final class RequestLogger {
    private static final String[] HIDDEN_HEADERS = { "authorization" };
    private static final String[] HIDDEN_JSON_FIELDS = { "password", "access_token", "refresh_token" };

    private final Logger logger = LoggerFactory.getLogger(RequestLogger.class);
    private final AtomicLong nextId = new AtomicLong();
    private final Gson gson;
    private final String prefix;

    public RequestLogger(final String prefix, final Gson gson) {
        this.gson = gson;
        this.prefix = prefix;
    }

    /**
     * Log an outgoing request.
     *
     * @return an id used to correlate the response log entry
     */
    public long logRequest(final HttpRequest request, @Nullable final String body) {
        final long idV = nextId.getAndIncrement();
        if (logger.isDebugEnabled()) {
            final String id = prefix + "-" + idV;
            final StringBuilder group = new StringBuilder();
            group.append(String.format("Request %s%n%s > %s %s%n", id, id, request.method(), request.uri()));
            appendHeaders(group, id, ">", request.headers());
            if (body != null && !body.isEmpty()) {
                group.append("\n");
                group.append(reformatJson(body));
            }
            logger.debug("{}", group);
        }
        return idV;
    }

    /**
     * Log an incoming response.
     */
    public void logResponse(final long idV, final HttpResponse<?> response, @Nullable final String body) {
        if (logger.isDebugEnabled()) {
            final String id = prefix + "-" + idV;
            final StringBuilder group = new StringBuilder();
            group.append(String.format("Response %s%n%s < %s %s%n", id, id, response.version(), response.statusCode()));
            appendHeaders(group, id, "<", response.headers());
            if (body != null && !body.isEmpty()) {
                group.append("\n");
                group.append(reformatJson(body));
            }
            logger.debug("{}", group);
        }
    }

    private void appendHeaders(final StringBuilder group, final String id, final String direction,
            final HttpHeaders headers) {
        for (Map.Entry<String, List<String>> header : headers.map().entrySet()) {
            for (String value : header.getValue()) {
                group.append(String.format("%s %s %s: %s%n", id, direction, header.getKey(),
                        isHidden(header.getKey()) ? "<HIDDEN>" : value));
            }
        }
    }

    private boolean isHidden(String headerName) {
        for (String hidden : HIDDEN_HEADERS) {
            if (hidden.equalsIgnoreCase(headerName)) {
                return true;
            }
        }
        return false;
    }

    private String reformatJson(final String jsonString) {
        try {
            final JsonElement json = JsonParser.parseString(jsonString);
            if (json.isJsonObject()) {
                for (String field : HIDDEN_JSON_FIELDS) {
                    if (json.getAsJsonObject().has(field)) {
                        json.getAsJsonObject().addProperty(field, "<HIDDEN>");
                    }
                }
            }
            return gson.toJson(json);
        } catch (final JsonSyntaxException e) {
            logger.debug("Could not reformat malformed JSON due to '{}'", e.getMessage());
            return jsonString;
        }
    }
}
