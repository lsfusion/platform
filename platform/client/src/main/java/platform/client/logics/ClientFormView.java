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

public class ClientFormView implements Serializable, LogicsSupplier {

    public boolean readOnly = false;

    public KeyStroke keyStroke = null;

    public String caption = "";

    // нужен именно List, чтобы проще был обход по дереву
    // считается, что containers уже топологически отсортированы
    public final List<ClientContainerView> containers;

    public List<ClientGroupObjectImplementView> groupObjects;
    private List<ClientPropertyView> properties;

    public final OrderedMap<ClientCellView,Boolean> defaultOrders = new OrderedMap<ClientCellView, Boolean>();
    public List<ClientRegularFilterGroupView> regularFilters;

    public ClientFunctionView printView;
    public ClientFunctionView xlsView;
    public ClientFunctionView nullView;
    public ClientFunctionView refreshView;
    public ClientFunctionView applyView;
    public ClientFunctionView cancelView;
    public ClientFunctionView okView;
    public ClientFunctionView closeView;

    private final List<ClientCellView> order = new ArrayList<ClientCellView>();

    public List<ClientObjectImplementView> getObjects() {

         ArrayList<ClientObjectImplementView> objects = new ArrayList<ClientObjectImplementView> ();
         for (ClientGroupObjectImplementView groupObject : groupObjects)
             for (ClientObjectImplementView object : groupObject)
                 objects.add(object);

         return objects;
     }

     public List<ClientPropertyView> getProperties() {
         return properties;
     }

     public List<ClientCellView> getCells() {
         return order;
     }

    public ClientGroupObjectImplementView getGroupObject(int id) {
        for (ClientGroupObjectImplementView groupObject : groupObjects)
            if (groupObject.getID() == id) return groupObject;
        return null;
    }

    ClientCellView getObject(int id, boolean classView) {
        for (ClientGroupObjectImplementView groupObject : groupObjects)
            for (ClientObjectImplementView object : groupObject)
                if (object.getID() == id) {
                    if (classView)
                        return object.classCellView;
                    else
                        return object.objectCellView;
                }
        return null;
    }

    private Map<Integer, ClientPropertyView> idProps;
    private Map<Integer, ClientPropertyView> getIDProps() {
        if(idProps==null) {
            idProps = new HashMap<Integer, ClientPropertyView>();
            for(ClientPropertyView property : properties)
                idProps.put(property.getID(), property);
        }
        return idProps;
    }
    public ClientPropertyView getProperty(int id) {
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

    public ClientFormView(DataInputStream inStream) throws IOException, ClassNotFoundException {

        readOnly = inStream.readBoolean();
        /// !!!! самому вернуть ссылку на groupObject после инстанцирования

        containers = new ArrayList<ClientContainerView>();
        int count = inStream.readInt();
        for(int i=0;i<count;i++)
            containers.add(new ClientContainerView(inStream,containers));

        groupObjects = deserializeList(inStream,new DeSerializeInstancer<ClientGroupObjectImplementView>() {
            ClientGroupObjectImplementView newObject(DataInputStream inStream) throws IOException, ClassNotFoundException {
                return new ClientGroupObjectImplementView(inStream,containers);
            }});
        properties = deserializeList(inStream,new DeSerializeInstancer<ClientPropertyView>() {
            ClientPropertyView newObject(DataInputStream inStream) throws IOException, ClassNotFoundException {
                return new ClientPropertyView(inStream,containers,groupObjects);
            }});
        regularFilters = deserializeList(inStream,new DeSerializeInstancer<ClientRegularFilterGroupView>() {
            ClientRegularFilterGroupView newObject(DataInputStream inStream) throws IOException, ClassNotFoundException {
                return new ClientRegularFilterGroupView(inStream,containers);
            }});

        Map<String, ClientPropertyView> sIDtoProperty = new HashMap();
        for (ClientPropertyView property : properties) {
            sIDtoProperty.put(property.getSID(), property);
        }

        for (ClientGroupObjectImplementView groupObject : groupObjects) {
            ClientHighlighter highlighter = groupObject.gridView.highlighter;
            if (highlighter != null) {
                highlighter.init(sIDtoProperty);
            }
        }

        int orderCount = inStream.readInt();
        for(int i=0;i<orderCount;i++) {
            ClientCellView order;
            if(inStream.readBoolean())
                order = getProperty(inStream.readInt());
            else
                order = getObject(inStream.readInt(), false);
            defaultOrders.put(order,inStream.readBoolean());
        }

        printView = new ClientFunctionView(inStream,containers);
        xlsView = new ClientFunctionView(inStream,containers);
        nullView = new ClientFunctionView(inStream,containers);
        refreshView = new ClientFunctionView(inStream,containers);
        applyView = new ClientFunctionView(inStream,containers);
        cancelView = new ClientFunctionView(inStream,containers);
        okView = new ClientFunctionView(inStream,containers);
        closeView = new ClientFunctionView(inStream,containers);

        int cellCount = inStream.readInt();
        for(int i=0;i<cellCount;i++) {
            int cellID = inStream.readInt();
            if(inStream.readBoolean()) // property
                order.add(getProperty(cellID));
            else
                order.add(getObject(cellID, inStream.readBoolean()));
        }

        keyStroke = (KeyStroke) new ObjectInputStream(inStream).readObject();

        caption = inStream.readUTF();
    }
}
