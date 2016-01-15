package org.dc.bco.manager.location.core;

import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.manager.location.lib.Location;
import org.dc.bco.dal.lib.layer.unit.AbstractUnitCollectionController;
import org.dc.bco.registry.location.lib.LocationRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import rst.homeautomation.device.GenericDeviceType.GenericDevice;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationController extends AbstractUnitCollectionController<GenericDevice, GenericDevice.Builder> implements Location {

    private LocationConfig config;

    public LocationController(final LocationConfig config) throws InstantiationException {
        super(GenericDevice.newBuilder());
        this.config = config;
    }

    public LocationController update(final LocationConfig config) {
        this.config = config;
        return this;
    }

    @Override
    public LocationConfig getConfig() {
        return config;
    }

    @Override
    public String getId() throws CouldNotPerformException {
        return config.getId();
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return config.getLabel();
    }

    @Override
    public ServiceFactory getServiceFactory() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocationRegistry getLocationRegistry() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}