/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.lib;

import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.scope.ScopeProvider;
import org.dc.jul.iface.Activatable;
import org.dc.jul.iface.Configurable;
import org.dc.jul.iface.Identifiable;
import org.dc.jul.iface.provider.LabelProvider;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author Divine Threepwood
 */
public interface Device extends ScopeProvider, LabelProvider, Identifiable<String>, Activatable, Configurable<String, DeviceConfig> {

    public DeviceConfig getConfig();

    public Location getLocation();

    public ServiceFactory getServiceFactory() throws NotAvailableException;
}
