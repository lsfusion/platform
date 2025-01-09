package lsfusion.gwt.client;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.GPropertyReaderDTO;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.interop.form.property.PropertyReadType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

import static lsfusion.gwt.client.form.object.GGroupObjectValue.checkTwins;

public class GFormChanges {
    public final NativeSIDMap<GGroupObject, GGroupObjectValue> objects = new NativeSIDMap<>();
    public final NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> gridObjects = new NativeSIDMap<>();
    public final NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> parentObjects = new NativeSIDMap<>();
    public final NativeSIDMap<GGroupObject, NativeHashMap<GGroupObjectValue, Integer>> expandables = new NativeSIDMap<>();
    public final NativeSIDMap<GPropertyReader, NativeHashMap<GGroupObjectValue, PValue>> properties = new NativeSIDMap<>();
    public final HashSet<GPropertyDraw> dropProperties = new HashSet<>();

    public final NativeSIDMap<GGroupObject, Boolean> updateStateObjects = new NativeSIDMap<>();

    public final ArrayList<GComponent> activateTabs = new ArrayList<>();
    public final ArrayList<GPropertyDraw> activateProps = new ArrayList<>();
    
    public final ArrayList<GContainer> collapseContainers = new ArrayList<>();
    public final ArrayList<GContainer> expandContainers = new ArrayList<>();

    public final HashSet<GPropertyDraw> updateProperties = new HashSet<>();

    public boolean needConfirm;

    public static GFormChanges remap(GForm form, GFormChangesDTO dto) {
        GFormChanges remapped = new GFormChanges();

        for (int i = 0; i < dto.objectsGroupIds.length; i++) {
            remapped.objects.put(form.getGroupObject(dto.objectsGroupIds[i]), checkTwins(dto.objects[i]));
        }

        for (int i = 0; i < dto.gridObjectsGroupIds.length; i++) {
            remapped.gridObjects.put(form.getGroupObject(dto.gridObjectsGroupIds[i]), checkTwins(dto.gridObjects[i]));
        }

        for (int i = 0; i < dto.parentObjectsGroupIds.length; i++) {
            remapped.parentObjects.put(form.getGroupObject(dto.parentObjectsGroupIds[i]), dto.parentObjects[i]);
        }

        for (int i = 0; i < dto.expandablesGroupIds.length; i++) {
            remapped.expandables.put(form.getGroupObject(dto.expandablesGroupIds[i]), remapAndCheckTwins(dto.expandableKeys[i], dto.expandableValues[i]));
        }

        for (int i = 0; i < dto.properties.length; i++) {
            remapped.properties.put(remapPropertyReader(form, dto.properties[i]), remapAndCheckTwins(dto.propertiesValueKeys[i], remapValues(dto.propertiesValueValues[i])));
        }

        for (Integer propertyID : dto.dropPropertiesIds) {
            remapped.dropProperties.add(form.getProperty(propertyID));
        }

        for (int i = 0; i < dto.updateStateObjectsGroupIds.length; i++) {
            remapped.updateStateObjects.put(form.getGroupObject(dto.updateStateObjectsGroupIds[i]), dto.updateStateObjectsGroupValues[i]);
        }

        for (int activateTab : dto.activateTabsIds) {
            remapped.activateTabs.add(form.findContainerByID(activateTab));
        }

        for (int activateProp : dto.activatePropsIds) {
            remapped.activateProps.add(form.getProperty(activateProp));
        }

        for (int collapseContainerId : dto.collapseContainerIds) {
            remapped.collapseContainers.add(form.findContainerByID(collapseContainerId));
        }

        for (int expandContainerId : dto.expandContainerIds) {
            remapped.expandContainers.add(form.findContainerByID(expandContainerId));
        }

        remapped.needConfirm = dto.needConfirm;

        return remapped;
    }

    private static <V> NativeHashMap<GGroupObjectValue, V> remapAndCheckTwins(GGroupObjectValue[] keys, V[] values) {
        NativeHashMap<GGroupObjectValue, V> result = new NativeHashMap<>();
        for(int i=0;i<keys.length;i++)
            result.put(checkTwins(keys[i]), values[i]);
        return result;
    }

    private static GPropertyReader remapPropertyReader(GForm form, GPropertyReaderDTO readerDTO) {
        return remapPropertyReader(form, readerDTO.type, readerDTO.readerID, readerDTO.index);
    }

    private static GPropertyReader remapPropertyReader(GForm form, int typeId, int readerId, int index) {
        switch (typeId) {
            case GPropertyReadType.DRAW:
                return form.getProperty(readerId);
            case GPropertyReadType.CAPTION:
                return form.getProperty(readerId).captionReader;
            case GPropertyReadType.SHOWIF:
                return form.getProperty(readerId).showIfReader;
            case GPropertyReadType.READONLY:
                return form.getProperty(readerId).readOnlyReader;
            case GPropertyReadType.CELL_VALUEELEMENTCLASS:
                return form.getProperty(readerId).valueElementClassReader;
            case GPropertyReadType.CAPTIONELEMENTCLASS:
                return form.getProperty(readerId).captionElementClassReader;
            case GPropertyReadType.CELL_FONT:
                return form.getProperty(readerId).fontReader;
            case GPropertyReadType.CELL_BACKGROUND:
                return form.getProperty(readerId).backgroundReader;
            case GPropertyReadType.CELL_FOREGROUND:
                return form.getProperty(readerId).foregroundReader;
            case GPropertyReadType.IMAGE:
                return form.getProperty(readerId).imageReader;
            case GPropertyReadType.FOOTER:
                return form.getProperty(readerId).footerReader;
            case GPropertyReadType.ROW_BACKGROUND:
                return form.getGroupObject(readerId).rowBackgroundReader;
            case GPropertyReadType.ROW_FOREGROUND:
                return form.getGroupObject(readerId).rowForegroundReader;
            case GPropertyReadType.LAST:
                return form.getProperty(readerId).lastReaders.get(index);
            case GPropertyReadType.CONTAINER_CAPTION:
                return form.findContainerByID(readerId).captionReader;
            case GPropertyReadType.CONTAINER_IMAGE:
                return form.findContainerByID(readerId).imageReader;
            case GPropertyReadType.CONTAINER_CAPTIONCLASS:
                return form.findContainerByID(readerId).captionClassReader;
            case GPropertyReadType.CONTAINER_VALUECLASS:
                return form.findContainerByID(readerId).valueClassReader;
            case GPropertyReadType.CUSTOM:
                return form.findContainerByID(readerId).customDesignReader;
            case GPropertyReadType.COMPONENT_SHOWIF:
                return form.findComponentByID(readerId).showIfReader;
            case GPropertyReadType.COMPONENT_ELEMENTCLASS:
                return form.findComponentByID(readerId).elementClassReader;
            case GPropertyReadType.GRID_VALUECLASS:
                return ((GGridProperty)form.findComponentByID(readerId)).valueElementClassReader;
            case GPropertyReadType.CUSTOM_OPTIONS:
                return form.getGroupObject(readerId).customOptionsReader;
            case GPropertyReadType.COMMENT:
                return form.getProperty(readerId).commentReader;
            case GPropertyReadType.COMMENTELEMENTCLASS:
                return form.getProperty(readerId).commentElementClassReader;
            case GPropertyReadType.PLACEHOLDER:
                return form.getProperty(readerId).placeholderReader;
            case GPropertyReadType.PATTERN:
                return form.getProperty(readerId).patternReader;
            case GPropertyReadType.REGEXP:
                return form.getProperty(readerId).regexpReader;
            case GPropertyReadType.REGEXPMESSAGE:
                return form.getProperty(readerId).regexpMessageReader;
            case GPropertyReadType.TOOLTIP:
                return form.getProperty(readerId).tooltipReader;
            case GPropertyReadType.VALUETOOLTIP:
                return form.getProperty(readerId).valueTooltipReader;
            default:
                return null;
        }
    }

    public static PValue[] remapValues(Serializable[] values) {
        PValue[] mappedValues = new PValue[values.length];
        for (int i = 0; i < values.length; i++)
            mappedValues[i] = PValue.convertFileValue(values[i]);
        return mappedValues;
    }

    // should correspond PropertyReadType
    public class GPropertyReadType {
        public final static byte DRAW = 0;
        public final static byte CAPTION = 1;
        public final static byte SHOWIF = 2;
        public final static byte FOOTER = 3;
        public final static byte READONLY = 4;
        public final static byte CELL_BACKGROUND = 5;
        public final static byte CELL_FOREGROUND = 6;
        public final static byte ROW_BACKGROUND = 7;
        public final static byte ROW_FOREGROUND = 8;
        public final static byte LAST = 9;
        public final static byte CONTAINER_CAPTION = 10;
        public final static byte IMAGE = 11;
        public final static byte CUSTOM = 12;
        public final static byte COMPONENT_SHOWIF = 13;
        public final static byte CUSTOM_OPTIONS = 14;
        public final static byte CONTAINER_IMAGE = 15;
        public final static byte CELL_VALUEELEMENTCLASS = 16;
        public final static byte COMPONENT_ELEMENTCLASS = 17;
        public final static byte CAPTIONELEMENTCLASS = 18;
        public final static byte COMMENT = 19;
        public final static byte COMMENTELEMENTCLASS = 20;
        public final static byte PLACEHOLDER = 21;
        public final static byte TOOLTIP = 22;
        public final static byte VALUETOOLTIP = 23;
        public final static byte PATTERN = 24;
        public final static byte REGEXP = 25;
        public final static byte REGEXPMESSAGE = 26;
        public final static byte CELL_FONT = 27;
        public final static byte CONTAINER_CAPTIONCLASS = 28;
        public final static byte CONTAINER_VALUECLASS = 29;
        public final static byte GRID_VALUECLASS = 30;
    }
}
