package org.dc.bco.manager.scene.binding.openhab;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.manager.scene.binding.openhab.transform.ActivationStateTransformer;
import org.dc.bco.manager.scene.remote.SceneRemote;
import org.dc.bco.registry.scene.remote.SceneRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPNotAvailableException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.VerificationFailedException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.openhab.binding.AbstractOpenHABBinding;
import org.dc.jul.extension.openhab.binding.AbstractOpenHABRemote;
import org.dc.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.dc.jul.pattern.ObservableImpl;
import org.dc.jul.pattern.Observer;
import org.dc.jul.pattern.Remote;
import org.dc.jul.storage.registry.RegistryImpl;
import org.dc.jul.storage.registry.RegistrySynchronizer;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneRegistryType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.homeautomation.state.EnablingStateType.EnablingState;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class SceneBindingOpenHABImpl extends AbstractOpenHABBinding {

    //TODO: Should be declared in the openhab config generator and used from there
    public static final String SCENE_MANAGER_ITEM_FILTER = "bco.manager.scene";

    private final SceneRegistryRemote sceneRegistryRemote;
    private final SceneRemoteFactoryImpl factory;
    private final RegistrySynchronizer<String, SceneRemote, SceneConfig, SceneConfig.Builder> registrySynchronizer;
    private final RegistryImpl<String, SceneRemote> registry;
    private final boolean hardwareSimulationMode;

    public SceneBindingOpenHABImpl() throws InstantiationException, JPNotAvailableException, InterruptedException {
        super();
        sceneRegistryRemote = new SceneRegistryRemote();
        registry = new RegistryImpl<>();
        factory = new SceneRemoteFactoryImpl();
        hardwareSimulationMode = JPService.getProperty(JPHardwareSimulationMode.class).getValue();

        this.registrySynchronizer = new RegistrySynchronizer<String, SceneRemote, SceneConfig, SceneConfig.Builder>(registry, sceneRegistryRemote.getSceneConfigRemoteRegistry(), factory) {

            @Override
            public boolean verifyConfig(final SceneConfig config) throws VerificationFailedException {
                return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
            }
        };

    }

    private String getSceneIdFromOpenHABItem(OpenhabCommand command) {
        return command.getItemBindingConfig().split(":")[1];
    }

    public void init() throws InitializationException, InterruptedException {
        init(SCENE_MANAGER_ITEM_FILTER, new AbstractOpenHABRemote(hardwareSimulationMode) {

            @Override
            public void internalReceiveUpdate(OpenhabCommand command) throws CouldNotPerformException {
                logger.debug("Ignore update for scene manager openhab binding.");
            }

            @Override
            public void internalReceiveCommand(OpenhabCommand command) throws CouldNotPerformException {
                try {

                    if (!command.hasOnOff() || !command.getOnOff().hasState()) {
                        throw new CouldNotPerformException("Command does not have an onOff value required for scenes");
                    }
                    logger.debug("Received command for scene [" + command.getItem() + "] from openhab");
                    registry.get(getSceneIdFromOpenHABItem(command)).setActivationState(ActivationStateTransformer.transform(command.getOnOff().getState()));
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Skip item update [" + command.getItem() + " = " + command.getOnOff() + "]!", ex);
                }
            }

        });
    }

    @Override
    public void init(String itemFilter, OpenHABRemote openHABRemote) throws InitializationException, InterruptedException {
        super.init(itemFilter, openHABRemote);
        try {
            factory.init(openHABRemote);
            sceneRegistryRemote.init();
            sceneRegistryRemote.activate();
            sceneRegistryRemote.waitForData();
            registrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }
}
