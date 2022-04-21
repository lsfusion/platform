package lsfusion.gwt.server.convert;

import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
import lsfusion.base.file.RawFileData;
import lsfusion.client.classes.data.ClientImageClass;
import lsfusion.client.form.ClientFormChanges;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.object.ClientCustomObjectValue;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.gwt.client.GFormChangesDTO;
import lsfusion.gwt.client.form.object.GCustomObjectValue;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GGroupObjectValueBuilder;
import lsfusion.gwt.client.form.property.GPropertyReaderDTO;
import lsfusion.gwt.client.form.property.cell.classes.*;
import lsfusion.gwt.server.FileUtils;
import lsfusion.http.provider.form.FormSessionObject;

import java.awt.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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

        dto.objectsGroupIds = new int[changes.objects.size()];
        dto.objects = new GGroupObjectValue[changes.objects.size()];
        int i = 0;
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
        dto.expandableKeys = new GGroupObjectValue[changes.expandables.size()][];
        dto.expandableValues = new Boolean[changes.expandables.size()][];
        i = 0;
        for (Map.Entry<ClientGroupObject, Map<ClientGroupObjectValue, Boolean>> entry : changes.expandables.entrySet()) {
            Map<ClientGroupObjectValue, java.lang.Boolean> values = entry.getValue();

            int j = 0;
            GGroupObjectValue[] expandableKeys = new GGroupObjectValue[values.size()];
            Boolean[] expandableValues = new Boolean[values.size()];
            for (Map.Entry<ClientGroupObjectValue, Boolean> expandable : values.entrySet()) {
                GGroupObjectValue groupObjectValue = convertOrCast(expandable.getKey());
                expandableKeys[j] = groupObjectValue;
                expandableValues[j] = expandable.getValue();
                j++;
            }
            dto.expandablesGroupIds[i] = entry.getKey().ID;
            dto.expandableKeys[i] = expandableKeys;
            dto.expandableValues[i] = expandableValues;
            i++;
        }

        dto.properties = new GPropertyReaderDTO[changes.properties.size()];
        dto.propertiesValueKeys = new GGroupObjectValue[changes.properties.size()][];
        dto.propertiesValueValues = new Serializable[changes.properties.size()][];
        i = 0;
        for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> entry : changes.properties.entrySet()) {
            ClientPropertyReader reader = entry.getKey();
            Map<ClientGroupObjectValue, Object> values = entry.getValue();

            int j = 0;
            GGroupObjectValue[] propValueKeys = new GGroupObjectValue[values.size()];
            Serializable[] propValueValues = new Serializable[values.size()];
            for (Map.Entry<ClientGroupObjectValue, Object> clientValues : values.entrySet()) {
                GGroupObjectValue groupObjectValue = convertOrCast(clientValues.getKey());

                Object propValue = convertOrCast(clientValues.getValue());
                if (propValue instanceof NamedFileData || propValue instanceof FileData || propValue instanceof RawFileData) {
                    propValue = convertFileValue(reader, propValue, sessionObject);
                }
                propValueKeys[j] = groupObjectValue;
                propValueValues[j] = (Serializable) propValue;
                j++;
            }

            dto.properties[i] = new GPropertyReaderDTO(reader.getID(), reader.getType(), reader instanceof ClientPropertyDraw.LastReader ? ((ClientPropertyDraw.LastReader) reader).index : -1);
            dto.propertiesValueKeys[i] = propValueKeys;
            dto.propertiesValueValues[i] = propValueValues;
            i++;
        }

        dto.dropPropertiesIds = new int[changes.dropProperties.size()];
        i = 0;
        for (ClientPropertyDraw dropProperty : changes.dropProperties) {
            dto.dropPropertiesIds[i++] = dropProperty.ID;
        }

        dto.updateStateObjectsGroupIds = new int[changes.updateStateObjects.size()];
        dto.updateStateObjectsGroupValues = new boolean[changes.updateStateObjects.size()];
        i = 0;
        for (Map.Entry<ClientGroupObject, Boolean> dropProperty : changes.updateStateObjects.entrySet()) {
            dto.updateStateObjectsGroupIds[i] = dropProperty.getKey().ID;
            dto.updateStateObjectsGroupValues[i++] = dropProperty.getValue();
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

        dto.collapseContainerIds = new int[changes.collapseContainers.size()];
        i = 0;
        for (ClientContainer container : changes.collapseContainers) {
            dto.collapseContainerIds[i++] = container.ID;
        }

        dto.expandContainerIds = new int[changes.expandContainers.size()];
        i = 0;
        for (ClientContainer container : changes.expandContainers) {
            dto.expandContainerIds[i++] = container.ID;
        }

        dto.needConfirm = changes.needConfirm;

        return dto;
    }

    private Object convertFileValue(ClientPropertyReader reader, Object value, FormSessionObject sessionObject) {
        if ((reader instanceof ClientPropertyDraw && ((ClientPropertyDraw) reader).baseType instanceof ClientImageClass)
                || reader instanceof ClientPropertyDraw.ImageReader) {
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
        for (Map.Entry<ClientObject, Serializable> keyPart : clientGroupObjValue.iterate()) {
            groupObjectValue.put(keyPart.getKey().ID, convertOrCast(keyPart.getValue()));
        }
        return groupObjectValue.toGroupObjectValue();
    }

    @Converter(from = ClientCustomObjectValue.class)
    public GCustomObjectValue convertCustomObjectValue(ClientCustomObjectValue customObjectValue) {
        return new GCustomObjectValue(customObjectValue.id, customObjectValue.idClass);
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

    @Converter(from = Instant.class)
    public GZDateTimeDTO convertDateTime(Instant dateTime) {
        return new GZDateTimeDTO(dateTime.toEpochMilli());
    }
}
