package org.openbase.bco.device.openhab.jp;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

import org.openbase.jps.core.AbstractJavaProperty;

import java.util.ArrayList;
import java.util.List;

public class JPExcludeDeviceList extends AbstractJavaProperty<List<String>> {

    public final static String[] COMMAND_IDENTIFIERS = {"--exclude-devices"};

    public JPExcludeDeviceList() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public String getDescription() {
        return "Specify devices (through their aliases) which will not be started by the device manager";
    }

    @Override
    protected List<String> getPropertyDefaultValue() {
        return new ArrayList<>();
    }

    @Override
    protected String[] generateArgumentIdentifiers() {
        return new String[0];
    }

    @Override
    protected List<String> parse(List<String> arguments) throws Exception {
        return arguments;
    }
}
