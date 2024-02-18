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
package no.seime.openhab.binding.fyta.internal.discovery;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.seime.openhab.binding.fyta.internal.BindingConstants;
import no.seime.openhab.binding.fyta.internal.dto.Plant;
import no.seime.openhab.binding.fyta.internal.handler.FytaAccountHandler;

/**
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
public class FytaDiscoveryService extends AbstractDiscoveryService {
    public static final Set<ThingTypeUID> DISCOVERABLE_THING_TYPES_UIDS = Collections
            .singleton(BindingConstants.THING_TYPE_PLANT);
    private static final long DISCOVERY_INTERVAL_MINUTES = 60 * 4; // EVERY 4 HOUR
    public static final String MAC_ADDRESS_PROPERTY = "macAddress";
    private final Logger logger = LoggerFactory.getLogger(FytaDiscoveryService.class);
    private final FytaAccountHandler accountHandler;
    private Optional<ScheduledFuture<?>> discoveryJob = Optional.empty();

    public FytaDiscoveryService(final FytaAccountHandler accountHandler) {
        super(DISCOVERABLE_THING_TYPES_UIDS, 3);
        this.accountHandler = accountHandler;
    }

    @Override
    protected void startBackgroundDiscovery() {
        discoveryJob = Optional
                .of(scheduler.scheduleWithFixedDelay(this::startScan, 0, DISCOVERY_INTERVAL_MINUTES, TimeUnit.MINUTES));
    }

    @Override
    protected void startScan() {
        logger.debug("Start scan for Fyta plants");
        synchronized (this) {
            removeOlderResults(getTimestampOfLastScan(), null, accountHandler.getThing().getUID());
            final ThingUID accountUID = accountHandler.getThing().getUID();
            accountHandler.doPoll();

            for (Plant plant : accountHandler.getModel().plants) {

                if (plant.sensor != null && plant.sensor.hasSensor && plant.sensor.macAddress != null) {
                    ThingTypeUID thingType = BindingConstants.THING_TYPE_PLANT;

                    final ThingUID deviceUID = new ThingUID(thingType, accountUID,
                            plant.sensor.macAddress.replace(":", ""));

                    final DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(deviceUID)
                            .withBridge(accountUID).withProperty(MAC_ADDRESS_PROPERTY, plant.sensor.macAddress)
                            .withLabel(createPlantDiscoveryLabel(plant))
                            .withRepresentationProperty(MAC_ADDRESS_PROPERTY).build();
                    thingDiscovered(discoveryResult);
                }
            }
        }
    }

    private String createPlantDiscoveryLabel(Plant plant) {
        return String.join(" / ", plant.nickname, plant.scientificName, plant.commonName);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        stopScan();
        discoveryJob.ifPresent(job -> {
            if (!job.isCancelled()) {
                job.cancel(true);
            }
            discoveryJob = Optional.empty();
        });
    }

    @Override
    protected synchronized void stopScan() {
        logger.debug("Stop scan for devices.");
        super.stopScan();
    }
}
