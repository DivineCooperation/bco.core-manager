package org.openbase.bco.manager.agent.core;

/*
 * #%L
 * BCO Manager Agent Core
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
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */

import org.openbase.bco.manager.agent.lib.AgentController;
import org.openbase.bco.manager.agent.lib.AgentControllerFactory;
import org.openbase.bco.manager.agent.lib.AgentManager;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.ControllerRegistryImpl;
import org.openbase.jul.storage.registry.EnableableEntryRegistrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;

import java.util.concurrent.TimeUnit;

public class AgentManagerController implements AgentManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AgentManagerController.class);

    private final AgentControllerFactory factory;
    private final ControllerRegistryImpl<String, AgentController> agentRegistry;
    private final ActivatableEntryRegistrySynchronizer<String, AgentController, UnitConfig, Builder> agentRegistrySynchronizer;

    public AgentManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.factory = AgentControllerFactoryImpl.getInstance();
            this.agentRegistry = new ControllerRegistryImpl<>();

            this.agentRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, AgentController, UnitConfig, UnitConfig.Builder>(agentRegistry, Registries.getUnitRegistry().getAgentUnitConfigRemoteRegistry(), Registries.getUnitRegistry(), factory) {

                @Override
                public boolean activationCondition(UnitConfig config) {
                    return UnitConfigProcessor.isEnabled(config);
                }
            };

        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() {
        // this has to stay, else do not implement VoidInitializable
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        BCOLogin.loginBCOUser();

        agentRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return agentRegistrySynchronizer.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        agentRegistrySynchronizer.deactivate();
    }

    @Override
    public void shutdown() {
        agentRegistrySynchronizer.shutdown();
    }

    @Override
    public void waitForInit(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        Registries.getUnitRegistry().waitForData(timeout, timeUnit);
    }
}
