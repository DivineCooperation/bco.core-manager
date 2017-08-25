package org.openbase.bco.manager.device.binding.openhab.util.configgen;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AbstractItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AgentItemEntry;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AgentItemEntry.AGENT_GROUP_LABEL;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AppItemEntry;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AppItemEntry.APP_GROUP_LABEL;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.LocationItemEntry;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.LocationItemEntry.LOCATION_GROUP_LABEL;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.SceneItemEntry;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.SceneItemEntry.SCENE_GROUP_LABEL;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.ServiceItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABItemConfig;
import org.openbase.bco.registry.agent.remote.AgentRegistryRemote;
import org.openbase.bco.registry.app.remote.AppRegistryRemote;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.bco.registry.scene.remote.SceneRegistryRemote;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType;
import rst.domotic.state.InventoryStateType.InventoryState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class OpenHABItemConfigGenerator {

    public static final int TAB_SIZE = 4;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABItemConfigGenerator.class);

    private final List<AbstractItemEntry> itemEntryList;
    private final List<GroupEntry> groupEntryList;
    private final UnitRegistry unitRegistry;
    private final LocationRegistryRemote locationRegistryRemote;
    private final SceneRegistryRemote sceneRegistryRemote;
    private final AgentRegistryRemote agentRegistryRemote;
    private final AppRegistryRemote appRegistryRemote;
    private final DeviceRegistry deviceRegistry;

    public OpenHABItemConfigGenerator(final DeviceRegistry deviceRegistry, final UnitRegistry unitRegistry, final LocationRegistryRemote locationRegistryRemote, final SceneRegistryRemote sceneRegistryRemote, final AgentRegistryRemote agentRegistryRemote, final AppRegistryRemote appRegistryRemote) throws InstantiationException {
        try {
            this.itemEntryList = new ArrayList<>();
            this.groupEntryList = new ArrayList<>();
            this.unitRegistry = unitRegistry;
            this.deviceRegistry = deviceRegistry;
            this.locationRegistryRemote = locationRegistryRemote;
            this.sceneRegistryRemote = sceneRegistryRemote;
            this.agentRegistryRemote = agentRegistryRemote;
            this.appRegistryRemote = appRegistryRemote;
        } catch (NullPointerException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException, CouldNotPerformException {
    }

    public synchronized void generate() throws CouldNotPerformException, InterruptedException {
        logger.info("generate item config");
        try {
            itemEntryList.clear();
            groupEntryList.clear();
            AbstractItemEntry.reset();
            GroupEntry.reset();
            generateGroupEntries();
            generateItemEntries();
            serializeToFile();
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Could not generate item config", ex);
        }
    }

    private void generateGroupEntries() throws CouldNotPerformException {
        try {
            // generate location groups
            GroupEntry groupEntry, rootEntry = null;
            List<UnitConfig> locationConfigList = locationRegistryRemote.getData().getLocationUnitConfigList();
            for (final UnitConfig locationUnitConfig : locationConfigList) {
                groupEntry = new GroupEntry(locationUnitConfig, locationRegistryRemote);
                groupEntryList.add(new GroupEntry(locationUnitConfig, locationRegistryRemote));

                if (locationUnitConfig.getLocationConfig().getRoot()) {
                    rootEntry = groupEntry;
                }
            }
            
            if(rootEntry == null) {
                logger.warn("Group entries could not be generated because the root location is still missing! Register at least one location if the group items should be generated.");
                return;
            }
            
            Collections.sort(groupEntryList, (GroupEntry o1, GroupEntry o2) -> o1.getLabel().compareTo(o2.getLabel()));
            generateOverviewGroupEntries(rootEntry);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not generate group entries.", ex);
        }
    }

    private void generateOverviewGroupEntries(final GroupEntry rootGroupEntry) throws CouldNotPerformException {
        // generate overview menu
        GroupEntry overviewGroupEntry = new GroupEntry("Overview", "Übersicht", "settings", rootGroupEntry);
        groupEntryList.add(overviewGroupEntry);

        for (UnitType unitType : UnitType.values()) {
            if (unitType.equals(UnitType.UNKNOWN)) {
                continue;
            }
            String unitLabel = "bco_" + unitType.name().toLowerCase();
            groupEntryList.add(new GroupEntry(unitLabel, unitType.name(), unitLabel, overviewGroupEntry));
        }

        groupEntryList.add(new GroupEntry(SCENE_GROUP_LABEL, SCENE_GROUP_LABEL, SCENE_GROUP_LABEL, overviewGroupEntry));
        groupEntryList.add(new GroupEntry(AGENT_GROUP_LABEL, AGENT_GROUP_LABEL, AGENT_GROUP_LABEL, overviewGroupEntry));
        groupEntryList.add(new GroupEntry(APP_GROUP_LABEL, APP_GROUP_LABEL, APP_GROUP_LABEL, overviewGroupEntry));
        groupEntryList.add(new GroupEntry(LOCATION_GROUP_LABEL, LOCATION_GROUP_LABEL, LOCATION_GROUP_LABEL, overviewGroupEntry));

        for (ServiceType serviceType : ServiceType.values()) {
            final String serviceLabel = "bco_" + serviceType.name().toLowerCase();
            groupEntryList.add(new GroupEntry(serviceLabel, serviceType.name(), serviceLabel, overviewGroupEntry));
        }
    }

    private synchronized void generateItemEntries() throws CouldNotPerformException, InterruptedException {
        try {
            
            for (UnitConfig locationUnitConfig : locationRegistryRemote.getLocationConfigs()) {
                Map<ServiceType, ServiceDescription> serviceDescriptionsOnLocation = new HashMap<>();
                for (UnitConfig unitConfig : unitRegistry.getUnitConfigs()) {
                    if (locationUnitConfig.getLocationConfig().getUnitIdList().contains(unitConfig.getId())) {
                        for (ServiceDescription serviceDescription : unitRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceDescriptionList()) {
                            if (!serviceDescriptionsOnLocation.containsKey(serviceDescription.getType())) {
                                serviceDescriptionsOnLocation.put(serviceDescription.getType(), serviceDescription);
                            } else {
                                if (serviceDescriptionsOnLocation.get(serviceDescription.getType()).getPattern() == ServiceTemplate.ServicePattern.PROVIDER
                                        && serviceDescription.getPattern() == ServiceTemplate.ServicePattern.OPERATION) {
                                    serviceDescriptionsOnLocation.put(serviceDescription.getType(), serviceDescription);
                                }
                            }
                        }
                    }
                }
                for (ServiceDescription serviceDescription : serviceDescriptionsOnLocation.values()) {
                    if (serviceDescription.getType() == ServiceType.COLOR_STATE_SERVICE
                            || serviceDescription.getType() == ServiceType.POWER_STATE_SERVICE
                            || serviceDescription.getType() == ServiceType.POWER_CONSUMPTION_STATE_SERVICE) {
                        LocationItemEntry entry = new LocationItemEntry(locationUnitConfig, serviceDescription);
                        itemEntryList.add(entry);
                        logger.debug("Added location entry [" + entry.buildStringRep() + "]");
                    }
                }
            }

            // Scenes
            for (final UnitConfig sceneUnitConfig : sceneRegistryRemote.getSceneConfigs()) {
                // Skip disabled scenes
                if (sceneUnitConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED) {
                    itemEntryList.add(new SceneItemEntry(sceneUnitConfig, locationRegistryRemote));
                }
            }

            final List<UnitConfig> deviceUnitConfigList = unitRegistry.getUnitConfigs(UnitType.DEVICE);

            for (UnitConfig deviceUnitConfig : deviceUnitConfigList) {

                // load device class
                DeviceClass deviceClass = deviceRegistry.getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId());

                // ignore non openhab items
                if (!deviceClass.getBindingConfig().getBindingId().equalsIgnoreCase("OPENHAB")) {
                    continue;
                }

                // ignore non installed items
                if (deviceUnitConfig.getDeviceConfig().getInventoryState().getValue() != InventoryState.State.INSTALLED) {
                    continue;
                }

                final List<UnitConfig> dalUnitConfigList = new ArrayList<>();

                for (String unitId : deviceUnitConfig.getDeviceConfig().getUnitIdList()) {
                    dalUnitConfigList.add(unitRegistry.getUnitConfigById(unitId));
                }

                for (final UnitConfig unitConfig : dalUnitConfigList) {

                    // ignore disabled units
                    if (!unitConfig.getEnablingState().getValue().equals(EnablingStateType.EnablingState.State.ENABLED)) {
                        continue;
                    }

                    Set<ServiceType> serviceTypeSet = new HashSet<>();
                    for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                        if (serviceConfig.getServiceDescription().getPattern() == ServicePattern.CONSUMER) {
                            continue;
                        }

                        if (serviceTypeSet.contains(serviceConfig.getServiceDescription().getType())) {
                            continue;
                        }

                        if (serviceConfig.getServiceDescription().getPattern() == ServicePattern.PROVIDER) {
                            if (unitHasServiceAsOperationService(unitConfig, serviceConfig.getServiceDescription().getType())) {
                                continue;
                            }
                        }
                        
                        // TODO: fix that this has to be skipped, issue: https://github.com/openbase/bco.manager/issues/43
                        if(serviceConfig.getServiceDescription().getType() == ServiceType.TEMPERATURE_ALARM_STATE_SERVICE ){
                            continue;
                        }

                        serviceTypeSet.add(serviceConfig.getServiceDescription().getType());
                        try {
                            itemEntryList.add(new ServiceItemEntry(deviceClass, deviceUnitConfig.getMetaConfig(), unitConfig, serviceConfig, locationRegistryRemote));
                        } catch (Exception ex) {
                            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not generate item for Service[" + serviceConfig.getServiceDescription().getType().name() + "] of Unit[" + unitConfig.getLabel()+ "]", ex), logger, LogLevel.WARN);
                        }
                    }
                }
            }

            for (final UnitConfig agentUnitConfig : agentRegistryRemote.getAgentConfigs()) {
                // Skip disabled agents
                if (agentUnitConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED) {
                    itemEntryList.add(new AgentItemEntry(agentUnitConfig, null, locationRegistryRemote));
                }
            }

            for (UnitConfig appUnitConfig : appRegistryRemote.getAppConfigs()) {
                // Skip disabled apps
                if (appUnitConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED) {
                    itemEntryList.add(new AppItemEntry(appUnitConfig, locationRegistryRemote));
                }
            }

            // sort items by command type and label
            Collections.sort(itemEntryList);
            
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate item entries.", ex);
        }
    }

    private void serializeToFile() throws CouldNotPerformException {
        try {
            String configAsString = "";

            File configFile = JPService.getProperty(JPOpenHABItemConfig.class).getValue();

            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += "/* === BCO AUTO GENERATED GROUP ENTRIES ============== */" + System.lineSeparator();
            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += System.lineSeparator();
            for (GroupEntry entry : groupEntryList) {
                configAsString += entry.buildStringRep() + System.lineSeparator();
            }
            configAsString += System.lineSeparator();
            configAsString += System.lineSeparator();
            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += "/* === BCO AUTO GENERATED ITEM ENTRIES =============== */" + System.lineSeparator();
            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += System.lineSeparator();
            for (AbstractItemEntry entry : itemEntryList) {
                configAsString += entry.buildStringRep() + System.lineSeparator();
            }
            configAsString += System.lineSeparator();

            // TODO need to be tested!
            FileUtils.writeStringToFile(configFile, configAsString, Charset.forName("UTF8"), false);

            logger.info("ItemConfig[" + configFile.getAbsolutePath() + "] successfully generated.");
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not serialize itemconfig to file!", ex);
        }
    }

    public void shutdown() {

    }

    private boolean unitHasServiceAsOperationService(UnitConfig unitConfig, ServiceType serviceType) {
        return unitConfig.getServiceConfigList().stream().anyMatch((tmpServiceConfig) -> (tmpServiceConfig.getServiceDescription().getType() == serviceType
                && tmpServiceConfig.getServiceDescription().getPattern() == ServiceTemplate.ServicePattern.OPERATION));
    }
}
