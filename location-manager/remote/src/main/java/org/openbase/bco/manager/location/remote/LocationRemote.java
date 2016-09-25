package org.openbase.bco.manager.location.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.manager.location.lib.Location;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableRemote;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.action.ActionConfigType;
import rst.homeautomation.control.action.SnapshotType.Snapshot;
import rst.homeautomation.state.AlarmStateType.AlarmState;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.state.BlindStateType.BlindState;
import rst.homeautomation.state.BrightnessStateType.BrightnessState;
import rst.homeautomation.state.ColorStateType.ColorState;
import rst.homeautomation.state.SmokeStateType.SmokeState;
import rst.homeautomation.state.StandbyStateType.StandbyState;
import rst.homeautomation.state.TamperStateType.TamperState;
import rst.homeautomation.state.TemperatureStateType.TemperatureState;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationDataType.LocationData;
import rst.vision.ColorType.Color;
import rst.vision.HSBColorType.HSBColor;
import rst.vision.RGBColorType.RGBColor;

/*
 * #%L
 * COMA LocationManager Remote
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationRemote extends AbstractConfigurableRemote<LocationData, LocationConfig> implements Location {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BlindState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureState.getDefaultInstance()));
    }

    public LocationRemote() {
        super(LocationData.class, LocationConfig.class);
    }

    @Override
    public void notifyDataUpdate(LocationData data) throws CouldNotPerformException {
    }

    @Override
    public String getLabel() throws NotAvailableException {
        try {
            if (config == null) {
                throw new NotAvailableException("locationConfig");
            }
            return config.getLabel();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("label", ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        CachedLocationRegistryRemote.waitForData();
        super.activate();
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessState brightness) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(brightness, this, Void.class);
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        try {
            return getData().getBrightnessState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Brightness", ex);
        }
    }

    @Override
    public Future<Void> setColorState(ColorState color) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(color, this, Void.class);
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        try {
            return getData().getColorState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Color", ex);
        }
    }

    public void setPowerState(PowerState.State state) throws CouldNotPerformException {
        LocationRemote.this.setPowerState(PowerState.newBuilder().setValue(state).build());
    }

    @Override
    public Future<Void> setPowerState(PowerState state) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(state, this, Void.class);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerState", ex);
        }
    }

    @Override
    public Future<Void> setBlindState(BlindState state) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(state, this, Void.class);
    }

    @Override
    public BlindState getBlindState() throws NotAvailableException {
        try {
            return getData().getBlindState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ShutterState", ex);
        }
    }

    @Override
    public Future<Void> setStandbyState(StandbyState state) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(state, this, Void.class);
    }

    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        try {
            return getData().getStandbyState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("StandbyState", ex);
        }
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureState value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        try {
            return getData().getTargetTemperatureState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TargetTemperature", ex);
        }
    }

    @Override
    public MotionState getMotionState() throws NotAvailableException {
        try {
            return getData().getMotionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("MotionState", ex);
        }
    }

    @Override
    public AlarmState getSmokeAlarmState() throws NotAvailableException {
        try {
            return getData().getSmokeAlarmState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("AlarmState", ex);
        }
    }

    @Override
    public SmokeState getSmokeState() throws NotAvailableException {
        try {
            return getData().getSmokeState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SmokeState", ex);
        }
    }

    @Override
    public TemperatureState getTemperatureState() throws NotAvailableException {
        try {
            return getData().getAcutalTemperatureState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Temperature", ex);
        }
    }

    @Override
    public PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        try {
            return getData().getPowerConsumptionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerConsumptionState", ex);
        }
    }

    @Override
    public TamperState getTamperState() throws NotAvailableException {
        try {
            return getData().getTamperState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TamperState", ex);
        }
    }

    @Override
    public Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(this, Snapshot.class);
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(snapshot, this, Void.class);
    }

    @Override
    public Future<Void> applyAction(ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(actionConfig, this, Void.class);
    }

    @Override
    public List<String> getNeighborLocationIds() throws CouldNotPerformException {
        List<String> neighborIdList = new ArrayList<>();
        try {
            for (LocationConfig locationConfig : CachedLocationRegistryRemote.getRegistry().getNeighborLocations(getId())) {
                neighborIdList.add(locationConfig.getId());
            }
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Could not get CachedLocationRegistryRemote!", ex);
        }
        return neighborIdList;
    }
}
