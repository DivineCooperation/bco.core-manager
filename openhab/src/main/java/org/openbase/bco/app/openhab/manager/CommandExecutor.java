package org.openbase.bco.app.openhab.manager;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Message;
import org.eclipse.smarthome.core.types.Command;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.app.openhab.manager.transform.ServiceStateCommandTransformerPool;
import org.openbase.bco.app.openhab.manager.transform.ServiceTypeCommandMapping;
import org.openbase.bco.app.openhab.registry.synchronizer.OpenHABItemProcessor;
import org.openbase.bco.app.openhab.registry.synchronizer.OpenHABItemProcessor.OpenHABItemNameMetaData;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public class CommandExecutor implements Observer<Object, JsonObject> {

    public static final String PAYLOAD_KEY = "payload";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);

    private final UnitControllerRegistry<UnitController<?, ?>> unitControllerRegistry;
    private final JsonParser jsonParser;

    public CommandExecutor(final UnitControllerRegistry unitControllerRegistry) {
        this.unitControllerRegistry = unitControllerRegistry;
        this.jsonParser = new JsonParser();
    }

    @Override
    public void update(Object source, JsonObject payload) {
        // extract item name from topic
        final String topic = payload.get(OpenHABRestCommunicator.TOPIC_KEY).getAsString();
        // topic structure: smarthome/items/{itemName}/command
        final String itemName = topic.split(OpenHABRestCommunicator.TOPIC_SEPARATOR)[2];

        // extract payload
        final String state = jsonParser.parse(payload.get(PAYLOAD_KEY).getAsString()).getAsJsonObject().get("value").getAsString();

        try {
            applyStateUpdate(itemName, state);
        } catch (CouldNotPerformException ex) {
            LOGGER.warn("Could not apply state update[" + state + "] for item[" + itemName + "]", ex);
        }
    }

    public void applyStateUpdate(final String itemName, final String state) throws CouldNotPerformException {
        OpenHABItemNameMetaData metaData;
        try {
            metaData = OpenHABItemProcessor.getMetaData(itemName);
        } catch (CouldNotPerformException ex) {
            // skip update for non bco handled items
            return;
        }
        try {
            final UnitController unitController = unitControllerRegistry.get(Registries.getUnitRegistry().getUnitConfigByAlias(metaData.getAlias()).getId());
            final Message serviceData = getServiceData(state, metaData.getServiceType());

            if (serviceData == null) {
                // unsupported state for service, see CommandTransformer for details
                return;
            }

            unitController.applyDataUpdate(serviceData, metaData.getServiceType());
        } catch (NotAvailableException ex) {
            if (!unitControllerRegistry.isInitiallySynchronized()) {
                LOGGER.debug("ItemUpdate[" + itemName + "=" + state + "] skipped because controller registry was not ready yet!");
                return;
            }
            throw ex;
        }
    }

    private static final String EMPTY_COMMAND_STRING = "null";

    public static Message getServiceData(final String commandString, final ServiceType serviceType) throws CouldNotPerformException {
        if (commandString.equalsIgnoreCase(EMPTY_COMMAND_STRING)) {
            LOGGER.debug("Ignore state update [" + commandString + "] for service[" + serviceType + "]");
            return null;
        }

        try {
            Command command = null;
            for (Class<? extends Command> commandClass : ServiceTypeCommandMapping.getCommandClasses(serviceType)) {
                try {
                    command = (Command) commandClass.getMethod("valueOf", commandString.getClass()).invoke(null, commandString);
                    break;
                } catch (IllegalAccessException | NoSuchMethodException ex) {
                    LOGGER.error("Command class[" + commandClass.getSimpleName() + "] does not posses a valueOf(String) method", ex);
                } catch (IllegalArgumentException ex) {
                    // continue with the next command class, exception will be thrown if none is found
                } catch (InvocationTargetException ex) {
                    // ignore because the value of method threw an exception, this can happen if e.g. 0 is returned for
                    // a roller shutter as the opening ratio and the stopMoveType is tested
                }
            }

            if (command == null) {
                throw new CouldNotPerformException("Could not transform [" + commandString + "] into a state for service type[" + serviceType.name() + "]");
            }

            Message serviceData = ServiceStateCommandTransformerPool.getInstance().getTransformer(serviceType, command.getClass()).transform(command);
            return TimestampProcessor.updateTimestamp(System.currentTimeMillis(), serviceData, TimeUnit.MICROSECONDS);
        } catch (NotAvailableException ex) {
            throw new CouldNotPerformException("Could not transform [" + commandString + "] into a state for service type[" + serviceType.name() + "]", ex);
        }
    }
}