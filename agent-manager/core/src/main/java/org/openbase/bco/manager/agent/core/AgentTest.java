package org.openbase.bco.manager.agent.core;

/*
 * #%L
 * COMA AgentManager Core
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

import static org.openbase.bco.manager.agent.core.AgentManagerLauncher.logger;
import org.openbase.bco.manager.agent.lib.jp.JPAgentId;
import org.openbase.bco.registry.agent.remote.AgentRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPVerbose;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;

/**
 *
 * * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a> Threepwood</a>
 */
public class AgentTest {

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.InstantiationException
     */
    public static void main(String[] args) throws InterruptedException, Exception {

        /* Setup JPService */
        JPService.setApplicationName(AgentTest.class);
        JPService.registerProperty(JPAgentId.class, "TestPersonLight");
        JPService.registerProperty(JPVerbose.class, true);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            AgentRegistryRemote agentRegistryRemote = new AgentRegistryRemote();
            agentRegistryRemote.init();
            agentRegistryRemote.activate();
            AgentFactoryImpl.getInstance().newInstance(agentRegistryRemote.getAgentConfigById(JPService.getProperty(JPAgentId.class).getValue())).enable();
            agentRegistryRemote.deactivate();
        } catch (JPServiceException | CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
