/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.lwm2m.internal;

import static org.eclipse.smarthome.binding.lwm2m.Lwm2mBindingConstants.THING_TYPE_BRIDGE;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.smarthome.binding.lwm2m.Lwm2mBindingConstants;
import org.eclipse.smarthome.binding.lwm2m.handler.Lwm2mClientHandler;
import org.eclipse.smarthome.binding.lwm2m.handler.Lwm2mObjectInstanceHandler;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Lwm2mHandlerFactory} is responsible for creating things and thing
 * handlers. It requests a list of all available ThingTypes in {@link ThingTypeRegistry}
 * and adds all ThingTypes for the current binding ID to the list of supported Things.
 *
 * We need to implement it this way, because we periodically sync the LwM2M Objects/Resources
 * of the OMAP LwM2M Registry to ESH Thing and Channel xml files. Those files are our reference
 * of supported Things instead of a fixed set like in other bindings.
 *
 * @author David Graeff - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, immediate = true, configurationPolicy = ConfigurationPolicy.OPTIONAL, name = "binding.lwm2m")
public class Lwm2mHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(Lwm2mHandlerFactory.class);
    final Set<ThingTypeUID> supportedThingTypeUIDs = new HashSet<>();
    private ServiceTracker<ThingTypeRegistry, ThingTypeRegistry> thingTypeRegistryServiceTracker;

    protected ThingTypeRegistry getThingTypeRegistry() {
        ThingTypeRegistry r = thingTypeRegistryServiceTracker.getService();
        if (r == null) {
            throw new RuntimeException(
                    "The ThingTypeRegistry was not available in Lwm2mHandlerFactory.supportsThingType");
        }
        return r;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        if (supportedThingTypeUIDs.isEmpty()) {
            ThingTypeRegistry r = getThingTypeRegistry();
            for (ThingType t : r.getThingTypes()) {
                if (t.getBindingId().equals(Lwm2mBindingConstants.BINDING_ID)) {
                    supportedThingTypeUIDs.add(t.getUID());
                }
            }
            supportedThingTypeUIDs.add(Lwm2mBindingConstants.THING_TYPE_BRIDGE);

            logger.info("LwM2M supported things: {}", supportedThingTypeUIDs.size());
        }
        return supportedThingTypeUIDs.contains(thingTypeUID);
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        thingTypeRegistryServiceTracker = new ServiceTracker<>(bundleContext, ThingTypeRegistry.class.getName(), null);
        thingTypeRegistryServiceTracker.open();
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        thingTypeRegistryServiceTracker.close();
        super.deactivate(componentContext);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_BRIDGE)) {
            return new Lwm2mClientHandler(thing);
        } else if (supportedThingTypeUIDs.contains(thingTypeUID)) {
            return new Lwm2mObjectInstanceHandler(thing);
        }

        return null;
    }
}
