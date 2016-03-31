/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.util.configgen.items;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.dc.bco.manager.device.binding.openhab.util.configgen.OpenHABItemConfigGenerator.TAB_SIZE;
import org.dc.jul.processing.StringProcessor;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public abstract class AbstractItemEntry implements ItemEntry {

    public static final String ITEM_SUBSEGMENT_DELIMITER = "_";
    public static final String ITEM_SEGMENT_DELIMITER = "__";
    
    protected String commandType;
    protected String itemId;
    protected String label;
    protected String icon;
    protected final List<String> groups;
    protected String itemHardwareConfig;

    private static int maxCommandTypeSize = 0;
    private static int maxItemIdSize = 0;
    private static int maxLabelSize = 0;
    private static int maxIconSize = 0;
    private static int maxGroupSize = 0;
    private static int maxBindingConfigSize = 0;

    protected AbstractItemEntry() throws org.dc.jul.exception.InstantiationException {
        this.groups = new ArrayList<>();
    }

    protected void calculateGaps() {
        maxCommandTypeSize = Math.max(maxCommandTypeSize, getCommandTypeStringRep().length());
        maxItemIdSize = Math.max(maxItemIdSize, getItemIdStringRep().length());
        maxLabelSize = Math.max(maxLabelSize, getLabelStringRep().length());
        maxIconSize = Math.max(maxIconSize, getIconStringRep().length());
        maxGroupSize = Math.max(maxGroupSize, getGroupsStringRep().length());
        maxBindingConfigSize = Math.max(maxBindingConfigSize, getBindingConfigStringRep().length());
    }

    public static void reset() {
        maxCommandTypeSize = 0;
        maxItemIdSize = 0;
        maxLabelSize = 0;
        maxIconSize = 0;
        maxGroupSize = 0;
        maxBindingConfigSize = 0;
    }

    @Override
    public String buildStringRep() {

        String stringRep = "";

        // command type
        stringRep += StringProcessor.fillWithSpaces(getCommandTypeStringRep(), maxCommandTypeSize + TAB_SIZE);

        // unit id
        stringRep += StringProcessor.fillWithSpaces(getItemIdStringRep(), maxItemIdSize + TAB_SIZE);

        // label
        stringRep += StringProcessor.fillWithSpaces(getLabelStringRep(), maxLabelSize + TAB_SIZE);

        // icon
        stringRep += StringProcessor.fillWithSpaces(getIconStringRep(), maxIconSize + TAB_SIZE);

        // groups
        stringRep += StringProcessor.fillWithSpaces(getGroupsStringRep(), maxGroupSize + TAB_SIZE);

        // binding config
        stringRep += StringProcessor.fillWithSpaces(getBindingConfigStringRep(), maxBindingConfigSize + TAB_SIZE);

        return stringRep;
    }

    @Override
    public String getCommandType() {
        return commandType;
    }

    @Override
    public String getItemId() {
        return itemId;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public List<String> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    @Override
    public String getItemHardwareConfig() {
        return itemHardwareConfig;
    }

    @Override
    public String getCommandTypeStringRep() {
        return commandType;
    }

    @Override
    public String getItemIdStringRep() {
        return itemId;
    }

    @Override
    public String getLabelStringRep() {
        if (label.isEmpty()) {
            return "";
        }
        return "\"" + label + "\"";
    }

    @Override
    public String getIconStringRep() {
        if (icon.isEmpty()) {
            return "";
        }
        return "<" + icon + ">";
    }

    @Override
    public String getGroupsStringRep() {
        if (groups.isEmpty()) {
            return "";
        }
        String stringRep = "(";
        boolean firstIteration = true;
        for (String group : groups) {
            if (!firstIteration) {
                stringRep += ",";
            } else {
                firstIteration = false;
            }
            stringRep += group;
        }
        stringRep += ")";
        return stringRep;
    }

    @Override
    public String getBindingConfigStringRep() {
        return "{ " + itemHardwareConfig + " }";
    }
}
