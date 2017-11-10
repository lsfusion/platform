package lsfusion.client.logics;

import lsfusion.base.BaseUtils;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.form.PropertyReadType;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ClientFormChanges {

    public final Map<ClientGroupObject, ClassViewType> classViews;
    public final Map<ClientGroupObject, ClientGroupObjectValue> objects;

    // assertion что ObjectInstance из того же GroupObjectInstance
    public final Map<ClientGroupObject, List<ClientGroupObjectValue>> gridObjects;

    // assertion для ключа GroupObjectInstance что в значении ObjectInstance из верхних GroupObjectInstance TreeGroupInstance'а этого ключа,
    // так же может быть ObjectInstance из этого ключа если GroupObject - отображается рекурсивно (тогда надо цеплять к этому GroupObjectValue, иначе к верхнему)
    public Map<ClientGroupObject, List<ClientGroupObjectValue>> parentObjects;

    public final Map<ClientGroupObject, Map<ClientGroupObjectValue, Boolean>> expandables;

    public final Map<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> properties;
    public final Set<ClientPropertyDraw> updateProperties; // пришедшие значения на обновление, а не изменение

    public final Set<ClientPropertyDraw> panelProperties;
    public final Set<ClientPropertyDraw> dropProperties;

    public ClientFormChanges(DataInputStream inStream, ClientForm clientForm) throws IOException {

        classViews = new HashMap<>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            classViews.put(clientForm.getGroupObject(inStream.readInt()), ClassViewType.values()[inStream.readInt()]);
        }

        objects = new HashMap<>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObject clientGroupObject = clientForm.getGroupObject(inStream.readInt());
            objects.put(clientGroupObject, new ClientGroupObjectValue(inStream, clientForm));
        }

        gridObjects = readGridObjectsMap(inStream, clientForm);
        parentObjects = readGridObjectsMap(inStream, clientForm);

        expandables = new HashMap<>();
        count = inStream.readInt();
        for (int i = 0; i < count; ++i) {
            ClientGroupObject groupObject = clientForm.getGroupObject(inStream.readInt());
            Map<ClientGroupObjectValue, Boolean> groupExpandables = new HashMap<>();
            int cnt = inStream.readInt();
            for (int j = 0; j < cnt; ++j) {
                ClientGroupObjectValue key = new ClientGroupObjectValue(inStream, clientForm);
                boolean expandable = inStream.readBoolean();
                groupExpandables.put(key, expandable);
            }

            expandables.put(groupObject, groupExpandables);
        }

        properties = new HashMap<>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {

            ClientPropertyReader clientPropertyRead = deserializePropertyReader(clientForm, inStream);

            Map<ClientGroupObjectValue, Object> propertyValues = new HashMap<>();
            int mapCount = inStream.readInt();
            for (int j = 0; j < mapCount; j++) {
                propertyValues.put(new ClientGroupObjectValue(inStream, clientForm), BaseUtils.deserializeObject(inStream));
            }

            properties.put(clientPropertyRead, propertyValues);
        }

        updateProperties = new HashSet<>();

        //PanelProperties
        panelProperties = new HashSet<>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            panelProperties.add(clientForm.getProperty(inStream.readInt()));
        }

        //DropProperties
        dropProperties = new HashSet<>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            dropProperties.add(clientForm.getProperty(inStream.readInt()));
        }
    }

    private ClientPropertyReader deserializePropertyReader(ClientForm clientForm, DataInputStream inStream) throws IOException {
        switch (inStream.readByte()) {
            case PropertyReadType.DRAW:
                return clientForm.getProperty(inStream.readInt());
            case PropertyReadType.CAPTION:
                return clientForm.getProperty(inStream.readInt()).captionReader;
            case PropertyReadType.SHOWIF:
                return clientForm.getProperty(inStream.readInt()).showIfReader;
            case PropertyReadType.FOOTER:
                return clientForm.getProperty(inStream.readInt()).footerReader;
            case PropertyReadType.CELL_BACKGROUND:
                return clientForm.getProperty(inStream.readInt()).backgroundReader;
            case PropertyReadType.CELL_FOREGROUND:
                return clientForm.getProperty(inStream.readInt()).foregroundReader;
            case PropertyReadType.READONLY:
                return clientForm.getProperty(inStream.readInt()).readOnlyReader;
            case PropertyReadType.ROW_BACKGROUND:
                return clientForm.getGroupObject(inStream.readInt()).rowBackgroundReader;
            case PropertyReadType.ROW_FOREGROUND:
                return clientForm.getGroupObject(inStream.readInt()).rowForegroundReader;
            default:
                throw new IOException();
        }
    }

    private Map<ClientGroupObject, List<ClientGroupObjectValue>> readGridObjectsMap(DataInputStream inStream, ClientForm clientForm) throws IOException {
        Map<ClientGroupObject, List<ClientGroupObjectValue>> gridObjects = new HashMap<>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObject clientGroupObject = clientForm.getGroupObject(inStream.readInt());

            List<ClientGroupObjectValue> clientGridObjects = new ArrayList<>();
            int listCount = inStream.readInt();
            for (int j = 0; j < listCount; j++) {
                clientGridObjects.add(new ClientGroupObjectValue(inStream, clientForm));
            }

            gridObjects.put(clientGroupObject, clientGridObjects);
        }

        return gridObjects;
    }
}
