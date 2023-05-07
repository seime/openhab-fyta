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
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

import no.seime.openhab.binding.fyta.internal.AccountConfiguration;
import no.seime.openhab.binding.fyta.internal.comm.RestApiClient;
import no.seime.openhab.binding.fyta.internal.comm.RestCommunicationException;
import no.seime.openhab.binding.fyta.internal.dto.GetPlantsRequest;
import no.seime.openhab.binding.fyta.internal.dto.GetPlantsResponse;
import no.seime.openhab.binding.fyta.internal.dto.LoginRequest;
import no.seime.openhab.binding.fyta.internal.dto.LoginResponse;

/**
 * The {@link FytaAccountHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
public class FytaAccountHandler extends BaseBridgeHandler {
    private static final int MAX_COMMUNICATION_ERRORS = 20;
    private static final long REFRESH_INTERVAL_SECONDS = 3600;
    private final Logger logger = LoggerFactory.getLogger(FytaAccountHandler.class);
    private Optional<ScheduledFuture<?>> statusFuture = Optional.empty();
    private GetPlantsResponse model;
    @NonNullByDefault({})
    AccountConfiguration config;
    private RestApiClient apiBridge;

    @Nullable
    private String accessToken;
    @Nullable
    private Instant accessTokenExpiry;

    private int communicationErrorCounter = 0;

    public FytaAccountHandler(Bridge thing, RestApiClient restApiClient) {
        super(thing);
        apiBridge = restApiClient;
        this.model = new GetPlantsResponse();
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        // Ignore commands as none are supported
    }

    @Override
    public void initialize() {
        // Stop any pending updates if any
        stopScheduledUpdate();

        updateStatus(ThingStatus.UNKNOWN);
        AccountConfiguration loadedConfig = getConfigAs(AccountConfiguration.class);
        config = loadedConfig;
        apiBridge.init(thing.getUID());
        scheduler.submit(this::doPoll);
    }

    @Override
    public void dispose() {
        stopScheduledUpdate();
        super.dispose();
    }

    public synchronized void doPoll() {

        try {
            if (accessToken == null || accessTokenExpiry == null || accessTokenExpiry
                    .isBefore(Instant.now().plus(REFRESH_INTERVAL_SECONDS * 2, ChronoUnit.SECONDS))) {
                logger.debug("Performing new login");
                LoginRequest loginRequest = new LoginRequest(config.email, config.password);
                final LoginResponse loginResponse = apiBridge.sendRequest(loginRequest, new TypeToken<LoginResponse>() {
                }.getType());
                accessToken = loginResponse.accessToken;
                accessTokenExpiry = Instant.now().plusSeconds(loginResponse.expiresIn);
                apiBridge.setAccessToken(loginResponse.accessToken);
                logger.debug("Login success");
            }

            final GetPlantsRequest getPlantsRequest = new GetPlantsRequest();
            model = apiBridge.sendRequest(getPlantsRequest, new TypeToken<GetPlantsResponse>() {
            }.getType());
            communicationErrorCounter = 0;
            updateStatus(ThingStatus.ONLINE);

            statusFuture = Optional.of(scheduler.schedule(this::doPoll, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS));

        } catch (RestCommunicationException e) {
            logger.warn("Error communicating with Fyta server", e);
            if (e.getHttpStatus() == HttpStatus.NOT_FOUND_404) {
                accessToken = null;
                accessTokenExpiry = null;

                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid credentials");
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                // Schedule a new attempt
                communicationErrorCounter++;
                if (communicationErrorCounter < MAX_COMMUNICATION_ERRORS) {
                    statusFuture = Optional.of(scheduler.schedule(this::doPoll, 600, TimeUnit.SECONDS));
                }

            }

        }
    }

    /**
     * Stops this thing's polling future
     */
    private void stopScheduledUpdate() {
        statusFuture.ifPresent(future -> {
            if (!future.isCancelled()) {
                future.cancel(true);
            }
            statusFuture = Optional.empty();
        });
    }

    public GetPlantsResponse getModel() {
        return model;
    }

    public RestApiClient getRestApiClient() {
        return apiBridge;
    }
}
