package org.openbase.bco.manager.user.core;

/*
 * #%L
 * BCO Manager User Core
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

import com.google.protobuf.Message;
import org.openbase.bco.dal.control.layer.unit.AbstractBaseUnitController;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.bco.dal.lib.layer.service.operation.ActivityMultiStateOperationService;
import org.openbase.bco.dal.remote.action.RemoteActionPool;
import org.openbase.bco.manager.user.lib.UserController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.activity.ActivityConfigType.ActivityConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivityMultiStateType.ActivityMultiState;
import rst.domotic.state.GlobalPositionStateType.GlobalPositionState;
import rst.domotic.state.LocalPositionStateType.LocalPositionState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.PresenceStateType.PresenceState.State;
import rst.domotic.state.UserTransitStateType.UserTransitState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.user.UserDataType.UserData;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.*;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserControllerImpl extends AbstractBaseUnitController<UserData, UserData.Builder> implements UserController {

    public static final String NET_DEVICE_VARIABLE_IDENTIFIER = "NET_DEVICE";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PresenceState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivityMultiState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserTransitState.getDefaultInstance()));
    }

    private final Object netDeviceDetectorMapLock = new SyncObject("NetDeviceDetectorMapLock");
    private final Map<String, NetDeviceDetector> netDeviceDetectorMap;

    private boolean enabled;

    public UserControllerImpl() throws org.openbase.jul.exception.InstantiationException {
        super(UserControllerImpl.class, UserData.newBuilder());
        try {
            this.netDeviceDetectorMap = new HashMap<>();
            registerOperationService(ServiceType.ACTIVITY_MULTI_STATE_SERVICE, new ActivityMultiStateOperationServiceImpl(this));
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        MetaConfigVariableProvider variableProvider = new MetaConfigVariableProvider(LabelProcessor.getBestMatch(config.getLabel()), config.getMetaConfig());

        synchronized (netDeviceDetectorMapLock) {

            // shutdown and remove all existing detectors
            for (NetDeviceDetector detector : netDeviceDetectorMap.values()) {
                detector.shutdown();
            }
            netDeviceDetectorMap.clear();
            try {
                for (String netDevice : variableProvider.getValues(NET_DEVICE_VARIABLE_IDENTIFIER).values()) {
                    if (!netDeviceDetectorMap.containsKey(netDevice)) {
                        NetDeviceDetector netDeviceDetector = new NetDeviceDetector();
                        netDeviceDetector.init(netDevice);
                        netDeviceDetectorMap.put(netDevice, netDeviceDetector);
                        netDeviceDetector.addObserver((NetDeviceDetector source, Boolean reachable) -> {
                            synchronized (netDeviceDetectorMapLock) {
                                final PresenceState.Builder presenceState = TimestampProcessor.updateTimestampWithCurrentTime(PresenceState.newBuilder(), logger);
                                for (NetDeviceDetector detector : netDeviceDetectorMap.values()) {
                                    if (detector.isReachable()) {
                                        applyDataUpdate(presenceState.setValue(State.PRESENT).build(), ServiceType.PRESENCE_STATE_SERVICE);
                                        return;
                                    }
                                }
                                applyDataUpdate(presenceState.setValue(State.ABSENT).build(), ServiceType.PRESENCE_STATE_SERVICE);
                            }
                        });
                        if (isActive()) {
                            netDeviceDetector.activate();
                        }
                    }
                }
            } catch (NotAvailableException ex) {
                logger.debug("No net devices found for " + this);
            }
        }
        return super.applyConfigUpdate(config);
    }

    @Override
    public void enable() throws CouldNotPerformException, InterruptedException {
        enabled = true;
        activate();
    }

    @Override
    public void disable() throws CouldNotPerformException, InterruptedException {
        enabled = false;
        deactivate();
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        synchronized (netDeviceDetectorMapLock) {
            for (NetDeviceDetector detector : netDeviceDetectorMap.values()) {
                try {
                    detector.activate();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not activate  " + detector + "!", ex, logger);
                }
            }
        }
        super.activate();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        synchronized (netDeviceDetectorMapLock) {
            for (NetDeviceDetector detector : netDeviceDetectorMap.values()) {
                try {
                    detector.deactivate();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not deactivate " + detector + "!", ex, logger);
                }
            }
        }
        super.deactivate();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Future<ActionDescription> setActivityMultiState(final ActivityMultiState activityMultiState) throws CouldNotPerformException {
        return applyUnauthorizedAction(activityMultiState, ACTIVITY_MULTI_STATE_SERVICE);
    }

    @Override
    public Future<ActionDescription> setPresenceState(PresenceState presenceState) throws CouldNotPerformException {
        return applyUnauthorizedAction(presenceState, PRESENCE_STATE_SERVICE);
    }

    @Override
    public Future<ActionDescription> setUserTransitState(UserTransitState userTransitState) throws CouldNotPerformException {
        return applyUnauthorizedAction(userTransitState, USER_TRANSIT_STATE_SERVICE);
    }

    @Override
    protected void applyCustomDataUpdate(UserData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case USER_TRANSIT_STATE_SERVICE:
                updateLastWithCurrentState(PRESENCE_STATE_SERVICE, internalBuilder);

                switch (internalBuilder.getUserTransitState().getValue()) {
                    case LONG_TERM_ABSENT:
                    case SHORT_TERM_ABSENT:
                    case SOON_PRESENT:
                        internalBuilder.getPresenceStateBuilder().setValue(State.ABSENT);
                        break;
                    case LONG_TERM_PRESENT:
                    case SHORT_TERM_PRESENT:
                    case SOON_ABSENT:
                        internalBuilder.getPresenceStateBuilder().setValue(State.PRESENT);
                        break;
                }

                copyResponsibleAction(USER_TRANSIT_STATE_SERVICE, PRESENCE_STATE_SERVICE, internalBuilder);
                updateLocalPosition(internalBuilder);
                break;
            case PRESENCE_STATE_SERVICE:
                updateLocalPosition(internalBuilder);
                break;
            case LOCAL_POSITION_STATE_SERVICE:
                updateLastWithCurrentState(PRESENCE_STATE_SERVICE, internalBuilder);

                if (internalBuilder.getLocalPositionState().getLocationIdCount() == 0) {
                    internalBuilder.getPresenceStateBuilder().setValue(State.ABSENT);
                } else {
                    internalBuilder.getPresenceStateBuilder().setValue(State.PRESENT);
                }

                copyResponsibleAction(USER_TRANSIT_STATE_SERVICE, PRESENCE_STATE_SERVICE, internalBuilder);
                break;
        }
    }

    private void updateLocalPosition(final UserData.Builder internalBuilder) {
        switch (internalBuilder.getPresenceState().getValue()) {
            case ABSENT:
                updateLastWithCurrentState(LOCAL_POSITION_STATE_SERVICE, internalBuilder);
                internalBuilder.getLocalPositionStateBuilder().clearLocationId().clearPosition();
                copyResponsibleAction(PRESENCE_STATE_SERVICE, LOCAL_POSITION_STATE_SERVICE, internalBuilder);
                break;
            case PRESENT:
                if (internalBuilder.getLocalPositionState().getLocationIdCount() == 0) {
                    try {
                        updateLastWithCurrentState(LOCAL_POSITION_STATE_SERVICE, internalBuilder);
                        internalBuilder.getLocalPositionStateBuilder().addLocationId(Registries.getUnitRegistry().getRootLocationConfig().getId());
                        copyResponsibleAction(PRESENCE_STATE_SERVICE, LOCAL_POSITION_STATE_SERVICE, internalBuilder);
                    } catch (CouldNotPerformException ex) {
                        logger.warn("Could not update local position state location id because of user transit update", ex);
                    }
                }
                break;
        }
    }

    @Override
    public Future<ActionDescription> setGlobalPositionState(final GlobalPositionState globalPositionState) throws CouldNotPerformException {
        return applyUnauthorizedAction(globalPositionState, GLOBAL_POSITION_STATE_SERVICE);
    }

    @Override
    public Future<ActionDescription> setLocalPositionState(LocalPositionState localPositionState) throws CouldNotPerformException {
        return applyUnauthorizedAction(localPositionState, LOCAL_POSITION_STATE_SERVICE);
    }

    private class NetDeviceDetector extends ObservableImpl<NetDeviceDetector, Boolean> implements Manageable<String> {

        private static final int REACHABLE_TIMEOUT = 5000;
        private static final int REQUEST_PERIOD = 60000;

        private String hostName;
        private Future detectorTask;
        private boolean reachable;

        @Override
        public void init(final String hostName) throws InitializationException, InterruptedException {
            this.hostName = hostName;
        }

        @Override
        public void activate() throws CouldNotPerformException, InterruptedException {
            detectorTask = GlobalScheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    reachable = checkIfReachable();
                    notifyObservers(reachable);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not inform observer about reachable state change!", ex, logger);
                }
            }, 0, REQUEST_PERIOD, TimeUnit.MILLISECONDS);
        }

        @Override
        public void deactivate() throws CouldNotPerformException, InterruptedException {
            detectorTask.cancel(false);
        }

        @Override
        public boolean isActive() {
            return detectorTask != null && !detectorTask.isDone();
        }

        public String getHostName() {
            return hostName;
        }

        public boolean checkIfReachable() {
            try {
                return InetAddress.getByName(hostName).isReachable(REACHABLE_TIMEOUT);
            } catch (IOException ex) {
                ExceptionPrinter.printHistory(new NotAvailableException(hostName + " is not reachable!", ex), logger);
                return false;
            }
        }

        public boolean isReachable() {
            return reachable;
        }

        @Override
        public void shutdown() {
            try {
                deactivate();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not shutdown " + this, ex, LoggerFactory.getLogger(getClass()));
            }
            super.shutdown();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[host:" + hostName + "]";
        }
    }

    public class ActivityMultiStateOperationServiceImpl implements ActivityMultiStateOperationService {

        private final Map<String, RemoteActionPool> remoteActionPoolMap;
        private UserController userController;

        public ActivityMultiStateOperationServiceImpl(final UserController userController) {
            this.userController = userController;
            this.remoteActionPoolMap = new HashMap<>();
        }

        @Override
        public Future<ActionDescription> setActivityMultiState(ActivityMultiState activityMultiState) {
            logger.info("Update activity list[" + activityMultiState.getActivityIdCount() + "]" + this);
            return GlobalScheduledExecutorService.submit(() -> {
                try {
                    if (activityMultiState.getActivityIdCount() == 0) {
                        for (Entry<String, RemoteActionPool> stringRemoteActionPoolEntry : remoteActionPoolMap.entrySet()) {
                            stringRemoteActionPoolEntry.getValue().stop();
                        }
                    } else if ((activityMultiState.getActivityIdCount() > 0)) {
                        final String activityId = activityMultiState.getActivityId(0);
                        for (Entry<String, RemoteActionPool> stringRemoteActionPoolEntry : remoteActionPoolMap.entrySet()) {
                            if (!stringRemoteActionPoolEntry.getKey().equals(activityId)) {
                                stringRemoteActionPoolEntry.getValue().stop();
                            }
                        }
                        if (!remoteActionPoolMap.containsKey(activityId)) {
                            final RemoteActionPool remoteActionPool = new RemoteActionPool(UserControllerImpl.this);
                            remoteActionPoolMap.put(activityId, remoteActionPool);
                            final ActivityConfig activityConfig = Registries.getActivityRegistry().getActivityConfigById(activityId);
                            remoteActionPool.initViaServiceStateDescription(activityConfig.getServiceStateDescriptionList());
                        }
                        remoteActionPoolMap.get(activityId).execute(activityMultiState.getResponsibleAction());
                    }

                    applyDataUpdate(activityMultiState.toBuilder().setTimestamp(TimestampProcessor.getCurrentTimestamp()).build(), ServiceType.ACTIVITY_MULTI_STATE_SERVICE);
                    return null;
                } catch (Exception ex) {
                    throw new CouldNotPerformException("Could not update activity state of " + this, ex);
                }
            });
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return UserControllerImpl.this;
        }
    }
}
