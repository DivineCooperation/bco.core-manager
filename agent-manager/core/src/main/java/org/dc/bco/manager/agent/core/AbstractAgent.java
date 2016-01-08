/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core;

import org.dc.bco.manager.agent.lib.Agent;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.agent.AgentConfigType;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public abstract class AbstractAgent implements Agent  {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private AgentConfigType.AgentConfig config;

    public AbstractAgent(AgentConfigType.AgentConfig config) {
        this.config = config;
        logger.info("Create agent:" + config.getLabel());
    }

    @Override
    public AgentConfigType.AgentConfig getConfig() throws NotAvailableException {
        return config;
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activate agent:" + config.getLabel());
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivate agent:" + config.getLabel());
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
