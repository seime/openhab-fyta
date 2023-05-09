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

import com.google.gson.annotations.SerializedName;

/**
 * The {@link Status} represents a human-readable overall status for a given parameters for the plant
 *
 * @author Arne Seime - Initial contribution
 *         /
 */
public enum Status {

    @SerializedName("1")
    TOO_LOW,
    @SerializedName("2")
    LOW,
    @SerializedName("3")
    PERFECT,
    @SerializedName("4")
    HIGH,
    @SerializedName("5")
    TOO_HIGH

}
