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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.RawType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.storage.Storage;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

import no.seime.openhab.binding.fyta.internal.BindingConstants;
import no.seime.openhab.binding.fyta.internal.PlantConfiguration;
import no.seime.openhab.binding.fyta.internal.comm.RestCommunicationException;
import no.seime.openhab.binding.fyta.internal.dto.GetPlantDetailsRequest;
import no.seime.openhab.binding.fyta.internal.dto.GetPlantDetailsResponse;
import no.seime.openhab.binding.fyta.internal.dto.Plant;

/**
 * The {@link FytaPlantHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Arne Seime - Initial contribution
 */
public class FytaPlantHandler extends BaseThingHandler {

    public static final int BEAM_REPORTING_INTERVAL_SECONDS = 3600;
    private final Logger logger = LoggerFactory.getLogger(FytaPlantHandler.class);
    private final Storage<String> storage;

    private final String STORAGE_KEY_THUMBNAIL = "thumbnail-url";
    protected long plantId;

    protected String macAddress;
    protected FytaAccountHandler accountHandler;

    private Optional<ScheduledFuture<?>> statusFuture = Optional.empty();

    @Nullable
    private Plant plant = null;

    public FytaPlantHandler(Thing thing, Storage<String> storage) {
        super(thing);
        this.storage = storage;
    }

    @Override
    public void initialize() {
        stopScheduledUpdate();
        PlantConfiguration config = getConfigAs(PlantConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);

        logger.debug("[{}] Initializing plant", config.macAddress);

        // Clear old image
        storage.remove(STORAGE_KEY_THUMBNAIL);

        accountHandler = (FytaAccountHandler) getBridge().getHandler();

        if (accountHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_MISSING_ERROR,
                    "Cannot poll plant without a configured account bridge");

        } else {
            Optional<Plant> device = accountHandler.getModel().findDeviceByMacAddress(config.macAddress);
            if (device.isPresent()) {
                plantId = device.get().id;
                macAddress = config.macAddress;
                loadPlantDetails();
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Could not find plant in internal model, check plantId configuration");
            }
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);

        logger.debug("Bridge status changed, now {}", bridgeStatusInfo);
        if (bridgeStatusInfo.getStatus() != ThingStatus.ONLINE) {
            thing.getChannels().forEach(channel -> updateState(channel.getUID(), UnDefType.UNDEF));
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            stopScheduledUpdate();
        } else {
            scheduler.submit(this::initialize);
        }
    }

    /**
     * Stops this thing's polling future
     */
    private synchronized void stopScheduledUpdate() {
        statusFuture.ifPresent(future -> {
            if (!future.isCancelled()) {
                future.cancel(true);
            }
            statusFuture = Optional.empty();
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        stopScheduledUpdate();
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        initialize();
    }

    public void loadPlantDetails() {
        Optional<Plant> optionalPlant = accountHandler.getModel().findDeviceByMacAddress(macAddress);
        if (optionalPlant.isPresent()) {
            try {
                final GetPlantDetailsRequest getDeviceDetailsRequest = new GetPlantDetailsRequest(plantId);

                final GetPlantDetailsResponse response = accountHandler.getRestApiClient()
                        .sendRequest(getDeviceDetailsRequest, new TypeToken<GetPlantDetailsResponse>() {
                        }.getType());

                this.plant = response.plant;

                Map<String, String> properties = optionalPlant.get().getThingProperties();
                updateThing(editThing().withProperties(properties).build());
                updateStatus(ThingStatus.ONLINE);

                // Update all channels
                getThing().getChannels().forEach(channel -> updateChannel(plant, channel.getUID()));

                // Schedule next fetching
                Instant nextPollingTime = this.plant.sensor.receivedDataAt.toInstant()
                        .plusSeconds(BEAM_REPORTING_INTERVAL_SECONDS + 60l);
                long delayUntilNextPolling = nextPollingTime.getEpochSecond() - Instant.now().getEpochSecond();
                if (delayUntilNextPolling < 600) {
                    delayUntilNextPolling = 600; // If data is delayed, do not hammer on server
                }

                logger.debug("[{}] Next polling at {}", macAddress, ZonedDateTime
                        .ofInstant(Instant.now().plusSeconds(delayUntilNextPolling), ZoneId.systemDefault()));

                statusFuture = Optional
                        .of(scheduler.schedule(this::loadPlantDetails, delayUntilNextPolling, TimeUnit.SECONDS));

            } catch (RestCommunicationException ex) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Error retrieving data from server: " + ex.getMessage());
                // Undef all channels if error
                thing.getChannels().forEach(channel -> updateState(channel.getUID(), UnDefType.UNDEF));
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Could not find plant in internal model, check macAddress in configuration");
            thing.getChannels().forEach(e -> updateState(e.getUID(), UnDefType.UNDEF));
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (plant != null) {
            updateChannel(plant, channelUID);
        } else {
            loadPlantDetails();
        }
    }

    private void updateChannel(Plant plant, ChannelUID channelUID) {
        logger.debug("[{}] Updating channel {}", macAddress, channelUID);
        switch (channelUID.getId()) {
            case BindingConstants.CHANNEL_TEMPERATURE:
                updateState(channelUID, new QuantityType<>(plant.measurements.temperature.values.current,
                        plant.temperatureUnit == 1 ? SIUnits.CELSIUS : ImperialUnits.FAHRENHEIT));
                break;
            case BindingConstants.CHANNEL_LIGHT:
                updateState(channelUID, new DecimalType(plant.measurements.light.values.current));
                break;
            case BindingConstants.CHANNEL_MOISTURE:
                updateState(channelUID, new QuantityType<>(plant.measurements.moisture.values.current, Units.PERCENT));
                break;
            case BindingConstants.CHANNEL_SALINITY:
                updateState(channelUID, new DecimalType(plant.measurements.salinity.values.currentFormatted));
                break;
            case BindingConstants.CHANNEL_TEMPERATURE_STATUS:
                updateState(channelUID, new StringType(plant.measurements.temperature.status.toString()));
                break;
            case BindingConstants.CHANNEL_LIGHT_STATUS:
                updateState(channelUID, new StringType(plant.measurements.light.status.toString()));
                break;
            case BindingConstants.CHANNEL_MOISTURE_STATUS:
                updateState(channelUID, new StringType(plant.measurements.moisture.status.toString()));
                break;
            case BindingConstants.CHANNEL_SALINITY_STATUS:
                updateState(channelUID, new StringType(plant.measurements.salinity.status.toString()));
                break;
            case BindingConstants.CHANNEL_NICKNAME:
                updateState(channelUID, new StringType(plant.nickname));
                break;
            case BindingConstants.CHANNEL_BATTERY:
                updateState(channelUID, new QuantityType<>(plant.measurements.battery, Units.PERCENT));
                break;
            case BindingConstants.CHANNEL_LAST_UPDATED:
                updateState(channelUID, new DateTimeType(plant.receivedDataAt));
                break;
            case BindingConstants.CHANNEL_THUMBNAIL:
                String cachedThumbnailUrl = storage.get(STORAGE_KEY_THUMBNAIL);
                if (plant.userThumbnailPath != null & !plant.userThumbnailPath.equals(cachedThumbnailUrl)) {
                    try {
                        logger.debug("[{}] Fetching thumbnail image {}", macAddress, plant.userThumbnailPath);

                        RawType image = accountHandler.getRestApiClient().getImage(plant.userThumbnailPath);
                        updateState(channelUID, image);
                    } catch (RestCommunicationException e) {
                        logger.warn("[{}] Error fetching thumbnail image {}", plant.userThumbnailPath, macAddress, e);
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                }
                if (plant.userThumbnailPath != null && !plant.userThumbnailPath.equals(cachedThumbnailUrl)) {
                    storage.put(STORAGE_KEY_THUMBNAIL, plant.userThumbnailPath);
                } else {
                    storage.remove(STORAGE_KEY_THUMBNAIL);
                }
                break;
            default:
                logger.warn("Unhandled channel {}", channelUID);
        }
    }

    @Override
    public @Nullable Bridge getBridge() {
        return super.getBridge();
    }
}
