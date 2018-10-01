package org.openbase.bco.manager.scene.core;

/*
 * #%L
 * BCO Manager Scene Core
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

import org.openbase.bco.dal.control.layer.unit.AbstractExecutableBaseUnitController;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ButtonRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.manager.scene.lib.SceneController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.SyncObject;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionDescriptionType;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.ButtonStateType.ButtonState;
import rst.domotic.state.ButtonStateType.ButtonState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.dal.ButtonDataType.ButtonData;
import rst.domotic.unit.scene.SceneDataType.SceneData;

import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UnitConfig
 */
public class SceneControllerImpl extends AbstractExecutableBaseUnitController<SceneData, SceneData.Builder> implements SceneController {

    public static final long ACTION_EXECUTION_TIMEOUT = 15000;

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescriptionType.ActionDescription.getDefaultInstance()));
    }

    private final Object buttonObserverLock = new SyncObject("ButtonObserverLock");
    private final Set<ButtonRemote> buttonRemoteSet;
    private final List<RemoteAction> remoteActionList;
    private final SyncObject actionListSync = new SyncObject("ActionListSync");
    private final Observer<DataProvider<ButtonData>, ButtonData> buttonObserver;

    public SceneControllerImpl() throws org.openbase.jul.exception.InstantiationException {
        super(SceneControllerImpl.class, SceneData.newBuilder());
        this.buttonRemoteSet = new HashSet<>();
        this.remoteActionList = new ArrayList<>();
        this.buttonObserver = (final DataProvider<ButtonData> source, ButtonData data) -> {

            // skip initial button state synchronization during system startup
            if (data.getButtonStateLast().getValue().equals(State.UNKNOWN)) {
                return;
            }

            if (data.getButtonState().getValue().equals(ButtonState.State.PRESSED)) {
                setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build());
            }
        };
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            Registries.waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(config);
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        config = super.applyConfigUpdate(config);

        try {
            synchronized (buttonObserverLock) {
                for (final ButtonRemote button : buttonRemoteSet) {
                    try {
                        logger.info("update: remove " + LabelProcessor.getBestMatch(getConfig().getLabel()) + " for button  " + button.getLabel());
                    } catch (NotAvailableException ex) {
                        Logger.getLogger(SceneControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    button.removeDataObserver(buttonObserver);
                }

                buttonRemoteSet.clear();
                ButtonRemote buttonRemote;

                for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType(LabelProcessor.getBestMatch(config.getLabel()), UnitType.BUTTON)) {
                    try {
                        buttonRemote = Units.getUnit(unitConfig, false, Units.BUTTON);
                        buttonRemoteSet.add(buttonRemote);
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register remote for Button[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "]!", ex), logger);
                    }
                }
                if (isActive()) {
                    for (final ButtonRemote button : buttonRemoteSet) {
                        try {
                            logger.info("update: register " + LabelProcessor.getBestMatch(getConfig().getLabel()) + " for button  " + button.getLabel());
                        } catch (NotAvailableException ex) {
                            Logger.getLogger(SceneControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        button.addDataObserver(buttonObserver);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not init all related button remotes.", ex), logger);
        }

        MultiException.ExceptionStack exceptionStack = null;

        synchronized (actionListSync) {
            remoteActionList.clear();
            RemoteAction action;
            for (ServiceStateDescription serviceStateDescription : config.getSceneConfig().getRequiredServiceStateDescriptionList()) {
                action = new RemoteAction();
                try {
                    action.init(ActionDescription.newBuilder().setServiceStateDescription(serviceStateDescription).build());
                    remoteActionList.add(action);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }

            for (ServiceStateDescription serviceStateDescription : config.getSceneConfig().getOptionalServiceStateDescriptionList()) {
                action = new RemoteAction();
                try {
                    action.init(ActionDescription.newBuilder().setServiceStateDescription(serviceStateDescription).build());
                    remoteActionList.add(action);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
        }

        try {
            MultiException.checkAndThrow(() ->"Could not fully init units of " + this, exceptionStack);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
        }
        return config;
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        synchronized (buttonObserverLock) {
            buttonRemoteSet.stream().forEach((button) -> {
                button.addDataObserver(buttonObserver);
            });
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        logger.debug("deactivate " + LabelProcessor.getBestMatch(getConfig().getLabel()));
        synchronized (buttonObserverLock) {
            buttonRemoteSet.stream().forEach((button) -> {
                button.removeDataObserver(buttonObserver);
            });
        }
        super.deactivate();
    }

    @Override
    protected void execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.info("Activate Scene[" + LabelProcessor.getBestMatch(getConfig().getLabel()) + "]");

        final Map<Future<ActionDescription>, RemoteAction> executionFutureList = new HashMap<>();

        synchronized (actionListSync) {
            for (final RemoteAction action : remoteActionList) {
                executionFutureList.put(action.execute(), action);
            }
        }
        MultiException.ExceptionStack exceptionStack = null;

        try {
            logger.info("Waiting for action finalisation...");

            long checkStart = System.currentTimeMillis() + ACTION_EXECUTION_TIMEOUT;
            long timeout;
            for (Entry<Future<ActionDescription>, RemoteAction> futureActionEntry : executionFutureList.entrySet()) {
                if (futureActionEntry.getKey().isDone()) {
                    continue;
                }
                logger.info("Waiting for action [" + futureActionEntry.getValue().getActionDescription().getServiceStateDescription().getServiceAttributeType() + "]");
                try {
                    timeout = checkStart - System.currentTimeMillis();
                    if (timeout <= 0) {
                        throw new RejectedException("Rejected because of scene timeout.");
                    }
                    futureActionEntry.getKey().get(timeout, TimeUnit.MILLISECONDS);
                } catch (ExecutionException | TimeoutException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
            MultiException.checkAndThrow(() ->"Could not execute all actions!", exceptionStack);
            logger.info("Deactivate Scene[" + getLabel() + "] because all actions are successfully executed.");
        } catch (CouldNotPerformException | CancellationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Scene[" + getLabel() + "] execution failed!", ex), logger);
        } finally {
            for (Entry<Future<ActionDescription>, RemoteAction> futureActionEntry : executionFutureList.entrySet()) {
                if (!futureActionEntry.getKey().isDone()) {
                    futureActionEntry.getKey().cancel(true);
                }
            }
            applyDataUpdate(ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).setTimestamp(TimestampProcessor.getCurrentTimestamp()).build(), ServiceType.ACTIVATION_STATE_SERVICE);
        }
    }

    @Override
    protected void stop(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.debug("Finished scene: " + getLabel());
    }

    @Override
    protected boolean isAutostartEnabled() throws CouldNotPerformException {
        return false;
    }
}
