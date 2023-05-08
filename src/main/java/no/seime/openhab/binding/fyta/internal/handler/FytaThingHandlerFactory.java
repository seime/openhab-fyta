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

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.Gson;

import no.seime.openhab.binding.fyta.internal.BindingConstants;
import no.seime.openhab.binding.fyta.internal.GsonFactory;
import no.seime.openhab.binding.fyta.internal.comm.RestApiClient;
import no.seime.openhab.binding.fyta.internal.discovery.FytaDiscoveryService;

/**
 * The {@link FytaThingHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.fyta", service = ThingHandlerFactory.class)
public class FytaThingHandlerFactory extends BaseThingHandlerFactory {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableSet(Stream
            .of(BindingConstants.THING_TYPE_ACCOUNT, BindingConstants.THING_TYPE_PLANT).collect(Collectors.toSet()));
    @NonNullByDefault({})
    private final HttpClient httpClient;
    @NonNullByDefault({})
    private StorageService storageService;
    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    private Gson gson = GsonFactory.create();

    @Activate
    public FytaThingHandlerFactory(@Reference HttpClientFactory httpClientFactory,
            @Reference StorageService storageService) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.storageService = storageService;
    }

    @Override
    protected @Nullable ThingHandler createHandler(final Thing thing) {
        final ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (BindingConstants.THING_TYPE_PLANT.equals(thingTypeUID)) {
            return new FytaPlantHandler(thing, storageService.getStorage(thing.getUID().toString()));
        } else if (BindingConstants.THING_TYPE_ACCOUNT.equals(thingTypeUID)) {
            RestApiClient restApiClient = new RestApiClient(httpClient, gson);
            FytaAccountHandler accountHandler = new FytaAccountHandler((Bridge) thing, restApiClient);
            registerDeviceDiscoveryService(accountHandler);
            return accountHandler;
        }
        return null;
    }

    private void registerDeviceDiscoveryService(FytaAccountHandler accountHandler) {
        FytaDiscoveryService discoveryService = new FytaDiscoveryService(accountHandler);
        discoveryServiceRegs.put(accountHandler.getThing().getUID(),
                bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<>()));
    }

    private void unregisterDeviceDiscoveryService(ThingUID thingUID) {
        if (discoveryServiceRegs.containsKey(thingUID)) {
            ServiceRegistration<?> serviceReg = discoveryServiceRegs.get(thingUID);
            serviceReg.unregister();
            discoveryServiceRegs.remove(thingUID);
        }
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof FytaAccountHandler) {
            ThingUID thingUID = thingHandler.getThing().getUID();
            unregisterDeviceDiscoveryService(thingUID);
        }
        super.removeHandler(thingHandler);
    }

    @Override
    public boolean supportsThingType(final ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }
}
