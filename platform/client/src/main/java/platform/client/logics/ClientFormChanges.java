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
    public final Map<ClientGroupObject,ClientGroupObjectValue> objects;
    public final Map<ClientGroupObject,List<ClientGroupObjectValue>> gridObjects;

    public final Map<ClientPropertyRead,Map<ClientGroupObjectValue,Object>> properties;
    public final Set<ClientPropertyDraw> panelProperties;
    public final Set<ClientPropertyDraw> dropProperties;

    public ClientFormChanges(DataInputStream inStream, ClientForm clientForm, Map<ClientGroupObject, GroupObjectController> controllers) throws IOException {

        classViews = new HashMap<ClientGroupObject, ClassViewType>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++)
            classViews.put(clientForm.getGroupObject(inStream.readInt()), ClassViewType.values()[inStream.readInt()]);

        objects = new HashMap<ClientGroupObject, ClientGroupObjectValue>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObject clientGroupObject = clientForm.getGroupObject(inStream.readInt());
            objects.put(clientGroupObject, new ClientGroupObjectValue(inStream, clientGroupObject));
        }

        gridObjects = new HashMap<ClientGroupObject, List<ClientGroupObjectValue>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObject clientGroupObject = clientForm.getGroupObject(inStream.readInt());
            
            List<ClientGroupObjectValue> clientGridObjects = new ArrayList<ClientGroupObjectValue>();
            int listCount = inStream.readInt();
            for (int j = 0; j < listCount; j++) {
                clientGridObjects.add(new ClientGroupObjectValue(inStream, clientGroupObject));
            }

            gridObjects.put(clientGroupObject, clientGridObjects);
        }

        //DropProperties
        panelProperties = new HashSet<ClientPropertyDraw>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++)
            panelProperties.add(clientForm.getProperty(inStream.readInt()));

        properties = new HashMap<ClientPropertyRead, Map<ClientGroupObjectValue, Object>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {

            ClientPropertyRead clientPropertyRead;
            switch(inStream.readByte()) {
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
        for (int i = 0; i < count; i++)
            dropProperties.add(clientForm.getProperty(inStream.readInt()));

        message = inStream.readUTF();
        dataChanged = (Boolean) BaseUtils.deserializeObject(inStream);
    }
}
