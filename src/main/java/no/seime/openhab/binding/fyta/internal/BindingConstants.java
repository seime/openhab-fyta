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
package no.seime.openhab.binding.fyta.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link BindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
public class BindingConstants {
    public static final String BINDING_ID = "fyta";
    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ACCOUNT = new ThingTypeUID(BINDING_ID, "account");
    public static final ThingTypeUID THING_TYPE_PLANT = new ThingTypeUID(BINDING_ID, "plant");
    // Fixed channels
    public static final String CHANNEL_NICKNAME = "nickname";
    public static final String CHANNEL_THUMBNAIL = "thumbnail";
    public static final String CHANNEL_TEMPERATURE_STATUS = "temperature-status";
    public static final String CHANNEL_TEMPERATURE = "temperature";
    public static final String CHANNEL_TEMPERATURE_MIN_ACCEPTABLE = "temperature-min-acceptable";
    public static final String CHANNEL_TEMPERATURE_MIN_GOOD = "temperature-min-good";
    public static final String CHANNEL_TEMPERATURE_MAX_GOOD = "temperature-max-good";
    public static final String CHANNEL_TEMPERATURE_MAX_ACCEPTABLE = "temperature-max-acceptable";

    public static final String CHANNEL_LIGHT_STATUS = "light-status";
    public static final String CHANNEL_LIGHT = "light";

    public static final String CHANNEL_MOISTURE_STATUS = "moisture-status";
    public static final String CHANNEL_MOISTURE = "moisture";
    public static final String CHANNEL_MOISTURE_MIN_ACCEPTABLE = "moisture-min-acceptable";
    public static final String CHANNEL_MOISTURE_MIN_GOOD = "moisture-min-good";
    public static final String CHANNEL_MOISTURE_MAX_GOOD = "moisture-max-good";
    public static final String CHANNEL_MOISTURE_MAX_ACCEPTABLE = "moisture-max-acceptable";

    public static final String CHANNEL_SALINITY_STATUS = "salinity-status";
    public static final String CHANNEL_SALINITY = "salinity";
    public static final String CHANNEL_SALINITY_MIN_ACCEPTABLE = "salinity-min-acceptable";
    public static final String CHANNEL_SALINITY_MIN_GOOD = "salinity-min-good";
    public static final String CHANNEL_SALINITY_MAX_GOOD = "salinity-max-good";
    public static final String CHANNEL_SALINITY_MAX_ACCEPTABLE = "salinity-max-acceptable";

    public static final String CHANNEL_BATTERY = "battery";
    public static final String CHANNEL_LAST_UPDATED = "last-updated";
}
