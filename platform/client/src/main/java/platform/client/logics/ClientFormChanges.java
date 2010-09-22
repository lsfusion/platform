package platform.client.logics;

import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ClientFormChanges {

    public Boolean dataChanged; 

    public String message;

    public final Map<ClientGroupObject,Byte> classViews;
    public final Map<ClientGroupObject,ClientGroupObjectValue> objects;
    public final Map<ClientGroupObject,List<ClientGroupObjectValue>> gridObjects;

    public final Map<ClientPropertyDraw,Map<ClientGroupObjectValue,Object>> properties;
    public final Set<ClientPropertyDraw> panelProperties;
    public final Set<ClientPropertyDraw> dropProperties;

    public ClientFormChanges()  {
        classViews = new HashMap<ClientGroupObject, Byte>();
        objects = new HashMap<ClientGroupObject, ClientGroupObjectValue>();
        gridObjects = new HashMap<ClientGroupObject, List<ClientGroupObjectValue>>();
        properties = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
        panelProperties = new HashSet<ClientPropertyDraw>(); 
        dropProperties = new HashSet<ClientPropertyDraw>();
        message = null;
        dataChanged = false;
    }
    
    public ClientFormChanges(DataInputStream inStream, ClientForm clientForm) throws IOException {

        classViews = new HashMap<ClientGroupObject, Byte>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++)
            classViews.put(clientForm.getGroupObject(inStream.readInt()),inStream.readByte());

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

        properties = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientPropertyDraw clientPropertyDraw = clientForm.getProperty(inStream.readInt());

            Map<ClientGroupObjectValue, Object> propertyValues = new HashMap<ClientGroupObjectValue, Object>();
            int mapCount = inStream.readInt();
            for (int j = 0; j < mapCount; j++) {
                propertyValues.put(new ClientGroupObjectValue(inStream, clientPropertyDraw, !panelProperties.contains(clientPropertyDraw)),
                        BaseUtils.deserializeObject(inStream));
            }

            properties.put(clientPropertyDraw, propertyValues);
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
