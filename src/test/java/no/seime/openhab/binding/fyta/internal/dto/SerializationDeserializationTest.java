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

        assertEquals(3, message.plants.length);

        Plant plant = message.plants[0];

        assertEquals(100000, plant.id);
        assertEquals("Ficus benjamina", plant.nickname);
        assertEquals("Ficus benjamina", plant.scientificName);
        assertEquals("Weeping Fig", plant.commonName);
        assertEquals(2, plant.overallStatus);
        assertEquals("https://api.prod.fyta-app.de/user-plant/100000/thumb_path?timestamp=2023-04-09%2016%3A17%3A12",
                plant.userThumbnailPath);
        assertEquals("https://api.prod.fyta-app.de/user-plant/100000/origin_path?timestamp=2023-04-09%2016%3A17%3A12",
                plant.userOriginalPath);
        Assertions.assertEquals(Status.PERFECT, plant.temperatureStatus);
        Assertions.assertEquals(Status.LOW, plant.lightStatus);
        Assertions.assertEquals(Status.PERFECT, plant.moistureStatus);
        Assertions.assertEquals(Status.TOO_LOW, plant.salinityStatus);
        assertEquals("2023-05-06T17:05:56Z", plant.receivedDataAt.toString());

        assertNotNull(plant.sensor);
        assertEquals("MAC_ADDRESS1", plant.sensor.macAddress);
        assertEquals("1.1.0", plant.sensor.firmwareVersion);
        assertEquals("2023-05-06T17:05:56Z", plant.sensor.receivedDataAt.toString());

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
        assertNotNull(measurements.moisture);
        assertNotNull(measurements.salinity);
        assertEquals(100, measurements.battery);

        Measurement light = measurements.light;
        assertEquals(1d, light.values.current);
        assertEquals("μmol/h", light.unit);
    }
}
