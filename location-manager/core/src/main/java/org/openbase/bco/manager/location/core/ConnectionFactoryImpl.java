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
import org.openbase.bco.manager.location.lib.ConnectionController;
import org.openbase.bco.manager.location.lib.ConnectionFactory;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.spatial.ConnectionConfigType.ConnectionConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionFactoryImpl implements ConnectionFactory {

    private static ConnectionFactoryImpl instance;

    public synchronized static ConnectionFactoryImpl getInstance() {
        if (instance == null) {
            instance = new ConnectionFactoryImpl();
        }
        return instance;
    }

    @Override
    public ConnectionController newInstance(ConnectionConfig config) throws InstantiationException, InterruptedException {
        ConnectionController connectionController;
        try {
            if (config == null) {
                throw new NotAvailableException("connectionConfig");
            }
            connectionController = new ConnectionControllerImpl(config);
            connectionController.init(config);
            return connectionController;
        } catch (InstantiationException | NotAvailableException | InitializationException ex) {
            throw new InstantiationException(LocationControllerImpl.class, config.getId(), ex);
        }
    }
}