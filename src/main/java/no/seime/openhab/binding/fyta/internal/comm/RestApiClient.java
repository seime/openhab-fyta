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

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.i18n.ConfigurationException;
import org.openhab.core.library.types.RawType;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Logger logger = LoggerFactory.getLogger(RestApiClient.class);
    private final HttpClient httpClient;
    private final Gson gson;
    @Nullable
    private String accessToken = null;
    @Nullable
    private RequestLogger requestLogger = null;

    public RestApiClient(HttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public void init(ThingUID bridgeUid) {
        this.requestLogger = new RequestLogger(bridgeUid.getId(), gson);
    }

    private Request buildRequest(final AbstractRequest req) {
        Request request = httpClient.newRequest(API_ENDPOINT + req.getRequestUrl()).method(req.getMethod());

        addRequestHeaders(request);

        if (!req.getMethod().contentEquals(HttpMethod.GET.asString())) { // POST, PATCH, PUT
            final String reqJson = gson.toJson(req);
            request = request.content(new BytesContentProvider(reqJson.getBytes(StandardCharsets.UTF_8)),
                    HEADER_CONTENT_TYPE_APPLICATION_JSON);
        }

        requestLogger.listenTo(request, new String[] {});

        return request;
    }

    private void addRequestHeaders(Request request) {
        request.getHeaders().remove(HttpHeader.USER_AGENT);
        request.getHeaders().remove(HttpHeader.ACCEPT);
        request.header(HttpHeader.USER_AGENT, "openHAB");
        request.header(HttpHeader.ACCEPT, HEADER_CONTENT_TYPE_APPLICATION_JSON);
        request.header(HttpHeader.CONTENT_TYPE, HEADER_CONTENT_TYPE_APPLICATION_JSON);
        if (accessToken != null) {
            request.header(HttpHeader.AUTHORIZATION, "Bearer " + accessToken);
        }
    }

    public <T> T sendRequest(final AbstractRequest req, final Type responseType) throws RestCommunicationException {

        try {

            return sendRequestInternal(buildRequest(req), req, responseType);
        } catch (InterruptedException | TimeoutException | ExecutionException | RestCommunicationException e) {
            Thread.currentThread().interrupt();
            throw new RestCommunicationException(String.format("Error sending request to server: %s", e.getMessage()),
                    e);
        }
    }

    public <T> T sendRequestInternal(final Request httpRequest, final AbstractRequest req, final Type responseType)
            throws ExecutionException, InterruptedException, TimeoutException, RestCommunicationException {

        try {
            final ContentResponse contentResponse = httpRequest.send();

            final String responseJson = contentResponse.getContentAsString();
            if (contentResponse.getStatus() == HttpStatus.OK_200) {
                final JsonObject o = JsonParser.parseString(responseJson).getAsJsonObject();
                if (o.has("error")) {
                    throw new RestCommunicationException(req, o.get("error").getAsString(),
                            contentResponse.getStatus());
                } else {
                    return gson.fromJson(responseJson, responseType);
                }
            } else if (contentResponse.getStatus() == HttpStatus.NOT_FOUND_404) {
                throw new ConfigurationException("User or plant not found, check configuration");
            } else if (contentResponse.getStatus() == HttpStatus.REQUEST_TIMEOUT_408) {
                throw new RestCommunicationException(req, "The operation timed out", contentResponse.getStatus());
            } else if (contentResponse.getStatus() == HttpStatus.TOO_MANY_REQUESTS_429) {
                throw new RestCommunicationException(req, "Too many requests, reduce polling time",
                        contentResponse.getStatus());
            } else {
                throw new RestCommunicationException(req, "Error sending request to server. Server responded with "
                        + contentResponse.getStatus() + " and payload " + responseJson, contentResponse.getStatus());
            }
        } catch (Exception e) {
            throw new RestCommunicationException(
                    String.format("Exception caught trying to communicate with API: %s", e.getMessage()), e);
        }
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public RawType getImage(String userThumbnailPath) throws RestCommunicationException {
        try {
            if (IMAGE_ENDPOINT != null) {
                userThumbnailPath = IMAGE_ENDPOINT + userThumbnailPath; // Only used for stubbing
            }

            Request request = httpClient.newRequest(userThumbnailPath).method(HttpMethod.GET);
            addRequestHeaders(request);
            ContentResponse image = request.send();
            String contentType = image.getHeaders().get(HttpHeader.CONTENT_TYPE);
            return new RawType(image.getContent(), contentType);

        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new RestCommunicationException(
                    String.format("Exception caught trying fetch image: %s", e.getMessage()), e);
        }
    }
}
