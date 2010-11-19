package platform.client.logics;

import platform.base.BaseUtils;
import platform.client.form.GroupObjectController;
import platform.interop.ClassViewType;
import platform.interop.form.PropertyRead;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ClientFormChanges {

    public Boolean dataChanged;

    public String message;

    public final Map<ClientGroupObject, ClassViewType> classViews;
    public final Map<ClientGroupObject, ClientGroupObjectValue> objects;

    // assertion что ObjectInstance из того же GroupObjectInstance
    public final Map<ClientGroupObject, List<ClientGroupObjectValue>> gridObjects;

    // assertion для ключа GroupObjectInstance что в значении ObjectInstance из верхних GroupObjectInstance TreeGroupInstance'а этого ключа,
    // так же может быть ObjectInstance из этого ключа если GroupObject - отображается рекурсивно (тогда надо цеплять к этому GroupObjectValue, иначе к верхнему)
    public Map<ClientGroupObject, List<ClientGroupObjectValue>> parentObjects;

    public final Map<ClientPropertyRead, Map<ClientGroupObjectValue, Object>> properties;
    public final Set<ClientPropertyDraw> panelProperties;
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
            objects.put(clientGroupObject, new ClientGroupObjectValue(inStream, clientGroupObject));
        }

        gridObjects = readGridObjectsMap(inStream, clientForm, false);
        parentObjects = readGridObjectsMap(inStream, clientForm, true);

        //DropProperties
        panelProperties = new HashSet<ClientPropertyDraw>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            panelProperties.add(clientForm.getProperty(inStream.readInt()));
        }

        properties = new HashMap<ClientPropertyRead, Map<ClientGroupObjectValue, Object>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {

            ClientPropertyRead clientPropertyRead;
            switch (inStream.readByte()) {
                case PropertyRead.DRAW:
                    clientPropertyRead = clientForm.getProperty(inStream.readInt());
                    break;
                case PropertyRead.CAPTION:
                    clientPropertyRead = clientForm.getProperty(inStream.readInt()).captionRead;
                    break;
                case PropertyRead.HIGHLIGHT:
                    clientPropertyRead = clientForm.getGroupObject(inStream.readInt());
                    break;
                default:
                    throw new IOException();
            }

            Map<ClientGroupObjectValue, Object> propertyValues = new HashMap<ClientGroupObjectValue, Object>();
            int mapCount = inStream.readInt();
            for (int j = 0; j < mapCount; j++) {
                propertyValues.put(new ClientGroupObjectValue(inStream, clientPropertyRead, panelProperties, classViews, controllers),
                                   BaseUtils.deserializeObject(inStream));
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
        dataChanged = (Boolean) BaseUtils.deserializeObject(inStream);
    }

    private Map<ClientGroupObject, List<ClientGroupObjectValue>> readGridObjectsMap(DataInputStream inStream, ClientForm clientForm, boolean parents) throws IOException {
        Map<ClientGroupObject, List<ClientGroupObjectValue>> gridObjects = new HashMap<ClientGroupObject, List<ClientGroupObjectValue>>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObject clientGroupObject = clientForm.getGroupObject(inStream.readInt());

            List<ClientGroupObjectValue> clientGridObjects = new ArrayList<ClientGroupObjectValue>();
            int listCount = inStream.readInt();
            for (int j = 0; j < listCount; j++) {
                clientGridObjects.add(parents ? inStream.readBoolean() ? new ClientGroupObjectValue() :
                        new ClientGroupObjectValue(clientGroupObject, inStream) : new ClientGroupObjectValue(inStream, clientGroupObject));
            }

            gridObjects.put(clientGroupObject, clientGridObjects);
        }

        return gridObjects;
    }
}
