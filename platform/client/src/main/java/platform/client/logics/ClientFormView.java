package platform.client.logics;

import platform.client.form.LogicsSupplier;
import platform.base.OrderedMap;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class ClientFormView implements Serializable, LogicsSupplier {

    public final List<ClientContainerView> containers;

    public List<ClientGroupObjectImplementView> groupObjects;
    public List<ClientPropertyView> properties;

    public final OrderedMap<ClientCellView,Boolean> defaultOrders = new OrderedMap<ClientCellView, Boolean>();
    public List<ClientRegularFilterGroupView> regularFilters;

    public ClientFunctionView printView;
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

    public ClientPropertyView getProperty(int id) {
        for (ClientPropertyView property : properties)
            if (property.getID() == id) return property;
        return null;
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
    }
}
