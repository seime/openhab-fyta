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
package no.seime.openhab.binding.fyta.internal.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.lang.reflect.Type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.reflect.TypeToken;

/**
 * Testing class for serialization and deserialization of http json playloads
 * 
 * @author Arne Seime - Initial contribution
 */
class SerializationDeserializationTest {

    protected WireHelper wireHelper = new WireHelper();

    @Test
    void testLoginResponse() throws IOException {
        final Type type = new TypeToken<LoginResponse>() {
        }.getType();

        final LoginResponse message = wireHelper.deSerializeFromClasspathResource("/mock_responses/login_response.json",
                type);

        assertEquals("XXXXAccessToken", message.accessToken);
        assertEquals(5184000, message.expiresIn);
        assertEquals("XXXXRefreshToken", message.refreshToken);
    }

    @Test
    void testGetPlantsResponse() throws IOException {
        final Type type = new TypeToken<GetPlantsResponse>() {
        }.getType();

        final GetPlantsResponse message = wireHelper
                .deSerializeFromClasspathResource("/mock_responses/get_plants_response.json", type);

        assertEquals(2, message.plants.length);

        Plant plant = message.plants[0];

        assertEquals(10000, plant.id);
        assertEquals("Fikentre sittegruppe", plant.nickname);
        assertEquals("Ficus benjamina", plant.scientificName);
        assertEquals("Weeping Fig", plant.commonName);
        assertEquals(3, plant.overallStatus);
        assertEquals("https://api.prod.fyta-app.de/user-plant/10000/thumb_path?timestamp=2023-04-09%2016%3A17%3A12",
                plant.userThumbnailPath);
        assertEquals("https://api.prod.fyta-app.de/user-plant/10000/origin_path?timestamp=2023-04-09%2016%3A17%3A12",
                plant.userOriginalPath);
        Assertions.assertEquals(Status.PERFECT, plant.temperatureStatus);
        Assertions.assertEquals(Status.TOO_LOW, plant.lightStatus);
        Assertions.assertEquals(Status.HIGH, plant.moistureStatus);
        Assertions.assertEquals(Status.TOO_LOW, plant.salinityStatus);
        assertEquals("2023-10-21T14:30:27Z", plant.receivedDataAt.toString());

        assertNotNull(plant.sensor);
        assertEquals("PLANT1_MAC_ADDRESS", plant.sensor.macAddress);
        assertEquals("1.4.0", plant.sensor.firmwareVersion);
        assertEquals("2023-10-21T14:30:27Z", plant.sensor.receivedDataAt.toString());

        // assertEquals(1,plant.temperatureUnit);
    }

    @Test
    void testGetPlantDetailsResponse() throws IOException {
        final Type type = new TypeToken<GetPlantDetailsResponse>() {
        }.getType();

        final GetPlantDetailsResponse message = wireHelper
                .deSerializeFromClasspathResource("/mock_responses/get_plant_details_response.json", type);

        assertNotNull(message.plant);
        Plant plant = message.plant;

        assertEquals(1, plant.temperatureUnit);
        Measurements measurements = plant.measurements;
        assertNotNull(measurements);

        assertNotNull(measurements.light);
        assertNotNull(measurements.temperature);
        assertEquals(10d, measurements.temperature.values.minAcceptable);
        assertEquals(17d, measurements.temperature.values.minGood);
        assertEquals(36d, measurements.temperature.values.maxGood);
        assertEquals(42d, measurements.temperature.values.maxAcceptable);
        assertEquals(18d, measurements.temperature.values.current);
        assertNotNull(measurements.moisture);
        assertNotNull(measurements.salinity);
        assertEquals(100, measurements.battery);

        assertEquals(Status.TOO_LOW, measurements.nutrients.status);

        Measurement light = measurements.light;
        assertEquals(0d, light.values.current);
        assertEquals("μmol/h", light.unit);
    }
}
