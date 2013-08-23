package lsfusion.client.logics;

import lsfusion.base.OrderedMap;
import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ApplicationContextHolder;
import lsfusion.base.identity.IdentityObject;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.LogicsSupplier;
import lsfusion.client.serialization.ClientIdentitySerializable;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.AbstractForm;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientForm extends IdentityObject implements LogicsSupplier, ClientIdentitySerializable,
                                                          AbstractForm<ClientContainer, ClientComponent>,
                                                          ApplicationContextHolder {

    public KeyStroke keyStroke = null;

    public String caption = "";

    public int autoRefresh = 0;

    public static ClientGroupObject lastActiveGroupObject;

    // пока используется только для сериализации туда-назад
    public Integer overridePageWidth;

    public ClientContainer mainContainer;

    public List<ClientTreeGroup> treeGroups = new ArrayList<ClientTreeGroup>();
    public List<ClientGroupObject> groupObjects = new ArrayList<ClientGroupObject>();
    public List<ClientPropertyDraw> propertyDraws = new ArrayList<ClientPropertyDraw>();

    public OrderedMap<ClientPropertyDraw, Boolean> defaultOrders = new OrderedMap<ClientPropertyDraw, Boolean>();
    public List<ClientRegularFilterGroup> regularFilterGroups = new ArrayList<ClientRegularFilterGroup>();

    public ClientContainer getMainContainer() {
        return mainContainer;
    }

    private ApplicationContext context;
    public ApplicationContext getContext() {
        return context;
    }
    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    public ClientForm() {
    }

    // этот конструктор используется при создании нового объекта в настройке бизнес-логики
    public ClientForm(int ID, ApplicationContext context) {
        super(ID);
        
        this.context = context;

        mainContainer = new ClientContainer(getContext());
    }

    public List<ClientObject> getObjects() {

        ArrayList<ClientObject> objects = new ArrayList<ClientObject>();
        for (ClientGroupObject groupObject : groupObjects) {
            for (ClientObject object : groupObject.objects) {
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
            for (ClientObject object : groupObject.objects) {
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

    public ClientTreeGroup getTreeGroup(int id) {
        for (ClientTreeGroup treeGroup : treeGroups) {
            if (treeGroup.getID() == id) {
                return treeGroup;
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

    public ClientPropertyDraw getProperty(String sid) {
        for (ClientPropertyDraw property : propertyDraws) {
            if (property.getSID().equals(sid)) {
                return property;
            }
        }
        return null;
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

        pool.writeObject(outStream, keyStroke);
        pool.writeString(outStream, caption);
        pool.writeInt(outStream, overridePageWidth);
        outStream.writeInt(autoRefresh);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
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

        keyStroke = pool.readObject(inStream);
        caption = pool.readString(inStream);
        overridePageWidth = pool.readInt(inStream);
        autoRefresh = inStream.readInt();
    }

    public boolean removePropertyDraw(ClientPropertyDraw clientPropertyDraw) {
        if (clientPropertyDraw.container != null) {
            clientPropertyDraw.container.removeFromChildren(clientPropertyDraw);
        }
        propertyDraws.remove(clientPropertyDraw);
        defaultOrders.remove(clientPropertyDraw);

        //drop caches
        idProps = null;

        return true;
    }

    public boolean removeGroupObject(ClientGroupObject groupObject) {
        groupObjects.remove(groupObject);

        ClientContainer groupContainer = groupObject.getClientComponent(mainContainer);
        if (groupContainer != null) {
            groupContainer.container.removeFromChildren(groupContainer);
        }

        return true;
    }

    public boolean removeTreeGroup(ClientTreeGroup treeGroup) {
        treeGroups.remove(treeGroup);
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
}
