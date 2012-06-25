package platform.client.logics;

import platform.base.BaseUtils;
import platform.client.form.GroupObjectController;
import platform.gwt.view.changes.dto.GFormChangesDTO;
import platform.gwt.view.changes.dto.GGroupObjectValueDTO;
import platform.gwt.view.changes.dto.GPropertyReaderDTO;
import platform.gwt.view.changes.dto.ObjectDTO;
import platform.interop.ClassViewType;
import platform.interop.form.PropertyReadType;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClientFormChanges {

    public String message;

    public final Map<ClientGroupObject, ClassViewType> classViews;
    public final Map<ClientGroupObject, ClientGroupObjectValue> objects;

    // assertion что ObjectInstance из того же GroupObjectInstance
    public final Map<ClientGroupObject, List<ClientGroupObjectValue>> gridObjects;

    // assertion для ключа GroupObjectInstance что в значении ObjectInstance из верхних GroupObjectInstance TreeGroupInstance'а этого ключа,
    // так же может быть ObjectInstance из этого ключа если GroupObject - отображается рекурсивно (тогда надо цеплять к этому GroupObjectValue, иначе к верхнему)
    public Map<ClientGroupObject, List<ClientGroupObjectValue>> parentObjects;

    public final Map<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> properties;
    public final Set<ClientPropertyReader> panelProperties;
    public final Set<ClientPropertyDraw> dropProperties;

    public ClientFormChanges(DataInputStream inStream, ClientForm clientForm, Map<ClientGroupObject, GroupObjectController> controllers) throws IOException {

        classViews = new HashMap<ClientGroupObject, ClassViewType>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            classViews.put(clientForm.getGroupObject(inStream.readInt()), ClassViewType.values()[inStream.readInt()]);
        }

        objects = new HashMap<ClientGroupObject, ClientGroupObjectValue>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObject clientGroupObject = clientForm.getGroupObject(inStream.readInt());
            objects.put(clientGroupObject, new ClientGroupObjectValue(inStream, clientForm));
        }

        gridObjects = readGridObjectsMap(inStream, clientForm);
        parentObjects = readGridObjectsMap(inStream, clientForm);

        //DropProperties
        panelProperties = new HashSet<ClientPropertyReader>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            panelProperties.add(deserializePropertyReader(clientForm, inStream));
        }

        properties = new HashMap<ClientPropertyReader, Map<ClientGroupObjectValue, Object>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {

            ClientPropertyReader clientPropertyRead = deserializePropertyReader(clientForm, inStream);

            Map<ClientGroupObjectValue, Object> propertyValues = new HashMap<ClientGroupObjectValue, Object>();
            int mapCount = inStream.readInt();
            for (int j = 0; j < mapCount; j++) {
                propertyValues.put(new ClientGroupObjectValue(inStream, clientForm), BaseUtils.deserializeObject(inStream));
            }

            properties.put(clientPropertyRead, propertyValues);
        }

        //DropProperties
        dropProperties = new HashSet<ClientPropertyDraw>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            dropProperties.add(clientForm.getProperty(inStream.readInt()));
        }

        message = inStream.readUTF();
    }

    private ClientPropertyReader deserializePropertyReader(ClientForm clientForm, DataInputStream inStream) throws IOException {
        switch (inStream.readByte()) {
            case PropertyReadType.DRAW:
                return clientForm.getProperty(inStream.readInt());
            case PropertyReadType.CAPTION:
                return clientForm.getProperty(inStream.readInt()).captionReader;
            case PropertyReadType.FOOTER:
                return clientForm.getProperty(inStream.readInt()).footerReader;
            case PropertyReadType.CELL_BACKGROUND:
                return clientForm.getProperty(inStream.readInt()).backgroundReader;
            case PropertyReadType.CELL_FOREGROUND:
                return clientForm.getProperty(inStream.readInt()).foregroundReader;
            case PropertyReadType.ROW_BACKGROUND:
                return clientForm.getGroupObject(inStream.readInt()).rowBackgroundReader;
            case PropertyReadType.ROW_FOREGROUND:
                return clientForm.getGroupObject(inStream.readInt()).rowForegroundReader;
            default:
                throw new IOException();
        }
    }

    private Map<ClientGroupObject, List<ClientGroupObjectValue>> readGridObjectsMap(DataInputStream inStream, ClientForm clientForm) throws IOException {
        Map<ClientGroupObject, List<ClientGroupObjectValue>> gridObjects = new HashMap<ClientGroupObject, List<ClientGroupObjectValue>>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObject clientGroupObject = clientForm.getGroupObject(inStream.readInt());

            List<ClientGroupObjectValue> clientGridObjects = new ArrayList<ClientGroupObjectValue>();
            int listCount = inStream.readInt();
            for (int j = 0; j < listCount; j++) {
                clientGridObjects.add(new ClientGroupObjectValue(inStream, clientForm));
            }

            gridObjects.put(clientGroupObject, clientGridObjects);
        }

        return gridObjects;
    }

    private GFormChangesDTO gwtFormChanges;
    public GFormChangesDTO getGwtFormChangesDTO() {
        if (gwtFormChanges == null) {
            gwtFormChanges = new GFormChangesDTO();

            for (Map.Entry<ClientGroupObject, ClassViewType> entry : classViews.entrySet()) {
                gwtFormChanges.classViews.put(entry.getKey().getID(), new ObjectDTO(entry.getValue().name()));
            }

            for (Map.Entry<ClientGroupObject, ClientGroupObjectValue> e : objects.entrySet()) {
                gwtFormChanges.objects.put(e.getKey().ID, e.getValue().getGwtGroupObjectValueDTO());
            }

            for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : gridObjects.entrySet()) {
                ArrayList<GGroupObjectValueDTO> keys = new ArrayList<GGroupObjectValueDTO>();

                for (ClientGroupObjectValue keyValue : entry.getValue()) {
                    keys.add(keyValue.getGwtGroupObjectValueDTO());
                }

                gwtFormChanges.gridObjects.put(entry.getKey().ID, keys);
            }

            for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : parentObjects.entrySet()) {
                ArrayList<GGroupObjectValueDTO> keys = new ArrayList<GGroupObjectValueDTO>();

                for (ClientGroupObjectValue keyValue : entry.getValue()) {
                    keys.add(keyValue.getGwtGroupObjectValueDTO());
                }

                gwtFormChanges.parentObjects.put(entry.getKey().ID, keys);
            }

            for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> entry : properties.entrySet()) {
                HashMap<GGroupObjectValueDTO, ObjectDTO> propValues = new HashMap<GGroupObjectValueDTO, ObjectDTO>();
                for (Map.Entry<ClientGroupObjectValue, Object> clientValues : entry.getValue().entrySet()) {
                    propValues.put(clientValues.getKey().getGwtGroupObjectValueDTO(), new ObjectDTO(convertValue(clientValues.getValue())));
                }
                ClientPropertyReader reader = entry.getKey();
                gwtFormChanges.properties.put(new GPropertyReaderDTO(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1, reader.getType()), propValues);
            }

            for (ClientPropertyReader dropProperty : dropProperties) {
                if (dropProperty instanceof ClientPropertyDraw) {
                    gwtFormChanges.dropProperties.add(((ClientPropertyDraw) dropProperty).ID);
                }
            }

            for (ClientPropertyReader panelProperty : panelProperties) {
                gwtFormChanges.panelProperties.add(new GPropertyReaderDTO(panelProperty.getID(), panelProperty.getGroupObject() != null ? panelProperty.getGroupObject().ID : -1, panelProperty.getType()));
            }
        }
        return gwtFormChanges;
    }

    public Object convertValue(Object value) {
        if (value instanceof Color) {
            return "#" + Integer.toHexString(((Color) value).getRGB()).substring(2, 8);
        } else {
            return value;
        }
    }
}
