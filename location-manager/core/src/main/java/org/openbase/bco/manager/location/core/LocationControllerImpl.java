package org.openbase.bco.manager.location.core;

/*
 * #%L
 * COMA LocationManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.MotionStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.PowerConsumptionStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.SmokeAlarmStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.SmokeStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.TamperStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.TemperatureStateProviderService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactory;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactoryImpl;
import org.openbase.bco.manager.location.lib.Location;
import org.openbase.bco.manager.location.lib.LocationController;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.action.ActionConfigType.ActionConfig;
import rst.homeautomation.control.action.SnapshotType.Snapshot;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.AlarmStateType;
import rst.homeautomation.state.AlarmStateType.AlarmState;
import rst.homeautomation.state.BlindStateType.BlindState;
import rst.homeautomation.state.BrightnessStateType.BrightnessState;
import rst.homeautomation.state.ColorStateType.ColorState;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.state.PowerConsumptionStateType;
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.state.SmokeStateType.SmokeState;
import rst.homeautomation.state.StandbyStateType.StandbyState;
import rst.homeautomation.state.TamperStateType.TamperState;
import rst.homeautomation.state.TemperatureStateType.TemperatureState;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationDataType.LocationData;
import rst.vision.ColorType.Color;
import rst.vision.HSBColorType.HSBColor;
import rst.vision.RGBColorType.RGBColor;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a> Threepwood</a>
 */
public class LocationControllerImpl extends AbstractConfigurableController<LocationData, LocationData.Builder, LocationConfig> implements LocationController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BlindState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureState.getDefaultInstance()));
    }

    private final UnitRemoteFactory factory;
    private final Map<String, UnitRemote> unitRemoteMap;
    private final Map<ServiceType, Collection<? extends Service>> serviceMap;
    private List<String> originalUnitIdList;

    public LocationControllerImpl(final LocationConfig config) throws InstantiationException {
        super(LocationData.newBuilder());
        this.factory = UnitRemoteFactoryImpl.getInstance();
        this.unitRemoteMap = new HashMap<>();
        this.serviceMap = new HashMap<>();
        originalUnitIdList = new ArrayList<>();
    }

    private boolean isSupportedServiceType(final ServiceType serviceType) {
        switch (serviceType) {
            case BRIGHTNESS_STATE_SERVICE:
            case COLOR_STATE_SERVICE:
            case MOTION_STATE_SERVICE:
            case POWER_CONSUMPTION_STATE_SERVICE:
            case POWER_STATE_SERVICE:
            case BLIND_STATE_SERVICE:
            case SMOKE_ALARM_STATE_SERVICE:
            case SMOKE_STATE_SERVICE:
            case STANDBY_STATE_SERVICE:
            case TAMPER_STATE_SERVICE:
            case TARGET_TEMPERATURE_STATE_SERVICE:
            case TEMPERATURE_STATE_SERVICE:
                return true;
            default:
                return false;
        }
    }

    private boolean isSupportedServiceType(final List<ServiceTemplate> serviceTemplates) {
        return serviceTemplates.stream().anyMatch((serviceTemplate) -> (isSupportedServiceType(serviceTemplate.getType())));
    }

    private void addRemoteToServiceMap(final ServiceType serviceType, final UnitRemote unitRemote) {
        //TODO: should be replaced with generic class loading
        // and the update can be realized with reflections or the setField method and a notify
        switch (serviceType) {
            case BRIGHTNESS_STATE_SERVICE:
                ((ArrayList<BrightnessStateOperationService>) serviceMap.get(ServiceType.BRIGHTNESS_STATE_SERVICE)).add((BrightnessStateOperationService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        BrightnessState brightness = getBrightnessState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setBrightnessState(brightness);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply brightness data change!", ex);
                        }
                    }
                });
                break;
            case COLOR_STATE_SERVICE:
                ((ArrayList<ColorStateOperationService>) serviceMap.get(ServiceType.COLOR_STATE_SERVICE)).add((ColorStateOperationService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        ColorState color = getColorState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setColorState(color);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply color data change!", ex);
                        }
                    }
                });
                break;
            case MOTION_STATE_SERVICE:
                ((ArrayList<MotionStateProviderService>) serviceMap.get(ServiceType.MOTION_STATE_SERVICE)).add((MotionStateProviderService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        MotionState motion = getMotionState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setMotionState(motion);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply motion data change!", ex);
                        }
                    }
                });
                break;
            case POWER_CONSUMPTION_STATE_SERVICE:
                ((ArrayList<PowerConsumptionStateProviderService>) serviceMap.get(ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).add((PowerConsumptionStateProviderService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        PowerConsumptionStateType.PowerConsumptionState powerConsumption = getPowerConsumptionState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setPowerConsumptionState(powerConsumption);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply power consumption data change!", ex);
                        }
//                        logger.info("Received powerConsumption[" + powerConsumption.getConsumption() + "] update for location [" + getLabel() + "]");
                    }
                });
                break;
            case POWER_STATE_SERVICE:
                ((ArrayList<PowerStateOperationService>) serviceMap.get(ServiceType.POWER_STATE_SERVICE)).add((PowerStateOperationService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        PowerState powerState = getPowerState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setPowerState(powerState);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply power data change!", ex);
                        }
                    }
                });
                break;
            case BLIND_STATE_SERVICE:
                ((ArrayList<BlindStateOperationService>) serviceMap.get(ServiceType.BLIND_STATE_SERVICE)).add((BlindStateOperationService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        BlindState blindState = getBlindState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setBlindState(blindState);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply shutter data change!", ex);
                        }
                    }
                });
                break;
            case SMOKE_ALARM_STATE_SERVICE:
                ((ArrayList<SmokeAlarmStateProviderService>) serviceMap.get(ServiceType.SMOKE_ALARM_STATE_SERVICE)).add((SmokeAlarmStateProviderService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        AlarmStateType.AlarmState smokeAlarmState = getSmokeAlarmState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setSmokeAlarmState(smokeAlarmState);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply smoke alarm state data change!", ex);
                        }
                    }
                });
                break;
            case SMOKE_STATE_SERVICE:
                ((ArrayList<SmokeStateProviderService>) serviceMap.get(ServiceType.SMOKE_STATE_SERVICE)).add((SmokeStateProviderService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        SmokeState smokeState = getSmokeState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setSmokeState(smokeState);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply smoke data change!", ex);
                        }
                    }
                });
                break;
            case STANDBY_STATE_SERVICE:
                ((ArrayList<StandbyStateOperationService>) serviceMap.get(ServiceType.STANDBY_STATE_SERVICE)).add((StandbyStateOperationService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        StandbyState standby = getStandbyState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setStandbyState(standby);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply standby data change!", ex);
                        }
                    }
                });
                break;
            case TAMPER_STATE_SERVICE:
                ((ArrayList<TamperStateProviderService>) serviceMap.get(ServiceType.TAMPER_STATE_SERVICE)).add((TamperStateProviderService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        TamperState tamper = getTamperState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setTamperState(tamper);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply tamper data change!", ex);
                        }
                    }
                });
                break;
            case TARGET_TEMPERATURE_STATE_SERVICE:
                ((ArrayList<TargetTemperatureStateOperationService>) serviceMap.get(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).add((TargetTemperatureStateOperationService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        TemperatureState targetTemperature = getTargetTemperatureState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setTargetTemperatureState(targetTemperature);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply target temperature data change!", ex);
                        }
                    }
                });
                break;
            case TEMPERATURE_STATE_SERVICE:
                ((ArrayList<TemperatureStateProviderService>) serviceMap.get(ServiceType.TEMPERATURE_STATE_SERVICE)).add((TemperatureStateProviderService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        TemperatureState temperature = getTemperatureState();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setAcutalTemperatureState(temperature);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply temperature data change!", ex);
                        }
                    }
                });
                break;
        }
    }

    @Override
    public void init(final LocationConfig config) throws InitializationException, InterruptedException {
        logger.debug("Init location [" + config.getLabel() + "]");
        try {
            originalUnitIdList = config.getUnitIdList();
            for (ServiceType serviceType : ServiceType.values()) {
                if (isSupportedServiceType(serviceType)) {
                    serviceMap.put(serviceType, new ArrayList<>());
                }
            }
            DeviceRegistry deviceRegistry = LocationManagerController.getInstance().getDeviceRegistry();
            for (UnitConfig unitConfig : deviceRegistry.getUnitConfigs()) {
                if (config.getUnitIdList().contains(unitConfig.getId())) {
                    List<ServiceTemplate> serviceTemplates = deviceRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTemplateList();

                    // ignore units that do not have any service supported by a location
                    if (!isSupportedServiceType(serviceTemplates)) {
                        continue;
                    }

                    UnitRemote unitRemote = factory.newInitializedInstance(unitConfig);
                    unitRemoteMap.put(unitConfig.getId(), unitRemote);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(config);
    }

    @Override
    public LocationConfig applyConfigUpdate(final LocationConfig config) throws CouldNotPerformException, InterruptedException {
        List<String> newUnitIdList = new ArrayList<>(config.getUnitIdList());
        for (String originalId : originalUnitIdList) {
            if (config.getUnitIdList().contains(originalId)) {
                newUnitIdList.remove(originalId);
            } else {
                unitRemoteMap.get(originalId).deactivate();
                unitRemoteMap.remove(originalId);
                for (Collection<? extends Service> serviceCollection : serviceMap.values()) {
                    Collection serviceSelectionCopy = new ArrayList<>(serviceCollection);
                    for (Object service : serviceSelectionCopy) {
                        if (((UnitRemote) service).getId().equals(originalId)) {
                            serviceCollection.remove(service);
                        }
                    }
                }
            }
        }
        for (String newUnitId : newUnitIdList) {
            DeviceRegistry deviceRegistry = LocationManagerController.getInstance().getDeviceRegistry();
            UnitConfig unitConfig = deviceRegistry.getUnitConfigById(newUnitId);
            List<ServiceTemplate> serviceTemplates = new ArrayList<>();

            // ignore units that do not have any service supported by a location
            if (!isSupportedServiceType(serviceTemplates)) {
                continue;
            }

            UnitRemote unitRemote = factory.newInitializedInstance(unitConfig);
            unitRemoteMap.put(unitConfig.getId(), unitRemote);
            if (isActive()) {
                for (ServiceTemplate serviceTemplate : serviceTemplates) {
                    addRemoteToServiceMap(serviceTemplate.getType(), unitRemote);
                }
                unitRemote.activate();
            }
        }
        if (isActive()) {
            getCurrentStatus();
        }
        originalUnitIdList = config.getUnitIdList();
        return super.applyConfigUpdate(config);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        logger.debug("Activate location [" + getLabel() + "]!");
        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            unitRemote.activate();
        }
        super.activate();

        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            for (ServiceTemplate serviceTemplate : LocationManagerController.getInstance().getDeviceRegistry().getUnitTemplateByType(unitRemote.getType()).getServiceTemplateList()) {
                addRemoteToServiceMap(serviceTemplate.getType(), unitRemote);
            }
        }
        getCurrentStatus();
    }

    private void getCurrentStatus() {
        try {
            BrightnessState brighntess = getBrightnessState();
            ColorState color = getColorState();
            MotionState motion = getMotionState();
            PowerState power = getPowerState();
            PowerConsumptionStateType.PowerConsumptionState powerConsumption = getPowerConsumptionState();
            BlindState blindState = getBlindState();
            AlarmState smokeAlarmState = getSmokeAlarmState();
            SmokeState smokeState = getSmokeState();
            StandbyState standby = getStandbyState();
            TamperState tamper = getTamperState();
            TemperatureState targetTemperature = getTargetTemperatureState();
            TemperatureState temperature = getTemperatureState();
            try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                dataBuilder.getInternalBuilder().setBrightnessState(brighntess);
                dataBuilder.getInternalBuilder().setColorState(color);
                dataBuilder.getInternalBuilder().setMotionState(motion);
                dataBuilder.getInternalBuilder().setPowerState(power);
                dataBuilder.getInternalBuilder().setPowerConsumptionState(powerConsumption);
                dataBuilder.getInternalBuilder().setBlindState(blindState);
                dataBuilder.getInternalBuilder().setSmokeAlarmState(smokeAlarmState);
                dataBuilder.getInternalBuilder().setSmokeState(smokeState);
                dataBuilder.getInternalBuilder().setStandbyState(standby);
                dataBuilder.getInternalBuilder().setTamperState(tamper);
                dataBuilder.getInternalBuilder().setTargetTemperatureState(targetTemperature);
                dataBuilder.getInternalBuilder().setAcutalTemperatureState(temperature);
            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not apply data change!", ex);
            }
        } catch (CouldNotPerformException ex) {
            logger.warn("Could not get current status", ex);
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            unitRemote.deactivate();
        }
    }

    @Override
    public void registerMethods(RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Location.class, this, server);
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return getConfig().getLabel();
    }

    @Override
    public Collection<ColorStateOperationService> getColorStateOperationServices() {
        return (Collection<ColorStateOperationService>) serviceMap.get(ServiceType.COLOR_STATE_SERVICE);
    }

    @Override
    public Collection<BrightnessStateOperationService> getBrightnessStateOperationServices() {
        return (Collection<BrightnessStateOperationService>) serviceMap.get(ServiceType.BRIGHTNESS_STATE_SERVICE);
    }

    @Override
    public Collection<PowerStateOperationService> getPowerStateOperationServices() {
        return (Collection<PowerStateOperationService>) serviceMap.get(ServiceType.POWER_STATE_SERVICE);
    }

    @Override
    public Collection<BlindStateOperationService> getBlindStateOperationServices() {
        return (Collection<BlindStateOperationService>) serviceMap.get(ServiceType.BLIND_STATE_SERVICE);
    }

    @Override
    public Collection<StandbyStateOperationService> getStandbyStateOperationServices() {
        return (Collection<StandbyStateOperationService>) serviceMap.get(ServiceType.STANDBY_STATE_SERVICE);
    }

    @Override
    public Collection<TargetTemperatureStateOperationService> getTargetTemperatureStateOperationServices() {
        return (Collection<TargetTemperatureStateOperationService>) serviceMap.get(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
    }

    @Override
    public Collection<MotionStateProviderService> getMotionStateProviderServices() {
        return (Collection<MotionStateProviderService>) serviceMap.get(ServiceType.MOTION_STATE_SERVICE);
    }

    @Override
    public Collection<SmokeAlarmStateProviderService> getSmokeAlarmStateProviderServices() {
        return (Collection<SmokeAlarmStateProviderService>) serviceMap.get(ServiceType.SMOKE_ALARM_STATE_SERVICE);
    }

    @Override
    public Collection<SmokeStateProviderService> getSmokeStateProviderServices() {
        return (Collection<SmokeStateProviderService>) serviceMap.get(ServiceType.SMOKE_STATE_SERVICE);
    }

    @Override
    public Collection<TemperatureStateProviderService> getTemperatureStateProviderServices() {
        return (Collection<TemperatureStateProviderService>) serviceMap.get(ServiceType.TEMPERATURE_STATE_SERVICE);
    }

    @Override
    public Collection<PowerConsumptionStateProviderService> getPowerConsumptionStateProviderServices() {
        return (Collection<PowerConsumptionStateProviderService>) serviceMap.get(ServiceType.POWER_CONSUMPTION_STATE_SERVICE);
    }

    @Override
    public Collection<TamperStateProviderService> getTamperStateProviderServices() {
        return (Collection<TamperStateProviderService>) serviceMap.get(ServiceType.TAMPER_STATE_SERVICE);
    }

    @Override
    public Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        try {
            Snapshot.Builder snapshotBuilder = Snapshot.newBuilder();
            for (UnitRemote remote : unitRemoteMap.values()) {
                snapshotBuilder.addAllActionConfig(remote.recordSnapshot().get().getActionConfigList());
            }
            return CompletableFuture.completedFuture(snapshotBuilder.build());
        } catch (final ExecutionException | CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot!", ex);
        }
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        try {
            for (final ActionConfig actionConfig : snapshot.getActionConfigList()) {
                unitRemoteMap.get(actionConfig.getUnitId()).applyAction(actionConfig);
            }
            return CompletableFuture.completedFuture(null);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot!", ex);
        }
    }

    @Override
    public Future<Void> applyAction(final ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        return unitRemoteMap.get(actionConfig.getUnitId()).applyAction(actionConfig);
    }

    @Override
    public List<String> getNeighborLocationIds() throws CouldNotPerformException {
        List<String> neighborIdList = new ArrayList<>();
        for (LocationConfig locationConfig : LocationManagerController.getInstance().getLocationRegistry().getNeighborLocations(getId())) {
            neighborIdList.add(locationConfig.getId());
        }
        return neighborIdList;
    }
}
