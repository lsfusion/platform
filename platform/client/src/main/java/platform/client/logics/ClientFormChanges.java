package platform.client.logics;

import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ClientFormChanges {

    public Boolean dataChanged; 

    public String message;

    public Map<ClientGroupObject,Byte> classViews;
    public Map<ClientGroupObject,ClientGroupObjectValue> objects;
    public Map<ClientGroupObject,List<ClientGroupObjectValue>> gridObjects;
    public Map<ClientPropertyDraw,Map<ClientGroupObjectValue,Object>> gridProperties;
    public Map<ClientPropertyDraw,Map<ClientGroupObjectValue,Object>> panelProperties;
    public Set<ClientPropertyDraw> dropProperties;

    public ClientFormChanges()  {
        classViews = new HashMap<ClientGroupObject, Byte>();
        objects = new HashMap<ClientGroupObject, ClientGroupObjectValue>();
        gridObjects = new HashMap<ClientGroupObject, List<ClientGroupObjectValue>>();
        gridProperties = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
        panelProperties = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
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

        gridProperties = deserializeProperties(inStream, clientForm, true);
        panelProperties = deserializeProperties(inStream, clientForm, false);

        //DropProperties
        dropProperties = new HashSet<ClientPropertyDraw>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++)
            dropProperties.add(clientForm.getProperty(inStream.readInt()));

        message = inStream.readUTF();
        dataChanged = (Boolean) BaseUtils.deserializeObject(inStream);
    }

    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> deserializeProperties(DataInputStream inStream,
                                                                                               ClientForm clientForm,
                                                                                               boolean deserializeKeys) throws IOException {
        Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> properties = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientPropertyDraw clientPropertyDraw = clientForm.getProperty(inStream.readInt());

            Map<ClientGroupObjectValue, Object> propertyValues = new HashMap<ClientGroupObjectValue, Object>();
            int mapCount = inStream.readInt();
            for (int j = 0; j < mapCount; j++) {
                propertyValues.put(new ClientGroupObjectValue(inStream, clientPropertyDraw, deserializeKeys),
                        BaseUtils.deserializeObject(inStream));
            }

            properties.put(clientPropertyDraw, propertyValues);
        }
        return properties;
    }
}
