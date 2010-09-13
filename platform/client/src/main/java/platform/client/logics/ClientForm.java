package platform.client.logics;

import platform.base.OrderedMap;
import platform.client.SwingUtils;
import platform.client.form.LogicsSupplier;
import platform.client.form.decorator.ClientHighlighter;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientForm implements Serializable, LogicsSupplier {

    public boolean readOnly = false;

    public KeyStroke keyStroke = null;

    public String caption = "";

    // нужен именно List, чтобы проще был обход по дереву
    // считается, что containers уже топологически отсортированы
    public final List<ClientContainer> containers;

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

    private final List<ClientPropertyDraw> order = new ArrayList<ClientPropertyDraw>();

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

    public ClientGroupObject getGroupObject(int id) {
        for (ClientGroupObject groupObject : groupObjects)
            if (groupObject.getID() == id) return groupObject;
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

        Map<String, ClientPropertyDraw> sIDtoProperty = new HashMap();
        for (ClientPropertyDraw property : properties) {
            sIDtoProperty.put(property.getSID(), property);
        }
        //заполняем свойства в колонках
        for (ClientPropertyDraw property : properties) {
            property.columnDisplayProperties = new ClientPropertyDraw[property.columnDisplayPropertiesIds.length];
            for (int i = 0; i < property.columnDisplayProperties.length; ++i) {
                property.columnDisplayProperties[i] = getProperty(property.columnDisplayPropertiesIds[i]);
            }
        }

        for (ClientGroupObject groupObject : groupObjects) {
            ClientHighlighter highlighter = groupObject.grid.highlighter;
            if (highlighter != null) {
                highlighter.init(sIDtoProperty);
            }
        }

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
}
