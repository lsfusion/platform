package platform.client.logics;

import platform.base.OrderedMap;
import platform.client.SwingUtils;
import platform.client.form.LogicsSupplier;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientForm implements LogicsSupplier, ClientIdentitySerializable {

    public boolean readOnly = false;

    public KeyStroke keyStroke = null;

    public String caption = "";

    public ClientContainer mainContainer;
    private int ID;

    public List<ClientGroupObject> groupObjects;
    public List<ClientPropertyDraw> propertyDraws;

    public final OrderedMap<ClientPropertyDraw,Boolean> defaultOrders = new OrderedMap<ClientPropertyDraw, Boolean>();
    public List<ClientRegularFilterGroup> regularFilterGroups;

    public ClientFunction printFunction;
    public ClientFunction xlsFunction;
    public ClientFunction nullFunction;
    public ClientFunction refreshFunction;
    public ClientFunction applyFunction;
    public ClientFunction cancelFunction;
    public ClientFunction okFunction;
    public ClientFunction closeFunction;

    private List<ClientPropertyDraw> order = new ArrayList<ClientPropertyDraw>();

    public ClientForm() {

    }

    public List<ClientObject> getObjects() {

         ArrayList<ClientObject> objects = new ArrayList<ClientObject> ();
         for (ClientGroupObject groupObject : groupObjects)
             for (ClientObject object : groupObject)
                 objects.add(object);

         return objects;
     }

     public List<ClientPropertyDraw> getPropertyDraws() {
         return propertyDraws;
     }

    public ClientObject getObject(int id) {
        for (ClientGroupObject groupObject : groupObjects)
            for(ClientObject object : groupObject)
                if (object.getID() == id) return object;
        return null;
    }

    public ClientGroupObject getGroupObject(int id) {
        for (ClientGroupObject groupObject : groupObjects)
            if (groupObject.getID() == id) return groupObject;
        return null;
    }

    public ClientRegularFilterGroup getRegularFilterGroup(int id) {
        for (ClientRegularFilterGroup filterGroup : regularFilterGroups) {
            if (filterGroup.ID == id) {
                return filterGroup;
            }
        }
        return null;
    }

    public ClientRegularFilter getRegularFilter(int id) {
        for (ClientRegularFilterGroup filterGroup : regularFilterGroups) {
            for (ClientRegularFilter filter : filterGroup.filters) {
                if (filter.ID == id) return filter;
            }
        }

        return null;
    }

    private Map<Integer, ClientPropertyDraw> idProps;
    private Map<Integer, ClientPropertyDraw> getIDProps() {
        if(idProps==null) {
            idProps = new HashMap<Integer, ClientPropertyDraw>();
            for(ClientPropertyDraw property : propertyDraws)
                idProps.put(property.getID(), property);
        }
        return idProps;
    }
    public ClientPropertyDraw getProperty(int id) {
        return getIDProps().get(id);
    }

    public String getFullCaption() {
        if (keyStroke != null) {
            StringBuilder fullCaption = new StringBuilder(caption);
            fullCaption.append(" (");
            fullCaption.append(SwingUtils.getKeyStrokeCaption(keyStroke));
            fullCaption.append(")");

            return fullCaption.toString();
        }
        return caption;
    }

    public int getID() {
        return ID;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeBoolean(readOnly);
        pool.serializeObject(outStream, mainContainer);
        pool.serializeCollection(outStream, groupObjects);
        pool.serializeCollection(outStream, propertyDraws);
        pool.serializeCollection(outStream, regularFilterGroups);

        outStream.writeInt(defaultOrders.size());
        for (Map.Entry<ClientPropertyDraw, Boolean> entry : defaultOrders.entrySet()) {
            pool.serializeObject(outStream, entry.getKey());
            outStream.writeBoolean(entry.getValue());
        }

        pool.serializeObject(outStream, printFunction);
        pool.serializeObject(outStream, xlsFunction);
        pool.serializeObject(outStream, nullFunction);
        pool.serializeObject(outStream, refreshFunction);
        pool.serializeObject(outStream, applyFunction);
        pool.serializeObject(outStream, cancelFunction);
        pool.serializeObject(outStream, okFunction);
        pool.serializeObject(outStream, closeFunction);

        pool.serializeCollection(outStream, order);

        pool.writeObject(outStream, keyStroke);
        pool.writeString(outStream, caption);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
        
        readOnly = inStream.readBoolean();

        mainContainer = pool.deserializeObject(inStream);
        groupObjects = pool.deserializeList(inStream);
        propertyDraws = pool.deserializeList(inStream);
        regularFilterGroups = pool.deserializeList(inStream);

        int orderCount = inStream.readInt();
        for(int i=0;i<orderCount;i++) {
            ClientPropertyDraw order = pool.deserializeObject(inStream);
            defaultOrders.put(order,inStream.readBoolean());
        }

        printFunction = pool.deserializeObject(inStream);
        xlsFunction = pool.deserializeObject(inStream);
        nullFunction = pool.deserializeObject(inStream);
        refreshFunction = pool.deserializeObject(inStream);
        applyFunction = pool.deserializeObject(inStream);
        cancelFunction = pool.deserializeObject(inStream);
        okFunction = pool.deserializeObject(inStream);
        closeFunction = pool.deserializeObject(inStream);

        order = pool.deserializeList(inStream);

        keyStroke = pool.readObject(inStream);

        caption = pool.readString(inStream);
    }

    public boolean removePropertyDraw(ClientPropertyDraw clientPropertyDraw) {
        if (clientPropertyDraw.container != null) {
            clientPropertyDraw.container.removeFromChildren(clientPropertyDraw);
        }
        propertyDraws.remove(clientPropertyDraw);
        order.remove(clientPropertyDraw);
        defaultOrders.remove(clientPropertyDraw);

        //drop caches
        idProps = null;

        return true;
    }

    public boolean removeGroupObject(ClientGroupObject groupObject) {
        groupObjects.remove(groupObject);
        //todo: what about properties

        return true;
    }

    public void removeFromRegularFilterGroups(ClientRegularFilterGroup client) {
        if (client.container != null)
            client.container.removeFromChildren(client);
        regularFilterGroups.remove(client);
    }
}
