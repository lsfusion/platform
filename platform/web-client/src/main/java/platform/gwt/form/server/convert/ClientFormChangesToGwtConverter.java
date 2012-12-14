package platform.gwt.form.server.convert;

import platform.client.logics.*;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GClassViewType;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
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
        GFormChangesDTO changesDTO = new GFormChangesDTO();

        for (Map.Entry<ClientGroupObject, ClassViewType> entry : changes.classViews.entrySet()) {
            changesDTO.classViews.put(entry.getKey().getID(), GClassViewType.valueOf(entry.getValue().name()));
        }

        for (Map.Entry<ClientGroupObject, ClientGroupObjectValue> e : changes.objects.entrySet()) {
            GGroupObjectValue groupObjectValue = convertOrCast(e.getValue());
            changesDTO.objects.put(e.getKey().ID, groupObjectValue);
        }

        for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : changes.gridObjects.entrySet()) {
            ArrayList<GGroupObjectValue> keys = new ArrayList<GGroupObjectValue>();

            for (ClientGroupObjectValue keyValue : entry.getValue()) {
                GGroupObjectValue groupObjectValue = convertOrCast(keyValue);
                keys.add(groupObjectValue);
            }

            changesDTO.gridObjects.put(entry.getKey().ID, keys);
        }

        for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : changes.parentObjects.entrySet()) {
            ArrayList<GGroupObjectValue> keys = new ArrayList<GGroupObjectValue>();

            for (ClientGroupObjectValue keyValue : entry.getValue()) {
                GGroupObjectValue groupObjectValue = convertOrCast(keyValue);
                keys.add(groupObjectValue);
            }

            changesDTO.parentObjects.put(entry.getKey().ID, keys);
        }

        for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> entry : changes.properties.entrySet()) {
            HashMap<GGroupObjectValue, Object> propValues = new HashMap<GGroupObjectValue, Object>();
            for (Map.Entry<ClientGroupObjectValue, Object> clientValues : entry.getValue().entrySet()) {
                GGroupObjectValue groupObjectValue = convertOrCast(clientValues.getKey());

                propValues.put(groupObjectValue, convertOrCast(clientValues.getValue()));
            }
            ClientPropertyReader reader = entry.getKey();
            changesDTO.properties.put(new GPropertyReaderDTO(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1, reader.getType()), propValues);
        }

        for (ClientPropertyReader dropProperty : changes.dropProperties) {
            if (dropProperty instanceof ClientPropertyDraw) {
                changesDTO.dropProperties.add(((ClientPropertyDraw) dropProperty).ID);
            }
        }

        for (ClientPropertyReader panelProperty : changes.panelProperties) {
            changesDTO.panelProperties.add(
                    new GPropertyReaderDTO(panelProperty.getID(), panelProperty.getGroupObject() != null ? panelProperty.getGroupObject().ID : -1, panelProperty.getType())
            );
        }

        return changesDTO;
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
        GGroupObjectValue groupObjectValue = new GGroupObjectValue();
        for (Map.Entry<ClientObject, Object> keyPart : clientGroupObjValue.entrySet()) {
            groupObjectValue.put(keyPart.getKey().ID, keyPart.getValue());
        }
        return groupObjectValue;
    }
}
