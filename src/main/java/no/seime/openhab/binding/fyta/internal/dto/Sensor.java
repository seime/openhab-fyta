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

import com.google.gson.annotations.SerializedName;

/**
 * The {@link Plant} represents a Fyta beam sensor
 *
 * @author Arne Seime - Initial contribution
 *
 */
public class Sensor {

    @SerializedName("id")
    public String macAddress;

    @SerializedName("version")
    public String firmwareVersion;

    @SerializedName("received_data_at")
    public ZonedDateTime receivedDataAt;
}
