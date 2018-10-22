package org.openbase.bco.app.openhab.registry.synchronizer;

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

import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.app.openhab.registry.diff.IdentifiableEnrichedThingDTO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.AbstractSynchronizer;
import rst.domotic.state.InventoryStateType.InventoryState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Synchronization for things not managed by the bco binding.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ThingDeviceUnitSynchronization extends AbstractSynchronizer<String, IdentifiableEnrichedThingDTO> {

    public ThingDeviceUnitSynchronization(final SyncObject synchronizationLock) throws InstantiationException {
        super(new ThingObservable(), synchronizationLock);
    }

    @Override
    protected void afterInternalSync() {
        logger.info("Internal sync finished!");
    }

    @Override
    public void update(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        logger.info("Update {} ...", identifiableEnrichedThingDTO.getDTO().UID);
        final EnrichedThingDTO updatedThing = identifiableEnrichedThingDTO.getDTO();

        // get device unit config for the thing
        try {
            final UnitConfig.Builder deviceUnitConfig = SynchronizationProcessor.getDeviceForThing(updatedThing).toBuilder();

            if (SynchronizationProcessor.updateUnitToThing(updatedThing, deviceUnitConfig)) {
                Registries.getUnitRegistry().updateUnitConfig(deviceUnitConfig.build());
//                try {
//                    Registries.getUnitRegistry().updateUnitConfig(deviceUnitConfig.build()).get();
//                } catch (ExecutionException ex) {
//                    throw new CouldNotPerformException("Could not update device[" + deviceUnitConfig.getLabel() + "] for thing[" + identifiableEnrichedThingDTO.getId() + "]", ex);
//                }
            }
        } catch (NotAvailableException ex) {
            logger.warn("Unit for thing {} not available", identifiableEnrichedThingDTO.getDTO().UID);
        }
    }

    @Override
    public void register(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        logger.info("Register {} ...", identifiableEnrichedThingDTO.getDTO().UID);
        final ThingDTO thingDTO = identifiableEnrichedThingDTO.getDTO();

        // handle initial sync
        if (isInitialSync()) {
            try {
                final UnitConfig deviceUnitConfig = SynchronizationProcessor.getDeviceForThing(thingDTO);
                // create items for dal units of the device
                for (String unitId : deviceUnitConfig.getDeviceConfig().getUnitIdList()) {
                    SynchronizationProcessor.registerAndValidateItems(Registries.getUnitRegistry().getUnitConfigById(unitId), thingDTO);
                }
                return;
            } catch (NotAvailableException ex) {
                // go on to register a device for thing
            }
        }

        try {
            SynchronizationProcessor.getDeviceForThing(thingDTO);
        } catch (NotAvailableException ex) {
            GlobalCachedExecutorService.submit(() -> {
                registerDevice(thingDTO);
                return null;
            });
        }
    }

    private void registerDevice(ThingDTO thingDTO) throws CouldNotPerformException, InterruptedException {
        //TODO: should this whole action be rolled back if one part fails?
        // get device class for thing
        DeviceClass deviceClass;
        try {
            deviceClass = SynchronizationProcessor.getDeviceClassByIdentifier(thingDTO);
        } catch (NotAvailableException ex) {
            logger.warn("Ignore thing {} because: {}", thingDTO.UID, ex.getMessage());
            return;
        }

        // create device for this class
        UnitConfig.Builder unitConfig = UnitConfig.newBuilder();
        unitConfig.setUnitType(UnitType.DEVICE);
        unitConfig.getDeviceConfigBuilder().setDeviceClassId(deviceClass.getId());
        unitConfig.getDeviceConfigBuilder().getInventoryStateBuilder().setValue(State.INSTALLED).setTimestamp(TimestampProcessor.getCurrentTimestamp());

        // update location according to thing
        if (thingDTO.location != null) {
            String locationId = SynchronizationProcessor.getLocationForThing(thingDTO).getId();

            unitConfig.getDeviceConfigBuilder().getInventoryStateBuilder().setLocationId(locationId);
            unitConfig.getPlacementConfigBuilder().setLocationId(locationId);
        } else {
            logger.info("Thing has no location defined so it will be added at the root location");
        }

        //TODO: which language to use
        // update label according to thing
        LabelProcessor.addLabel(unitConfig.getLabelBuilder(), Locale.ENGLISH, thingDTO.label);

        // add thing uid to meta config to have a mapping between thing and device
        unitConfig.getMetaConfigBuilder().addEntryBuilder().setKey(SynchronizationProcessor.OPENHAB_THING_UID_KEY).setValue(thingDTO.UID);

        try {
            final UnitConfig deviceUnitConfig = Registries.getUnitRegistry().registerUnitConfig(unitConfig.build()).get();

            // create items for dal units of the device
            for (String unitId : deviceUnitConfig.getDeviceConfig().getUnitIdList()) {
                SynchronizationProcessor.registerAndValidateItems(Registries.getUnitRegistry().getUnitConfigById(unitId), thingDTO);
            }
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not register device for thing[" + thingDTO.thingTypeUID + "]", ex);
        }
    }

    @Override
    public void remove(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        logger.info("Remove {} ...", identifiableEnrichedThingDTO.getDTO().UID);
        final EnrichedThingDTO enrichedThingDTO = identifiableEnrichedThingDTO.getDTO();

        // get device unit config for thing
        UnitConfig deviceUnitConfig;
        try {
            deviceUnitConfig = SynchronizationProcessor.getDeviceForThing(enrichedThingDTO);
        } catch (NotAvailableException ex) {
            // do nothing if no device exists
            return;
        }

        // remove device
        Registries.getUnitRegistry().removeUnitConfig(deviceUnitConfig);
//        try {
//            Registries.getUnitRegistry().removeUnitConfig(deviceUnitConfig).get();
//        } catch (ExecutionException ex) {
//            throw new CouldNotPerformException("Could not remove device[" + deviceUnitConfig.getLabel() + "] for thing[" + identifiableEnrichedThingDTO.getId() + "]", ex);
//        }
    }

    @Override
    public List<IdentifiableEnrichedThingDTO> getEntries() throws CouldNotPerformException {
        final List<IdentifiableEnrichedThingDTO> identifiableEnrichedThingDTOList = new ArrayList<>();
        for (final EnrichedThingDTO enrichedThingDTO : OpenHABRestCommunicator.getInstance().getThings()) {
            identifiableEnrichedThingDTOList.add(new IdentifiableEnrichedThingDTO(enrichedThingDTO));
        }
        return identifiableEnrichedThingDTOList;
    }

    @Override
    public boolean isSupported(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) {
        // only handle things not managed by the bco binding
        return !identifiableEnrichedThingDTO.getId().startsWith(ThingUnitSynchronization.BCO_BINDING_ID);
    }
}