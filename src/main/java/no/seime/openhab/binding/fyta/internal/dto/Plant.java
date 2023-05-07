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

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link Plant} represents a plant with an associcated Fyta Beam sensor
 *
 * @author Arne Seime - Initial contribution
 */
public class Plant {
    public long id;

    public String nickname;

    @SerializedName("scientific_name")
    public String scientificName;
    @SerializedName("common_name")
    public String commonName;

    @SerializedName("status")
    public int overallStatus;
    @SerializedName("temperature_status")
    public Status temperatureStatus;
    @SerializedName("light_status")
    public Status lightStatus;
    @SerializedName("moisture_status")
    public Status moistureStatus;
    @SerializedName("salinity_status")
    public Status salinityStatus;

    @SerializedName("thumb_path")
    public String userThumbnailPath;

    @SerializedName("origin_path")
    public String userOriginalPath;

    @SerializedName("received_data_at")
    public ZonedDateTime receivedDataAt;

    public Sensor sensor;

    @SerializedName("has_remote_sensor")
    public boolean hasSensor;

    public Measurements measurements;

    @SerializedName("temperature_unit")
    public int temperatureUnit;

    public Map<String, String> getThingProperties() {

        Map<String, String> properties = new HashMap<>();
        properties.put("nickname", nickname);
        properties.put("scientificName", scientificName);
        properties.put("macAddress", sensor.macAddress);
        properties.put("firmwareVersion", sensor.firmwareVersion);
        if (userThumbnailPath != null) {
            properties.put("userThumbnailPath", userThumbnailPath);
        }
        if (userOriginalPath != null) {
            properties.put("userOriginalPath", userOriginalPath);
        }
        return properties;
    }
}
