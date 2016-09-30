package org.openbase.bco.manager.user.core;

/*
 * #%L
 * COMA UserManager Core
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

import org.openbase.bco.manager.user.lib.UserController;
import org.openbase.bco.manager.user.lib.UserFactory;
import org.openbase.bco.manager.user.lib.UserManager;
import org.openbase.bco.registry.user.lib.UserRegistry;
import org.openbase.bco.registry.user.lib.provider.UserRegistryProvider;
import org.openbase.bco.registry.user.remote.UserRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.storage.registry.ControllerRegistry;
import org.openbase.jul.storage.registry.EnableableEntryRegistrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.authorization.UserConfigType;
import rst.homeautomation.state.EnablingStateType.EnablingState;

/**
 *
 * * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserManagerController implements UserRegistryProvider, UserManager {

    protected static final Logger logger = LoggerFactory.getLogger(UserManagerController.class);

    private static UserManagerController instance;
    private final UserFactory factory;
    private final ControllerRegistry<String, UserController> userRegistry;
    private final UserRegistryRemote userRegistryRemote;
    private final EnableableEntryRegistrySynchronizer<String, UserController, UserConfigType.UserConfig, UserConfigType.UserConfig.Builder> registrySynchronizer;

    public UserManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.instance = this;
            this.factory = UserFactoryImpl.getInstance();
            this.userRegistry = new ControllerRegistry<>();
            this.userRegistryRemote = new UserRegistryRemote();

            this.registrySynchronizer = new EnableableEntryRegistrySynchronizer<String, UserController, UserConfigType.UserConfig, UserConfigType.UserConfig.Builder>(userRegistry, userRegistryRemote.getUserConfigRemoteRegistry(), factory) {

                @Override
                public boolean enablingCondition(UserConfigType.UserConfig config) {
                    return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
                }
            };

        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    public static UserManagerController getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(UserManagerController.class);
        }
        return instance;
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            this.userRegistryRemote.init();
            this.userRegistryRemote.activate();
            this.userRegistryRemote.waitForData();
            this.registrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        this.userRegistryRemote.shutdown();
        instance = null;
    }

    @Override
    public UserRegistry getUserRegistry() throws NotAvailableException {
        return userRegistryRemote;
    }
}
