package lsfusion.gwt.form.server.convert;

import lsfusion.base.FileData;
import lsfusion.base.RawFileData;
import lsfusion.client.logics.*;
import lsfusion.client.logics.classes.ClientImageClass;
import lsfusion.gwt.form.server.logics.spring.LogicsProvider;
import lsfusion.gwt.form.server.FileUtils;
import lsfusion.gwt.form.shared.view.GClassViewType;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValueBuilder;
import lsfusion.gwt.form.shared.view.changes.dto.*;
import lsfusion.interop.ClassViewType;

import java.awt.*;
import java.sql.Date;
import java.sql.Time;
import java.util.List;
import java.util.*;

@SuppressWarnings("UnusedDeclaration")
public class ClientFormChangesToGwtConverter extends ObjectConverter {
    private static final class InstanceHolder {
        private static final ClientFormChangesToGwtConverter instance = new ClientFormChangesToGwtConverter();
    }

    public static ClientFormChangesToGwtConverter getInstance() {
        return InstanceHolder.instance;
    }

    private ClientFormChangesToGwtConverter() {
    }

    @Converter(from = ClientFormChanges.class)
    public GFormChangesDTO convertFormChanges(ClientFormChanges changes, Integer requestIndex, LogicsProvider blProvider) {
        GFormChangesDTO dto = new GFormChangesDTO();

        dto.requestIndex = requestIndex;

        dto.classViewsGroupIds = new int[changes.classViews.size()];
        dto.classViews = new GClassViewType[changes.classViews.size()];
        int i = 0;
        for (Map.Entry<ClientGroupObject, ClassViewType> entry : changes.classViews.entrySet()) {
            dto.classViewsGroupIds[i] = entry.getKey().getID();
            dto.classViews[i++] = GClassViewType.valueOf(entry.getValue().name());
        }

        dto.objectsGroupIds = new int[changes.objects.size()];
        dto.objects = new GGroupObjectValue[changes.objects.size()];
        i = 0;
        for (Map.Entry<ClientGroupObject, ClientGroupObjectValue> e : changes.objects.entrySet()) {
            GGroupObjectValue groupObjectValue = convertOrCast(e.getValue(), blProvider);
            dto.objectsGroupIds[i] = e.getKey().ID;
            dto.objects[i++] = groupObjectValue;
        }

        dto.gridObjectsGroupIds = new int[changes.gridObjects.size()];
        dto.gridObjects = new ArrayList[changes.gridObjects.size()];
        i = 0;
        for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : changes.gridObjects.entrySet()) {
            ArrayList<GGroupObjectValue> keys = new ArrayList<>();

            for (ClientGroupObjectValue keyValue : entry.getValue()) {
                GGroupObjectValue groupObjectValue = convertOrCast(keyValue, blProvider);
                keys.add(groupObjectValue);
            }

            dto.gridObjectsGroupIds[i] = entry.getKey().ID;
            dto.gridObjects[i++] = keys;
        }

        dto.parentObjectsGroupIds = new int[changes.parentObjects.size()];
        dto.parentObjects = new ArrayList[changes.parentObjects.size()];
        i = 0;
        for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : changes.parentObjects.entrySet()) {
            ArrayList<GGroupObjectValue> keys = new ArrayList<>();

            for (ClientGroupObjectValue keyValue : entry.getValue()) {
                GGroupObjectValue groupObjectValue = convertOrCast(keyValue, blProvider);
                keys.add(groupObjectValue);
            }

            dto.parentObjectsGroupIds[i] = entry.getKey().ID;
            dto.parentObjects[i++] = keys;
        }

        dto.expandablesGroupIds = new int[changes.expandables.size()];
        dto.expandables = new HashMap[changes.expandables.size()];
        i = 0;
        for (Map.Entry<ClientGroupObject, Map<ClientGroupObjectValue, Boolean>> entry : changes.expandables.entrySet()) {
            HashMap<GGroupObjectValue, Boolean> expandables = new HashMap<>();
            for (Map.Entry<ClientGroupObjectValue, Boolean> expandable : entry.getValue().entrySet()) {
                GGroupObjectValue groupObjectValue = convertOrCast(expandable.getKey(), blProvider);
                expandables.put(groupObjectValue, expandable.getValue());
            }
            dto.expandablesGroupIds[i] = entry.getKey().ID;
            dto.expandables[i++] = expandables;
        }

        dto.properties = new GPropertyReaderDTO[changes.properties.size()];
        dto.propertiesValues = new HashMap[changes.properties.size()];
        i = 0;
        for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> entry : changes.properties.entrySet()) {
            HashMap<GGroupObjectValue, Object> propValues = new HashMap<>();
            ClientPropertyReader reader = entry.getKey();
            for (Map.Entry<ClientGroupObjectValue, Object> clientValues : entry.getValue().entrySet()) {
                GGroupObjectValue groupObjectValue = convertOrCast(clientValues.getKey(), blProvider);

                Object propValue = convertOrCast(clientValues.getValue(), blProvider);
                if (propValue instanceof FileData || propValue instanceof RawFileData) {
                    propValue = convertFileValue(reader, propValue);
                }
                propValues.put(groupObjectValue, propValue);
            }

            dto.properties[i] = new GPropertyReaderDTO(reader.getID(), reader.getType());
            dto.propertiesValues[i++] = propValues;
        }

        dto.panelPropertiesIds = new int[changes.panelProperties.size()];
        i = 0;
        for (ClientPropertyDraw panelProperty : changes.panelProperties) {
            dto.panelPropertiesIds[i++] = panelProperty.ID;
        }

        dto.dropPropertiesIds = new int[changes.dropProperties.size()];
        i = 0;
        for (ClientPropertyDraw dropProperty : changes.dropProperties) {
            dto.dropPropertiesIds[i++] = dropProperty.ID;
        }

        dto.activateTabsIds = new int[changes.activateTabs.size()];
        i = 0;
        for (ClientComponent tab : changes.activateTabs) {
            dto.activateTabsIds[i++] = tab.ID;
        }

        dto.activatePropsIds = new int[changes.activateProps.size()];
        i = 0;
        for (ClientPropertyDraw activateProperty : changes.activateProps) {
            dto.activatePropsIds[i++] = activateProperty.ID;
        }

        return dto;
    }

    private Object convertFileValue(ClientPropertyReader reader, Object value) {
        if (reader instanceof ClientPropertyDraw && ((ClientPropertyDraw) reader).baseType instanceof ClientImageClass) {
            return FileUtils.createPropertyImage(value != null ? ((RawFileData) value).getBytes() : null, ((ClientPropertyDraw) reader).getPropertyFormName());
        } else {
            return value == null ? null : true;
        }
    }

    @Converter(from = Color.class)
    public ColorDTO convertColor(Color color) {
        return StaticConverters.convertColor(color);
    }

    @Converter(from = ClientGroupObjectValue.class)
    public GGroupObjectValue convertGroupObjectValue(ClientGroupObjectValue clientGroupObjValue) {
        GGroupObjectValueBuilder groupObjectValue = new GGroupObjectValueBuilder();
        for (Map.Entry<ClientObject, Object> keyPart : clientGroupObjValue.entrySet()) {
            groupObjectValue.put(keyPart.getKey().ID, keyPart.getValue());
        }
        return groupObjectValue.toGroupObjectValue();
    }

    @Converter(from = Date.class)
    public GDateDTO convertDate(Date gDate, LogicsProvider blProvider) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(gDate);
        return new GDateDTO(gc.get(Calendar.DAY_OF_MONTH), gc.get(Calendar.MONTH), gc.get(Calendar.YEAR) - 1900);
    }

    @Converter(from = Time.class)
    public GTimeDTO convertTime(Time time, LogicsProvider blProvider) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.clear();
        gc.setTime(time);
        return new GTimeDTO(gc.get(Calendar.HOUR_OF_DAY), gc.get(Calendar.MINUTE), gc.get(Calendar.SECOND));
    }
}
