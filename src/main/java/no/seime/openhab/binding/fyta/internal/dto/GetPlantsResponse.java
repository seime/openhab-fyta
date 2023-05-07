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

import java.util.Arrays;
import java.util.Optional;

/**
 * All classes in the .dto are data transfer classes used by the GSON mapper. This class reflects a
 * part of a request/response data structure.
 *
 * @author Arne Seime - Initial contribution.
 */

public class GetPlantsResponse {
    public Plant[] plants = new Plant[0];

    public Optional<Plant> findDeviceByMacAddress(String macAddress) {
        return Arrays.stream(plants).filter(e -> macAddress.equals(e.sensor.macAddress)).findFirst();
    }
}
