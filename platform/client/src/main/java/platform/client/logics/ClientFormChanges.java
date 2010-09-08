package platform.client.logics;

import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ClientFormChanges {

    public Boolean dataChanged; 

    public String message;

    public final Map<ClientGroupObject,Byte> classViews;
    public Map<ClientGroupObject,ClientGroupObjectValue> objects;
    public Map<ClientGroupObject,ClientGroupObjectClass> classes;
    public Map<ClientGroupObject,List<ClientGroupObjectValue>> gridObjects;
    public Map<ClientGroupObject,List<ClientGroupObjectClass>> gridClasses;
    public Map<ClientPropertyDraw,Map<ClientGroupObjectValue,Object>> gridProperties;
    public Map<ClientPropertyDraw,Object> panelProperties;
    public Set<ClientPropertyDraw> dropProperties;

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

        classes = new HashMap<ClientGroupObject, ClientGroupObjectClass>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObject clientGroupObject = clientForm.getGroupObject(inStream.readInt());
            classes.put(clientGroupObject,new ClientGroupObjectClass(inStream, clientGroupObject, true));
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

        gridClasses = new HashMap<ClientGroupObject, List<ClientGroupObjectClass>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObject clientGroupObject = clientForm.getGroupObject(inStream.readInt());

            List<ClientGroupObjectClass> clientGridClasses = new ArrayList<ClientGroupObjectClass>();
            int listCount = inStream.readInt();
            for (int j = 0; j < listCount; j++)
                clientGridClasses.add(new ClientGroupObjectClass(inStream, clientGroupObject, false));

            gridClasses.put(clientGroupObject, clientGridClasses);
        }

//        for (ClientGroupObject groupObject : gridObjects.keySet()) {
//            System.out.println(groupObject + " : " + gridObjects.get(groupObject).size());
//        }

        gridProperties = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientPropertyDraw clientPropertyDraw = clientForm.getProperty(inStream.readInt());

            Map<ClientGroupObjectValue, Object> gridPropertyValues = new HashMap<ClientGroupObjectValue, Object>();
            int mapCount = inStream.readInt();
            for (int j = 0; j < mapCount; j++) {
                gridPropertyValues.put(new ClientGroupObjectValue(inStream, clientPropertyDraw),
                        BaseUtils.deserializeObject(inStream));
            }

            gridProperties.put(clientPropertyDraw, gridPropertyValues);
        }

        panelProperties = new HashMap<ClientPropertyDraw, Object>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            panelProperties.put(clientForm.getProperty(inStream.readInt()), BaseUtils.deserializeObject(inStream));
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
