package org.openbase.bco.manager.location.lib;

/*
 * #%L
 * COMA LocationManager Library
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import org.openbase.bco.dal.lib.layer.service.provider.HandleProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.ReedSwitchProviderService;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.iface.Configurable;
import org.openbase.jul.iface.provider.LabelProvider;
import rst.spatial.ConnectionConfigType.ConnectionConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface Connection extends ScopeProvider, LabelProvider, Configurable<String, ConnectionConfig>, HandleProviderService, ReedSwitchProviderService {

}
