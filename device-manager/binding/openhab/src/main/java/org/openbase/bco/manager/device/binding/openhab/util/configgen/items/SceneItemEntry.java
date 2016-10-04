package org.openbase.bco.manager.device.binding.openhab.util.configgen.items;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
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
import org.openbase.bco.manager.device.binding.openhab.util.configgen.GroupEntry;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.processing.StringProcessor;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneItemEntry extends AbstractItemEntry {

    public static String SCENE_GROUP_LABEL = "Scenes";

    public SceneItemEntry(final UnitConfig sceneUnitConfig, final LocationRegistryRemote locationRegistryRemote) throws org.openbase.jul.exception.InstantiationException {
        super();
        try {
            this.itemId = generateItemId(sceneUnitConfig);
            this.icon = "";
            this.commandType = "Switch";
            this.label = sceneUnitConfig.getLabel();
            this.itemHardwareConfig = "rsb=\"bco.manager.scene:" + sceneUnitConfig.getId() + "\"";
            groups.add(SCENE_GROUP_LABEL);
            groups.add(GroupEntry.generateGroupID(sceneUnitConfig.getPlacementConfig().getLocationId(), locationRegistryRemote));
            calculateGaps();
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    private String generateItemId(final UnitConfig sceneUnitConfig) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("Scene")
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(sceneUnitConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
