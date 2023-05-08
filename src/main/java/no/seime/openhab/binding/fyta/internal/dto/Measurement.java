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
 * The {@link Measurements} represents an details on a specific measurement
 *
 * @author Arne Seime - Initial contribution
 */
public class Measurement {
    public Status status;
    public String unit;
    public Values values;
    @SerializedName("absolute_values")
    public AbsoluteValues absoluteValues;

    public static class Values {
        @SerializedName("min_good")
        public double minGood;
        @SerializedName("max_good")
        public double maxGood;
        @SerializedName("min_acceptable")
        public double minAcceptable;
        @SerializedName("max_acceptable")
        public double maxAcceptable;

        public double current;

        public String currentFormatted;
        @SerializedName("optimal_hours")
        public int optimalHours;
    }

    public static class AbsoluteValues {
        public double min;
        public double max;
    }
}
