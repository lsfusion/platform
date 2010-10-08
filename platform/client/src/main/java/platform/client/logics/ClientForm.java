package platform.client.logics;

import platform.base.OrderedMap;
import platform.client.SwingUtils;
import platform.client.form.LogicsSupplier;
import platform.client.serialization.ClientCustomSerializable;
import platform.client.serialization.ClientSerializationPool;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientForm implements Serializable, LogicsSupplier, ClientCustomSerializable {

    public boolean readOnly = false;

    public KeyStroke keyStroke = null;

    public String caption = "";

    // нужен именно List, чтобы проще был обход по дереву
    // считается, что containers уже топологически отсортированы
    public List<ClientContainer> containers;
    public ClientContainer getMainContainer() {
        for (ClientContainer container : containers)
            if (container.container == null)
                return container;
        return null;
    }

    public List<ClientGroupObject> groupObjects;
    private List<ClientPropertyDraw> properties;

    public final OrderedMap<ClientPropertyDraw,Boolean> defaultOrders = new OrderedMap<ClientPropertyDraw, Boolean>();
    public List<ClientRegularFilterGroup> regularFilters;

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

    public ClientForm(DataInputStream inStream) throws IOException, ClassNotFoundException {

        readOnly = inStream.readBoolean();
        /// !!!! самому вернуть ссылку на groupObject после инстанцирования

        containers = new ArrayList<ClientContainer>();
        int count = inStream.readInt();
        for(int i=0;i<count;i++)
            containers.add(new ClientContainer(inStream,containers));

        groupObjects = deserializeList(inStream,new DeSerializeInstancer<ClientGroupObject>() {
            ClientGroupObject newObject(DataInputStream inStream) throws IOException, ClassNotFoundException {
                return new ClientGroupObject(inStream,containers);
            }});
        properties = deserializeList(inStream,new DeSerializeInstancer<ClientPropertyDraw>() {
            ClientPropertyDraw newObject(DataInputStream inStream) throws IOException, ClassNotFoundException {
                return new ClientPropertyDraw(inStream,containers,groupObjects);
            }});
        regularFilters = deserializeList(inStream,new DeSerializeInstancer<ClientRegularFilterGroup>() {
            ClientRegularFilterGroup newObject(DataInputStream inStream) throws IOException, ClassNotFoundException {
                return new ClientRegularFilterGroup(inStream,containers);
            }});

        int orderCount = inStream.readInt();
        for(int i=0;i<orderCount;i++) {
            ClientPropertyDraw order = getProperty(inStream.readInt());
            defaultOrders.put(order,inStream.readBoolean());
        }

        printFunction = new ClientFunction(inStream,containers);
        xlsFunction = new ClientFunction(inStream,containers);
        nullFunction = new ClientFunction(inStream,containers);
        refreshFunction = new ClientFunction(inStream,containers);
        applyFunction = new ClientFunction(inStream,containers);
        cancelFunction = new ClientFunction(inStream,containers);
        okFunction = new ClientFunction(inStream,containers);
        closeFunction = new ClientFunction(inStream,containers);

        int cellCount = inStream.readInt();
        for(int i=0;i<cellCount;i++) {
            int cellID = inStream.readInt();
            order.add(getProperty(cellID));
        }

        keyStroke = (KeyStroke) new ObjectInputStream(inStream).readObject();

        caption = inStream.readUTF();
    }

    public List<ClientObject> getObjects() {

         ArrayList<ClientObject> objects = new ArrayList<ClientObject> ();
         for (ClientGroupObject groupObject : groupObjects)
             for (ClientObject object : groupObject)
                 objects.add(object);

         return objects;
     }

     public List<ClientPropertyDraw> getProperties() {
         return properties;
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
        for (ClientRegularFilterGroup filterGroup : regularFilters) {
            if (filterGroup.ID == id) {
                return filterGroup;
            }
        }
        return null;
    }

    public ClientRegularFilter getRegularFilter(int id) {
        for (ClientRegularFilterGroup filterGroup : regularFilters) {
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
            for(ClientPropertyDraw property : properties)
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

    abstract class DeSerializeInstancer<T> {
          abstract T newObject(DataInputStream inStream) throws IOException, ClassNotFoundException;
    }

    <T> List<T> deserializeList(DataInputStream inStream, DeSerializeInstancer<T> instancer) throws IOException, ClassNotFoundException {
        List<T> list = new ArrayList<T>();
        int count = inStream.readInt();
        for(int i=0;i<count;i++)
            list.add(instancer.newObject(inStream));
        return list;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeBoolean(readOnly);
        pool.serializeCollection(outStream, containers);
        pool.serializeCollection(outStream, groupObjects);
        pool.serializeCollection(outStream, properties);
        pool.serializeCollection(outStream, regularFilters);

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
        readOnly = inStream.readBoolean();

        containers = pool.deserializeList(inStream);
        groupObjects = pool.deserializeList(inStream);
        properties = pool.deserializeList(inStream);
        regularFilters = pool.deserializeList(inStream);

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
}
