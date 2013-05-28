package platform.gwt.form.shared.view.changes;

import platform.gwt.form.shared.view.GClassViewType;
import platform.gwt.form.shared.view.GForm;
import platform.gwt.form.shared.view.GGroupObject;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.dto.GFormChangesDTO;
import platform.gwt.form.shared.view.changes.dto.GPropertyReaderDTO;
import platform.gwt.form.shared.view.reader.GPropertyReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GFormChanges {
    public final HashMap<GGroupObject, GClassViewType> classViews = new HashMap<GGroupObject, GClassViewType>();
    public final HashMap<GGroupObject, GGroupObjectValue> objects = new HashMap<GGroupObject, GGroupObjectValue>();
    public final HashMap<GGroupObject, ArrayList<GGroupObjectValue>> gridObjects = new HashMap<GGroupObject, ArrayList<GGroupObjectValue>>();
    public final HashMap<GGroupObject, ArrayList<GGroupObjectValue>> parentObjects = new HashMap<GGroupObject, ArrayList<GGroupObjectValue>>();
    public final HashMap<GPropertyReader, HashMap<GGroupObjectValue, Object>> properties = new HashMap<GPropertyReader, HashMap<GGroupObjectValue, Object>>();
    public final HashSet<GPropertyReader> panelProperties = new HashSet<GPropertyReader>();
    public final HashSet<GPropertyDraw> dropProperties = new HashSet<GPropertyDraw>();

    public final HashSet<GPropertyDraw> updateProperties = new HashSet<GPropertyDraw>();

    public static GFormChanges remap(GForm form, GFormChangesDTO dto) {
        GFormChanges remapped = new GFormChanges();

        for (int i = 0; i < dto.classViewsGroupIds.length; i++) {
            remapped.classViews.put(form.getGroupObject(dto.classViewsGroupIds[i]), dto.classViews[i]);
        }

        for (int i = 0; i < dto.objectsGroupIds.length; i++) {
            remapped.objects.put(form.getGroupObject(dto.objectsGroupIds[i]), dto.objects[i]);
        }

        for (int i = 0; i < dto.gridObjectsGroupIds.length; i++) {
            remapped.gridObjects.put(form.getGroupObject(dto.gridObjectsGroupIds[i]), dto.gridObjects[i]);
        }

        for (int i = 0; i < dto.parentObjectsGroupIds.length; i++) {
            remapped.parentObjects.put(form.getGroupObject(dto.parentObjectsGroupIds[i]), dto.parentObjects[i]);
        }

        for (int i = 0; i < dto.properties.length; i++) {
            remapped.properties.put(remapPropertyReader(form, dto.properties[i]), dto.propertiesValues[i]);
        }

        for (int propertyId : dto.panelPropertiesIds) {
            remapped.panelProperties.add(form.getProperty(propertyId));
        }

        for (Integer propertyID : dto.dropPropertiesIds) {
            remapped.dropProperties.add(form.getProperty(propertyID));
        }

        return remapped;
    }

    private static GPropertyReader remapPropertyReader(GForm form, GPropertyReaderDTO readerDTO) {
        return remapPropertyReader(form, readerDTO.type, readerDTO.readerID);
    }

    private static GPropertyReader remapPropertyReader(GForm form, int typeId, int readerId) {
        switch (typeId) {
            case GPropertyReadType.DRAW:
                return form.getProperty(readerId);
            case GPropertyReadType.CAPTION:
                return form.getProperty(readerId).captionReader;
            case GPropertyReadType.READONLY:
                return form.getProperty(readerId).readOnlyReader;
            case GPropertyReadType.CELL_BACKGROUND:
                return form.getProperty(readerId).backgroundReader;
            case GPropertyReadType.CELL_FOREGROUND:
                return form.getProperty(readerId).foregroundReader;
            case GPropertyReadType.FOOTER:
                return form.getProperty(readerId).footerReader;
            case GPropertyReadType.ROW_BACKGROUND:
                return form.getGroupObject(readerId).rowBackgroundReader;
            case GPropertyReadType.ROW_FOREGROUND:
                return form.getGroupObject(readerId).rowForegroundReader;
            default:
                return null;
        }
    }

    public class GPropertyReadType {
        public final static byte DRAW = 0;
        public final static byte CAPTION = 1;
        public final static byte FOOTER = 2;
        public final static byte READONLY = 3;
        public final static byte CELL_BACKGROUND = 4;
        public final static byte CELL_FOREGROUND = 5;
        public final static byte ROW_BACKGROUND = 6;
        public final static byte ROW_FOREGROUND = 7;
    }
}
