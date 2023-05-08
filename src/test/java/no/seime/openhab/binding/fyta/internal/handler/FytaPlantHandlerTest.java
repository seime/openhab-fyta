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
package no.seime.openhab.binding.fyta.internal.handler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.RawType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.test.storage.VolatileStorage;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.internal.ThingImpl;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.gson.Gson;

import no.seime.openhab.binding.fyta.internal.BindingConstants;
import no.seime.openhab.binding.fyta.internal.GsonFactory;
import no.seime.openhab.binding.fyta.internal.PlantConfiguration;
import no.seime.openhab.binding.fyta.internal.comm.RestApiClient;
import no.seime.openhab.binding.fyta.internal.dto.GetPlantsResponse;
import no.seime.openhab.binding.fyta.internal.dto.Status;

/**
 *
 * @author Arne Seime - Initial contribution
 */

@ExtendWith(MockitoExtension.class)
class FytaPlantHandlerTest {

    private WireMockServer wireMockServer;

    private HttpClient httpClient;

    private @Mock Configuration configuration;
    private @Mock Bridge bridge;
    private @Mock FytaAccountHandler accountHandler;

    private RestApiClient restApiClient;
    private VolatileStorage<Object> storage;

    private Gson gson = GsonFactory.create();

    private Thing thing;

    private FytaPlantHandler plantHandler;

    private ThingHandlerCallback thingHandlerCallback;

    private PlantConfiguration plantConfiguration;

    @BeforeEach
    public void setUp() throws Exception {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        int port = wireMockServer.port();
        WireMock.configureFor("localhost", port);
        RestApiClient.API_ENDPOINT = "http://localhost:" + port + "/api";
        RestApiClient.IMAGE_ENDPOINT = "http://localhost:" + port;

        httpClient = new HttpClient();
        httpClient.start();

        restApiClient = new RestApiClient(httpClient, gson);
        restApiClient.init(new ThingUID("fyta:bridge:1"));
        restApiClient.setAccessToken("ACCESSTOKEN");

        plantConfiguration = new PlantConfiguration();
        plantConfiguration.macAddress = "MAC_ADDRESS1";
        when(configuration.as(PlantConfiguration.class)).thenReturn(plantConfiguration);

        thing = createThing();
        plantHandler = Mockito.spy(new FytaPlantHandler(thing, new VolatileStorage<>()));
        thingHandlerCallback = Mockito.mock(ThingHandlerCallback.class);
        plantHandler.setCallback(thingHandlerCallback);

        when(plantHandler.getBridge()).thenReturn(bridge);
        when(bridge.getHandler()).thenReturn(accountHandler);
        GetPlantsResponse getPlantsResponse = gson
                .fromJson(getClasspathJSONContent("/mock_responses/get_plants_response.json"), GetPlantsResponse.class);
        assertNotNull(getPlantsResponse);
        when(accountHandler.getModel()).thenReturn(getPlantsResponse);
        when(accountHandler.getRestApiClient()).thenReturn(restApiClient);

        storage = new VolatileStorage<>();
    }

    @AfterEach
    public void shutdown() throws Exception {
        httpClient.stop();
        plantHandler.dispose();
    }

    @Test
    void testInitialize() throws IOException, InterruptedException {

        // Setup get lock response
        prepareGetNetworkResponse("/api/user-plant/100000", "/mock_responses/get_plant_details_response.json", 200);
        prepareGetNetworkResponse("/user-plant/100000/thumb_path?timestamp=2023-04-09%2016%3A17%3A12",
                "image".getBytes(StandardCharsets.UTF_8), "image/png", 200);

        plantHandler.initialize();

        Thread.sleep(2000);

        // Temp
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_TEMPERATURE_STATUS),
                new StringType(Status.PERFECT.toString()));
        verify(thingHandlerCallback).stateUpdated(new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_TEMPERATURE),
                new QuantityType<>(21, SIUnits.CELSIUS));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_TEMPERATURE_MIN_ACCEPTABLE),
                new QuantityType<>(10, SIUnits.CELSIUS));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_TEMPERATURE_MIN_GOOD),
                new QuantityType<>(17, SIUnits.CELSIUS));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_TEMPERATURE_MAX_GOOD),
                new QuantityType<>(36, SIUnits.CELSIUS));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_TEMPERATURE_MAX_ACCEPTABLE),
                new QuantityType<>(42, SIUnits.CELSIUS));

        // Moisture
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_MOISTURE_STATUS),
                new StringType(Status.PERFECT.toString()));
        verify(thingHandlerCallback).stateUpdated(new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_MOISTURE),
                new QuantityType<>(52, Units.PERCENT));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_MOISTURE_MIN_ACCEPTABLE),
                new QuantityType<>(25, Units.PERCENT));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_MOISTURE_MIN_GOOD),
                new QuantityType<>(35, Units.PERCENT));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_MOISTURE_MAX_GOOD),
                new QuantityType<>(70, Units.PERCENT));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_MOISTURE_MAX_ACCEPTABLE),
                new QuantityType<>(80, Units.PERCENT));

        // Salinity
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_SALINITY_STATUS),
                new StringType(Status.TOO_LOW.toString()));
        verify(thingHandlerCallback).stateUpdated(new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_SALINITY),
                new DecimalType(0.32));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_SALINITY_MIN_ACCEPTABLE), new DecimalType(0.4));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_SALINITY_MIN_GOOD), new DecimalType(0.6));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_SALINITY_MAX_GOOD), new DecimalType(1));
        verify(thingHandlerCallback).stateUpdated(
                new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_SALINITY_MAX_ACCEPTABLE), new DecimalType(1.2));

        // Light
        verify(thingHandlerCallback).stateUpdated(new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_LIGHT_STATUS),
                new StringType(Status.LOW.toString()));
        verify(thingHandlerCallback).stateUpdated(new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_LIGHT),
                new DecimalType(1));

        // Other
        verify(thingHandlerCallback).stateUpdated(new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_NICKNAME),
                new StringType("Ficus benjamina"));
        verify(thingHandlerCallback).stateUpdated(new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_THUMBNAIL),
                new RawType("image".getBytes(StandardCharsets.UTF_8), "image/png"));
        verify(thingHandlerCallback).stateUpdated(new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_BATTERY),
                new QuantityType<>(100, Units.PERCENT));
        verify(thingHandlerCallback).stateUpdated(new ChannelUID(thing.getUID(), BindingConstants.CHANNEL_LAST_UPDATED),
                new DateTimeType(ZonedDateTime.parse("2023-05-06T18:16:47Z")));
    }

    private Thing createThing() {

        ThingImpl plantThing = new ThingImpl(BindingConstants.THING_TYPE_PLANT, "100000");
        plantThing.addChannel(
                ChannelBuilder.create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_NICKNAME)).build());
        plantThing.addChannel(
                ChannelBuilder.create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_THUMBNAIL)).build());
        plantThing.addChannel(
                ChannelBuilder.create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_BATTERY)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_LAST_UPDATED)).build());

        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_TEMPERATURE_STATUS)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_TEMPERATURE)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_TEMPERATURE_MIN_ACCEPTABLE))
                .build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_TEMPERATURE_MIN_GOOD)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_TEMPERATURE_MAX_GOOD)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_TEMPERATURE_MAX_ACCEPTABLE))
                .build());

        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_LIGHT_STATUS)).build());
        plantThing.addChannel(
                ChannelBuilder.create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_LIGHT)).build());

        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_MOISTURE_STATUS)).build());
        plantThing.addChannel(
                ChannelBuilder.create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_MOISTURE)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_MOISTURE_MIN_ACCEPTABLE)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_MOISTURE_MIN_GOOD)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_MOISTURE_MAX_GOOD)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_MOISTURE_MAX_ACCEPTABLE)).build());

        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_SALINITY_STATUS)).build());
        plantThing.addChannel(
                ChannelBuilder.create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_SALINITY)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_SALINITY_MIN_ACCEPTABLE)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_SALINITY_MIN_GOOD)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_SALINITY_MAX_GOOD)).build());
        plantThing.addChannel(ChannelBuilder
                .create(new ChannelUID(plantThing.getUID(), BindingConstants.CHANNEL_SALINITY_MAX_ACCEPTABLE)).build());

        plantThing.setConfiguration(configuration);
        return plantThing;
    }

    private void prepareGetNetworkResponse(String urlPath, String responseResource, int responseCode)
            throws IOException {
        stubFor(get(urlEqualTo(urlPath))
                .willReturn(aResponse().withStatus(responseCode).withBody(getClasspathJSONContent(responseResource))));
    }

    private void prepareGetNetworkResponse(String urlPath, byte[] responseContent, String contentType, int responseCode)
            throws IOException {
        stubFor(get(urlEqualTo(urlPath)).willReturn(aResponse().withStatus(responseCode)
                .withHeader(HttpHeader.CONTENT_TYPE.asString(), contentType).withBody(responseContent)));
    }

    private String getClasspathJSONContent(String path) throws IOException {
        return new String(getClass().getResourceAsStream(path).readAllBytes(), StandardCharsets.UTF_8);
    }
}
