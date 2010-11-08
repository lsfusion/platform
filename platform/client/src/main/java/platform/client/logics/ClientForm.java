package platform.client.logics;

import platform.base.IdentityObject;
import platform.base.OrderedMap;
import platform.client.SwingUtils;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.form.LogicsSupplier;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientForm extends IdentityObject implements LogicsSupplier, ClientIdentitySerializable {

    public boolean readOnly = false;

    public KeyStroke keyStroke = null;

    public String caption = "";

    // пока используется только для сериализации туда-назад
    public Integer overridePageWidth;

    public ClientContainer mainContainer = new ClientContainer();

    public List<ClientTreeGroup> treeGroups = new ArrayList<ClientTreeGroup>();
    public List<ClientGroupObject> groupObjects = new ArrayList<ClientGroupObject>();
    public List<ClientPropertyDraw> propertyDraws = new ArrayList<ClientPropertyDraw>();

    public OrderedMap<ClientPropertyDraw, Boolean> defaultOrders = new OrderedMap<ClientPropertyDraw, Boolean>();
    public List<ClientRegularFilterGroup> regularFilterGroups = new ArrayList<ClientRegularFilterGroup>();

    public ClientFunction printFunction = new ClientFunction();
    public ClientFunction xlsFunction = new ClientFunction();
    public ClientFunction nullFunction = new ClientFunction();
    public ClientFunction refreshFunction = new ClientFunction();
    public ClientFunction applyFunction = new ClientFunction();
    public ClientFunction cancelFunction = new ClientFunction();
    public ClientFunction okFunction = new ClientFunction();
    public ClientFunction closeFunction = new ClientFunction();

    private List<ClientPropertyDraw> order = new ArrayList<ClientPropertyDraw>();

    public ClientForm() {

    }

    public List<ClientObject> getObjects() {

        ArrayList<ClientObject> objects = new ArrayList<ClientObject>();
        for (ClientGroupObject groupObject : groupObjects) {
            for (ClientObject object : groupObject) {
                objects.add(object);
            }
        }

        return objects;
    }

    public List<ClientPropertyDraw> getPropertyDraws() {
        return propertyDraws;
    }

    public ClientObject getObject(int id) {
        for (ClientGroupObject groupObject : groupObjects) {
            for (ClientObject object : groupObject) {
                if (object.getID() == id) {
                    return object;
                }
            }
        }
        return null;
    }

    public ClientGroupObject getGroupObject(int id) {
        for (ClientGroupObject groupObject : groupObjects) {
            if (groupObject.getID() == id) {
                return groupObject;
            }
        }
        return null;
    }

    public ClientRegularFilterGroup getRegularFilterGroup(int id) {
        for (ClientRegularFilterGroup filterGroup : regularFilterGroups) {
            if (filterGroup.getID() == id) {
                return filterGroup;
            }
        }
        return null;
    }

    public ClientRegularFilter getRegularFilter(int id) {
        for (ClientRegularFilterGroup filterGroup : regularFilterGroups) {
            for (ClientRegularFilter filter : filterGroup.filters) {
                if (filter.ID == id) {
                    return filter;
                }
            }
        }

        return null;
    }

    private Map<Integer, ClientPropertyDraw> idProps;

    private Map<Integer, ClientPropertyDraw> getIDProps() {
        if (idProps == null) {
            idProps = new HashMap<Integer, ClientPropertyDraw>();
            for (ClientPropertyDraw property : propertyDraws) {
                idProps.put(property.getID(), property);
            }
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

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeBoolean(readOnly);
        pool.serializeObject(outStream, mainContainer);
        pool.serializeCollection(outStream, treeGroups, serializationType);
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
        pool.writeInt(outStream, overridePageWidth);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        readOnly = inStream.readBoolean();

        mainContainer = pool.deserializeObject(inStream);
        treeGroups = pool.deserializeList(inStream);
        groupObjects = pool.deserializeList(inStream);
        propertyDraws = pool.deserializeList(inStream);
        regularFilterGroups = pool.deserializeList(inStream);

        defaultOrders = new OrderedMap<ClientPropertyDraw, Boolean>();
        int orderCount = inStream.readInt();
        for (int i = 0; i < orderCount; i++) {
            ClientPropertyDraw order = pool.deserializeObject(inStream);
            defaultOrders.put(order, inStream.readBoolean());
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
        overridePageWidth = pool.readInt(inStream);
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

    public void addToRegularFilterGroups(ClientRegularFilterGroup client) {
        regularFilterGroups.add(client);
    }

    public void removeFromRegularFilterGroups(ClientRegularFilterGroup client) {
        if (client.container != null) {
            client.container.removeFromChildren(client);
        }
        regularFilterGroups.remove(client);
    }

    public ClientContainer findContainerBySID(String sID) {
        return mainContainer.findContainerBySID(sID);
    }

    public void setDefaultOrders(OrderedMap<ClientPropertyDraw, Boolean> defaultOrders) {
        this.defaultOrders = defaultOrders;
        IncrementDependency.update(this, "defaultOrders");
    }

    public OrderedMap<ClientPropertyDraw, Boolean> getDefaultOrders() {
        return defaultOrders;
    }
}
