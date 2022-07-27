package lsfusion.client.form;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.context.ApplicationContextHolder;
import lsfusion.base.context.ContextIdentityObject;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.controller.remote.serialization.ClientCustomSerializable;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.filter.ClientRegularFilter;
import lsfusion.client.form.filter.ClientRegularFilterGroup;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.object.table.tree.ClientTreeGroup;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.async.ClientAsyncEventExec;
import lsfusion.client.form.property.async.ClientAsyncSerializer;
import lsfusion.interop.form.event.FormEvent;
import lsfusion.interop.form.event.FormScheduler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import static lsfusion.client.ClientResourceBundle.getString;

public class ClientForm extends ContextIdentityObject implements ClientCustomSerializable,
                                                                 ApplicationContextHolder {

    public String canonicalName = "";
    public String creationPath = "";

    public List<FormScheduler> formSchedulers = new ArrayList<>();
    public Map<FormEvent, ClientAsyncEventExec> asyncExecMap;

    public static ClientGroupObject lastActiveGroupObject;

    // пока используется только для сериализации туда-назад
    public Integer overridePageWidth;

    public ClientContainer mainContainer;

    public Set<ClientTreeGroup> treeGroups = new HashSet<>();
    public List<ClientGroupObject> groupObjects = new ArrayList<>();
    public List<ClientPropertyDraw> propertyDraws = new ArrayList<>();

    public OrderedMap<ClientPropertyDraw, Boolean> defaultOrders = new OrderedMap<>();
    public List<ClientRegularFilterGroup> regularFilterGroups = new ArrayList<>();

    public List<List<ClientPropertyDraw>> pivotColumns = new ArrayList<>();
    public List<List<ClientPropertyDraw>> pivotRows = new ArrayList<>();
    public List<ClientPropertyDraw> pivotMeasures = new ArrayList<>();

    public ClientForm() {
    }

    public List<ClientObject> getObjects() {
        ArrayList<ClientObject> objects = new ArrayList<>();
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
            idProps = new HashMap<>();
            for (ClientPropertyDraw property : propertyDraws) {
                idProps.put(property.getID(), property);
            }
        }
        return idProps;
    }

    public ClientPropertyDraw getProperty(int id) {
        return getIDProps().get(id);
    }

    public ClientPropertyDraw getProperty(String propertyFormName) {
        for (ClientPropertyDraw property : propertyDraws) {
            if (property.getPropertyFormName().equals(propertyFormName)) {
                return property;
            }
        }
        return null;
    }

    public String getCaption() {
        return mainContainer.getNotNullCaption();
    }

    public String getTooltip(String caption) {
        return MainController.showDetailedInfo ?
                String.format("<html><body>" +
                        "<b>%s</b><br/><hr>" +
                        "<b>sID:</b> %s<br/>" +
                        "<b>" + getString("logics.scriptpath") + ":</b> %s<br/>" +
                        "</body></html>", caption, canonicalName, creationPath) :
                String.format("<html><body><b>%s</b></body></html>", caption);
    }

    public OrderedMap<ClientPropertyDraw, Boolean> getDefaultOrders(ClientGroupObject group) {
        OrderedMap<ClientPropertyDraw, Boolean> result = new OrderedMap<>();
        for (Map.Entry<ClientPropertyDraw, Boolean> entry : defaultOrders.entrySet()) {
            if (BaseUtils.nullEquals(entry.getKey().groupObject, group)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.serializeObject(outStream, mainContainer);
        pool.serializeCollection(outStream, treeGroups);
        pool.serializeCollection(outStream, groupObjects);
        pool.serializeCollection(outStream, propertyDraws);
        pool.serializeCollection(outStream, regularFilterGroups);

        outStream.writeInt(defaultOrders.size());
        for (Map.Entry<ClientPropertyDraw, Boolean> entry : defaultOrders.entrySet()) {
            pool.serializeObject(outStream, entry.getKey());
            outStream.writeBoolean(entry.getValue());
        }

        pool.writeString(outStream, canonicalName);
        pool.writeString(outStream, creationPath);
        pool.writeInt(outStream, overridePageWidth);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        ClientContainer mainContainer = pool.deserializeObject(inStream);
        mainContainer.main = true;
        this.mainContainer = mainContainer;
        treeGroups = pool.deserializeSet(inStream);
        groupObjects = pool.deserializeList(inStream);
        propertyDraws = pool.deserializeList(inStream);
        regularFilterGroups = pool.deserializeList(inStream);

        defaultOrders = new OrderedMap<>();
        int orderCount = inStream.readInt();
        for (int i = 0; i < orderCount; i++) {
            ClientPropertyDraw order = pool.deserializeObject(inStream);
            defaultOrders.put(order, inStream.readBoolean());
        }

        pivotColumns = deserializePivot(pool, inStream);
        pivotRows = deserializePivot(pool, inStream);
        pivotMeasures = pool.deserializeList(inStream);

        canonicalName = pool.readString(inStream);
        creationPath = pool.readString(inStream);
        overridePageWidth = pool.readInt(inStream);
        formSchedulers = deserializeFormSchedulers(inStream);
        asyncExecMap = deserializeAsyncExecMap(inStream);
    }

    private List<List<ClientPropertyDraw>> deserializePivot(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        List<List<ClientPropertyDraw>> properties = new ArrayList<>();
        int size = inStream.readInt();
        for(int i = 0; i < size; i++) {
            properties.add(pool.deserializeList(inStream));
        }
        return properties;
    }

    private List<FormScheduler> deserializeFormSchedulers(DataInputStream inStream) throws IOException {
        List<FormScheduler> formSchedulers = new ArrayList<>();
        int size = inStream.readInt();
        for(int i = 0; i < size; i++) {
            formSchedulers.add((FormScheduler) FormEvent.deserialize(inStream));
        }
        return formSchedulers;
    }

    private Map<FormEvent, ClientAsyncEventExec> deserializeAsyncExecMap(DataInputStream inStream) throws IOException {
        Map<FormEvent, ClientAsyncEventExec> asyncExecMap = new HashMap<>();
        int size = inStream.readInt();
        for(int i = 0; i < size; i++) {
            asyncExecMap.put(FormEvent.deserialize(inStream), ClientAsyncSerializer.deserializeEventExec(inStream));
        }
        return asyncExecMap;
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

    public ClientContainer findContainerByID(int id) {
        return mainContainer.findContainerByID(id);
    }

    public ClientComponent findComponentByID(int id) {
        return mainContainer.findComponentByID(id);
    }

    public ClientContainer findParentContainerBySID(String sID) {
        return mainContainer.findParentContainerBySID(null, sID);
    }
}
