/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.service;

import org.dc.bco.manager.device.binding.openhab.comm.OpenHABCommunicator;
import org.dc.bco.manager.device.binding.openhab.transform.ItemTransformer;
import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.bco.dal.lib.layer.service.ServiceType;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.NotSupportedException;
import java.util.concurrent.Future;
import org.dc.bco.manager.device.binding.openhab.OpenHABBindingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.service.ServiceConfigType;

/**
 *
 * @author mpohling
 * @param <ST> related service type
 */
public abstract class OpenHABService<ST extends Service & Unit> implements Service {

	private OpenHABCommunicator openHABCommunicator;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected final ST unit;
	private final String itemName;
	private final ServiceType serviceType;
    private final ServiceConfigType.ServiceConfig config;

	public OpenHABService(final ST unit) throws InstantiationException {
		try {
			this.unit = unit;
			this.serviceType = detectServiceType();
            this.config = loadServiceConfig();
			this.itemName = ItemTransformer.getItemName(this);
			this.openHABCommunicator = OpenHABBindingImpl.getInstance().getBusCommunicator();
		} catch (CouldNotPerformException ex) {
			throw new InstantiationException(this, ex);
		}
	}
    
    private ServiceConfigType.ServiceConfig loadServiceConfig() throws CouldNotPerformException {
        for(ServiceConfigType.ServiceConfig serviceConfig : ((Unit) unit).getUnitConfig().getServiceConfigList()) {
            if(serviceConfig.getType() == serviceType.getRSTType()) {
                return serviceConfig;
            }
        }
        throw new CouldNotPerformException("Could not detect service config! Service["+serviceType.getRSTType().name()+"] is not configured in Unit["+((Unit) unit).getId()+"]!");
    }

	public final ServiceType detectServiceType() throws NotSupportedException {
		return ServiceType.valueOfByServiceName(getClass().getSimpleName().replaceFirst("Service", "").replaceFirst("Provider", "").replaceFirst("Impl", ""));
	}

	public ST getUnit() {
		return unit;
	}

	public String getItemID() {
		return itemName;
	}

    @Override
    public ServiceConfigType.ServiceConfig getServiceConfig() {
        return config;
    }

	@Override
	public ServiceType getServiceType() {
		return serviceType;
	}

	public Future executeCommand(final OpenhabCommandType.OpenhabCommand.Builder command) throws CouldNotPerformException {
		if (itemName == null) {
			throw new NotAvailableException("itemID");
		}
		return executeCommand(itemName, command, OpenhabCommandType.OpenhabCommand.ExecutionType.SYNCHRONOUS);
	}

	public Future executeCommand(final String itemName, final OpenhabCommandType.OpenhabCommand.Builder command, final OpenhabCommandType.OpenhabCommand.ExecutionType type) throws CouldNotPerformException {
		if (command == null) {
			throw new CouldNotPerformException("Skip sending empty command!", new NullPointerException("Argument command is null!"));
		}

		if (openHABCommunicator == null) {
			throw new CouldNotPerformException("Skip sending command, binding not ready!", new NullPointerException("Argument rsbBinding is null!"));
		}

		logger.debug("Execute command: Setting item [" + this.itemName + "] to [" + command.getType().toString() + "]");
		return openHABCommunicator.executeCommand(command.setItem(itemName).setExecutionType(type).build());
	}
}