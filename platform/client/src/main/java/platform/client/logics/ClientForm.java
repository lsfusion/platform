package platform.client.logics;

import platform.base.identity.IdentityObject;
import platform.base.OrderedMap;
import platform.client.SwingUtils;
import platform.base.context.ApplicationContext;
import platform.base.context.ApplicationContextHolder;
import platform.client.form.LogicsSupplier;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.gwt.view.GForm;
import platform.interop.form.layout.AbstractForm;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientForm extends IdentityObject implements LogicsSupplier, ClientIdentitySerializable,
                                                          AbstractForm<ClientContainer, ClientComponent, ClientFunction>,
                                                          ApplicationContextHolder {

    public KeyStroke keyStroke = null;

    public String caption = "";

    public static ClientGroupObject lastActiveGroupObject;

    // пока используется только для сериализации туда-назад
    public Integer overridePageWidth;

    public ClientContainer mainContainer;

    public List<ClientTreeGroup> treeGroups = new ArrayList<ClientTreeGroup>();
    public List<ClientGroupObject> groupObjects = new ArrayList<ClientGroupObject>();
    public List<ClientPropertyDraw> propertyDraws = new ArrayList<ClientPropertyDraw>();

    public OrderedMap<ClientPropertyDraw, Boolean> defaultOrders = new OrderedMap<ClientPropertyDraw, Boolean>();
    public List<ClientRegularFilterGroup> regularFilterGroups = new ArrayList<ClientRegularFilterGroup>();
    public Map<String, String> blockedScreen = new HashMap<String, String>();

    private ClientFunction printFunction;
    private ClientFunction editFunction;
    private ClientFunction xlsFunction;
    private ClientFunction nullFunction;
    private ClientFunction refreshFunction;
    private ClientFunction applyFunction;
    private ClientFunction cancelFunction;
    private ClientFunction okFunction;
    private ClientFunction closeFunction;

    public ClientContainer getMainContainer() {
        return mainContainer;
    }

    public ClientFunction getPrintFunction() {
        return printFunction;
    }

    public ClientFunction getEditFunction() {
        return editFunction;
    }

    public ClientFunction getXlsFunction() {
        return xlsFunction;
    }

    public ClientFunction getNullFunction() {
        return nullFunction;
    }

    public ClientFunction getRefreshFunction() {
        return refreshFunction;
    }

    public ClientFunction getApplyFunction() {
        return applyFunction;
    }

    public ClientFunction getCancelFunction() {
        return cancelFunction;
    }

    public ClientFunction getOkFunction() {
        return okFunction;
    }

    public ClientFunction getCloseFunction() {
        return closeFunction;
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

        printFunction = new ClientFunction(getContext());
        editFunction = new ClientFunction(getContext());
        xlsFunction = new ClientFunction(getContext());
        nullFunction = new ClientFunction(getContext());
        refreshFunction = new ClientFunction(getContext());
        applyFunction = new ClientFunction(getContext());
        cancelFunction = new ClientFunction(getContext());
        okFunction = new ClientFunction(getContext());
        closeFunction = new ClientFunction(getContext());
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

        pool.serializeObject(outStream, printFunction);
        pool.serializeObject(outStream, editFunction);
        pool.serializeObject(outStream, xlsFunction);
        pool.serializeObject(outStream, nullFunction);
        pool.serializeObject(outStream, refreshFunction);
        pool.serializeObject(outStream, applyFunction);
        pool.serializeObject(outStream, cancelFunction);
        pool.serializeObject(outStream, okFunction);
        pool.serializeObject(outStream, closeFunction);

        pool.writeObject(outStream, keyStroke);
        pool.writeString(outStream, caption);
        pool.writeInt(outStream, overridePageWidth);
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

        printFunction = pool.deserializeObject(inStream);
        editFunction = pool.deserializeObject(inStream);
        xlsFunction = pool.deserializeObject(inStream);
        nullFunction = pool.deserializeObject(inStream);
        refreshFunction = pool.deserializeObject(inStream);
        applyFunction = pool.deserializeObject(inStream);
        cancelFunction = pool.deserializeObject(inStream);
        okFunction = pool.deserializeObject(inStream);
        closeFunction = pool.deserializeObject(inStream);

        keyStroke = pool.readObject(inStream);
        caption = pool.readString(inStream);
        overridePageWidth = pool.readInt(inStream);
        blockedScreen = pool.readObject(inStream);
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
        //todo: what about properties

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

    private GForm gwtForm;
    public GForm getGwtForm() {
        if (gwtForm == null) {
            gwtForm = new GForm();
            gwtForm.caption = caption;
            gwtForm.mainContainer = mainContainer.getGwtComponent();

            for (ClientGroupObject group : groupObjects) {
                gwtForm.groupObjects.add(group.getGwtGroupObject());
            }

            for (ClientPropertyDraw property : propertyDraws) {
                gwtForm.propertyDraws.add(property.getGwtPropertyDraw());
            }

            for (ClientRegularFilterGroup filterGroup : regularFilterGroups) {
                gwtForm.regularFilterGroups.add(filterGroup.getGwtRegularFilterGroup());
            }
        }

        return gwtForm;
    }
}
