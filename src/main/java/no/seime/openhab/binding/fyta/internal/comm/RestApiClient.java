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

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.RawType;
import org.openhab.core.thing.ThingUID;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import no.seime.openhab.binding.fyta.internal.dto.AbstractRequest;

/**
 * The {@link RestApiClient} is responsible for API login and communication
 *
 * @author Arne Seime - Initial contribution
 */
public class RestApiClient {
    public static final String HEADER_CONTENT_TYPE_APPLICATION_JSON = "application/json";
    public static String API_ENDPOINT = "https://web.fyta.de/api";

    public static String IMAGE_ENDPOINT = null; // Only used for stubbing

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_REQUEST_TIMEOUT = 408;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private HttpClient httpClient;

    @Nullable
    private String accessToken = null;

    private Gson gson;

    private RequestLogger requestLogger;

    public RestApiClient(HttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.requestLogger = new RequestLogger("fyta", gson);
    }

    public void init(ThingUID bridgeUid) {
        this.requestLogger = new RequestLogger(bridgeUid.getId(), gson);
    }

    private HttpRequest.Builder newRequestBuilder(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "openHAB").header("Accept", HEADER_CONTENT_TYPE_APPLICATION_JSON);
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return builder;
    }

    public <T> T sendRequest(final AbstractRequest req, final Type responseType) throws RestCommunicationException {
        final String method = req.getMethod();
        final HttpRequest.Builder builder = newRequestBuilder(API_ENDPOINT + req.getRequestUrl());

        @Nullable
        String reqJson = null;
        if ("GET".equals(method)) {
            builder.GET();
        } else { // POST, PATCH, PUT
            reqJson = gson.toJson(req);
            builder.header("Content-Type", HEADER_CONTENT_TYPE_APPLICATION_JSON).method(method,
                    HttpRequest.BodyPublishers.ofString(reqJson, StandardCharsets.UTF_8));
        }
        final HttpRequest httpRequest = builder.build();

        final HttpResponse<String> response;
        final long id = requestLogger.logRequest(httpRequest, reqJson);
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RestCommunicationException(String.format("Error sending request to server: %s", e.getMessage()),
                    e);
        } catch (IOException e) {
            throw new RestCommunicationException(
                    String.format("Exception caught trying to communicate with API: %s", e.getMessage()), e);
        }
        requestLogger.logResponse(id, response, response.body());

        try {
            return handleResponse(response, req, responseType);
        } catch (RestCommunicationException e) {
            throw e;
        } catch (Exception e) {
            throw new RestCommunicationException(
                    String.format("Exception caught trying to communicate with API: %s", e.getMessage()), e);
        }
    }

    private <T> T handleResponse(final HttpResponse<String> response, final AbstractRequest req,
            final Type responseType) throws RestCommunicationException {
        final int status = response.statusCode();
        final String responseJson = response.body();
        if (status == HTTP_OK) {
            final JsonObject o = JsonParser.parseString(responseJson).getAsJsonObject();
            if (o.has("error")) {
                throw new RestCommunicationException(req, o.get("error").getAsString(), status);
            } else {
                final T result = gson.fromJson(responseJson, responseType);
                if (result == null) {
                    throw new RestCommunicationException(req, "Empty response from server", status);
                }
                return result;
            }
        } else if (status == HTTP_NOT_FOUND) {
            throw new RestCommunicationException(req, "User or plant not found, check configuration", status);
        } else if (status == HTTP_REQUEST_TIMEOUT) {
            throw new RestCommunicationException(req, "The operation timed out", status);
        } else if (status == HTTP_TOO_MANY_REQUESTS) {
            throw new RestCommunicationException(req, "Too many requests, reduce polling time", status);
        } else {
            throw new RestCommunicationException(req,
                    "Error sending request to server. Server responded with " + status + " and payload " + responseJson,
                    status);
        }
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public RawType getImage(String userThumbnailPath) throws RestCommunicationException {
        if (IMAGE_ENDPOINT != null) {
            userThumbnailPath = IMAGE_ENDPOINT + userThumbnailPath; // Only used for stubbing
        }
        try {
            HttpRequest request = newRequestBuilder(userThumbnailPath).GET().build();
            HttpResponse<byte[]> image = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (image.statusCode() != HTTP_OK) {
                throw new RestCommunicationException(String.format("Server responded with %d fetching image %s",
                        image.statusCode(), userThumbnailPath));
            }
            String contentType = image.headers().firstValue("Content-Type").orElse(RawType.DEFAULT_MIME_TYPE);
            return new RawType(image.body(), contentType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RestCommunicationException(
                    String.format("Exception caught trying fetch image: %s", e.getMessage()), e);
        } catch (IOException | IllegalArgumentException e) {
            throw new RestCommunicationException(
                    String.format("Exception caught trying fetch image: %s", e.getMessage()), e);
        }
    }
}
