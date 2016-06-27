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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.OpenHABItemConfigGenerator.TAB_SIZE;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;
import rst.homeautomation.service.ServiceTemplateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public abstract class AbstractItemEntry implements ItemEntry {

    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractItemEntry.class);

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

    protected AbstractItemEntry() throws org.openbase.jul.exception.InstantiationException {
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

    protected String getDefaultCommand(ServiceTemplateType.ServiceTemplate.ServiceType type) {
        switch (type) {
            case COLOR_SERVICE:
                return "Color";
            case OPENING_RATIO_PROVIDER:
            case OPENING_RATIO_SERVICE:
            case POWER_CONSUMPTION_PROVIDER:
            case TEMPERATURE_PROVIDER:
            case MOTION_PROVIDER:
            case TAMPER_PROVIDER:
            case BRIGHTNESS_PROVIDER:
            case BATTERY_PROVIDER:
            case SMOKE_ALARM_STATE_PROVIDER:
            case SMOKE_STATE_PROVIDER:
            case TEMPERATURE_ALARM_STATE_PROVIDER:
            case TARGET_TEMPERATURE_PROVIDER:
            case TARGET_TEMPERATURE_SERVICE:
                return "Number";
            case SHUTTER_PROVIDER:
            case SHUTTER_SERVICE:
                return "Rollershutter";
            case POWER_SERVICE:
            case POWER_PROVIDER:
            case BUTTON_PROVIDER:
                return "Switch";
            case BRIGHTNESS_SERVICE:
            case DIM_PROVIDER:
            case DIM_SERVICE:
                return "Dimmer";
            case REED_SWITCH_PROVIDER:
                return "Contact";
            case HANDLE_PROVIDER:
                return "String";
            default:
                logger.warn("Unkown Service Type: " + type);
                return "";
        }
    }
}