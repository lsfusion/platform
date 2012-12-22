package platform.gwt.form.server.convert;

import platform.client.logics.*;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GClassViewType;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.changes.GGroupObjectValueBuilder;
import platform.gwt.form.shared.view.changes.dto.ColorDTO;
import platform.gwt.form.shared.view.changes.dto.GFormChangesDTO;
import platform.gwt.form.shared.view.changes.dto.GPropertyReaderDTO;
import platform.interop.ClassViewType;

import java.awt.*;
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
    public GFormChangesDTO convertFormChanges(ClientFormChanges changes) {
        GFormChangesDTO dto = new GFormChangesDTO();

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
            ArrayList<GGroupObjectValue> keys = new ArrayList<GGroupObjectValue>();

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
            ArrayList<GGroupObjectValue> keys = new ArrayList<GGroupObjectValue>();

            for (ClientGroupObjectValue keyValue : entry.getValue()) {
                GGroupObjectValue groupObjectValue = convertOrCast(keyValue);
                keys.add(groupObjectValue);
            }

            dto.parentObjectsGroupIds[i] = entry.getKey().ID;
            dto.parentObjects[i++] = keys;
        }

        dto.properties = new GPropertyReaderDTO[changes.properties.size()];
        dto.propertiesValues = new HashMap[changes.properties.size()];
        i = 0;
        for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> entry : changes.properties.entrySet()) {
            HashMap<GGroupObjectValue, Object> propValues = new HashMap<GGroupObjectValue, Object>();
            for (Map.Entry<ClientGroupObjectValue, Object> clientValues : entry.getValue().entrySet()) {
                GGroupObjectValue groupObjectValue = convertOrCast(clientValues.getKey());

                propValues.put(groupObjectValue, convertOrCast(clientValues.getValue()));
            }
            ClientPropertyReader reader = entry.getKey();

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

        return dto;
    }

    @Converter(from = Color.class)
    public ColorDTO convertColor(Color color) {
        return new ColorDTO(Integer.toHexString(color.getRGB()).substring(2, 8));
    }

    @Converter(from = String.class)
    public String convertString(String s) {
        return GwtSharedUtils.rtrim(s);
    }

    @Converter(from = ClientGroupObjectValue.class)
    public GGroupObjectValue convertGroupObjectValue(ClientGroupObjectValue clientGroupObjValue) {
        GGroupObjectValueBuilder groupObjectValue = new GGroupObjectValueBuilder();
        for (Map.Entry<ClientObject, Object> keyPart : clientGroupObjValue.entrySet()) {
            groupObjectValue.put(keyPart.getKey().ID, keyPart.getValue());
        }
        return groupObjectValue.toGroupObjectValue();
    }
}
