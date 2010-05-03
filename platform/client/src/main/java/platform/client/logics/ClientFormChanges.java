package platform.client.logics;

import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ClientFormChanges {

    public String message;

    public final Map<ClientGroupObjectImplementView,Byte> classViews;
    public Map<ClientGroupObjectImplementView,ClientGroupObjectValue> objects;
    public Map<ClientGroupObjectImplementView,ClientGroupObjectClass> classes;
    public Map<ClientGroupObjectImplementView,List<ClientGroupObjectValue>> gridObjects;
    public Map<ClientGroupObjectImplementView,List<ClientGroupObjectClass>> gridClasses;
    public Map<ClientPropertyView,Map<ClientGroupObjectValue,Object>> gridProperties;
    public Map<ClientPropertyView,Object> panelProperties;
    public Set<ClientPropertyView> dropProperties;

    public ClientFormChanges(DataInputStream inStream,ClientFormView clientFormView) throws IOException {
        
        classViews = new HashMap<ClientGroupObjectImplementView, Byte>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++)
            classViews.put(clientFormView.getGroupObject(inStream.readInt()),inStream.readByte());

        objects = new HashMap<ClientGroupObjectImplementView, ClientGroupObjectValue>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObjectImplementView clientGroupObject = clientFormView.getGroupObject(inStream.readInt());
            objects.put(clientGroupObject,new ClientGroupObjectValue(inStream, clientGroupObject, true));
        }

        classes = new HashMap<ClientGroupObjectImplementView, ClientGroupObjectClass>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObjectImplementView clientGroupObject = clientFormView.getGroupObject(inStream.readInt());
            classes.put(clientGroupObject,new ClientGroupObjectClass(inStream, clientGroupObject, true));
        }

        gridObjects = new HashMap<ClientGroupObjectImplementView, List<ClientGroupObjectValue>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObjectImplementView clientGroupObject = clientFormView.getGroupObject(inStream.readInt());
            
            List<ClientGroupObjectValue> clientGridObjects = new ArrayList<ClientGroupObjectValue>();
            int listCount = inStream.readInt();
            for (int j = 0; j < listCount; j++)
                clientGridObjects.add(new ClientGroupObjectValue(inStream, clientGroupObject));

            gridObjects.put(clientGroupObject, clientGridObjects);
        }

        gridClasses = new HashMap<ClientGroupObjectImplementView, List<ClientGroupObjectClass>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientGroupObjectImplementView clientGroupObject = clientFormView.getGroupObject(inStream.readInt());

            List<ClientGroupObjectClass> clientGridClasses = new ArrayList<ClientGroupObjectClass>();
            int listCount = inStream.readInt();
            for (int j = 0; j < listCount; j++)
                clientGridClasses.add(new ClientGroupObjectClass(inStream, clientGroupObject, false));

            gridClasses.put(clientGroupObject, clientGridClasses);
        }

//        for (ClientGroupObjectImplementView groupObject : gridObjects.keySet()) {
//            System.out.println(groupObject + " : " + gridObjects.get(groupObject).size());
//        }

        gridProperties = new HashMap<ClientPropertyView, Map<ClientGroupObjectValue, Object>>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientPropertyView clientPropertyView = clientFormView.getProperty(inStream.readInt());

            Map<ClientGroupObjectValue, Object> gridPropertyValues = new HashMap<ClientGroupObjectValue, Object>();
            int mapCount = inStream.readInt();
            for (int j = 0; j < mapCount; j++)
                gridPropertyValues.put(new ClientGroupObjectValue(inStream, clientPropertyView.groupObject),
                                   BaseUtils.deserializeObject(inStream));

            gridProperties.put(clientPropertyView, gridPropertyValues);
        }

        panelProperties = new HashMap<ClientPropertyView, Object>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            panelProperties.put(clientFormView.getProperty(inStream.readInt()),BaseUtils.deserializeObject(inStream));
        }

        //DropProperties
        dropProperties = new HashSet<ClientPropertyView>();
        count = inStream.readInt();
        for (int i = 0; i < count; i++)
            dropProperties.add(clientFormView.getProperty(inStream.readInt()));

        message = inStream.readUTF();
    }
}
