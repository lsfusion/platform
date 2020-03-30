package lsfusion.gwt.server.convert;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.client.classes.data.ClientImageClass;
import lsfusion.client.form.ClientFormChanges;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.gwt.client.GFormChangesDTO;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GGroupObjectValueBuilder;
import lsfusion.gwt.client.form.property.GClassViewType;
import lsfusion.gwt.client.form.property.GPropertyReaderDTO;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.GTimeDTO;
import lsfusion.gwt.server.FileUtils;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.form.property.ClassViewType;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public GFormChangesDTO convertFormChanges(ClientFormChanges changes, Integer requestIndex, FormSessionObject sessionObject) {
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
            GGroupObjectValue groupObjectValue = convertOrCast(e.getValue());
            dto.objectsGroupIds[i] = e.getKey().ID;
            dto.objects[i++] = groupObjectValue;
        }

        dto.gridObjectsGroupIds = new int[changes.gridObjects.size()];
        dto.gridObjects = new ArrayList[changes.gridObjects.size()];
        i = 0;
        for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : changes.gridObjects.entrySet()) {
            ArrayList<GGroupObjectValue> keys = new ArrayList<>();

            for (ClientGroupObjectValue keyValue : entry.getValue()) {
                GGroupObjectValue groupObjectValue = convertOrCast(keyValue);
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
                GGroupObjectValue groupObjectValue = convertOrCast(keyValue);
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
                GGroupObjectValue groupObjectValue = convertOrCast(expandable.getKey());
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
                GGroupObjectValue groupObjectValue = convertOrCast(clientValues.getKey());

                Object propValue = convertOrCast(clientValues.getValue());
                if (propValue instanceof FileData || propValue instanceof RawFileData) {
                    propValue = convertFileValue(reader, propValue, sessionObject);
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

    private Object convertFileValue(ClientPropertyReader reader, Object value, FormSessionObject sessionObject) {
        if (reader instanceof ClientPropertyDraw && ((ClientPropertyDraw) reader).baseType instanceof ClientImageClass) {
            return FileUtils.saveFormFile((RawFileData) value, sessionObject);
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
            groupObjectValue.put(keyPart.getKey().ID, convertOrCast(keyPart.getValue()));
        }
        return groupObjectValue.toGroupObjectValue();
    }

    @Converter(from = LocalDate.class)
    public GDateDTO convertDate(LocalDate gDate) {
        return new GDateDTO(gDate.getYear(), gDate.getMonthValue(), gDate.getDayOfMonth());
    }

    @Converter(from = LocalTime.class)
    public GTimeDTO convertTime(LocalTime time) {
        return new GTimeDTO(time.getHour(), time.getMinute(), time.getSecond());
    }

    @Converter(from = LocalDateTime.class)
    public GDateTimeDTO convertDateTime(LocalDateTime dateTime) {
        return new GDateTimeDTO(dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
    }
}
