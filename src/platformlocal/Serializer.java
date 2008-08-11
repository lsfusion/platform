package platformlocal;

import java.io.*;
import java.util.*;

class Serializer {

    // -------------------------------------- Сериализация самой формы -------------------------------------------- //

    public static void serializeFormChanges(DataOutputStream outStream, FormChanges formChanges) throws IOException {

        //Objects
        outStream.writeInt(formChanges.Objects.size());
        for (GroupObjectImplement groupObject : formChanges.Objects.keySet()) {

            serializeGroupObjectImplement(outStream, groupObject);
            serializeGroupObjectValue(outStream, groupObject, formChanges.Objects.get(groupObject));
        }
//        System.out.println("Objects : " + outStream.size());

        //GridObjects
        outStream.writeInt(formChanges.GridObjects.size());
        for (GroupObjectImplement groupObject : formChanges.GridObjects.keySet()) {

            serializeGroupObjectImplement(outStream, groupObject);

            List<GroupObjectValue> gridObjects = formChanges.GridObjects.get(groupObject);

            outStream.writeInt(gridObjects.size());
            for (GroupObjectValue groupObjectValue : gridObjects)
                serializeGroupObjectValue(outStream, groupObject, groupObjectValue);
        }
//        System.out.println("GridObjects : " + outStream.size());

        //GridProperties
        outStream.writeInt(formChanges.GridProperties.size());
        for (PropertyView propertyView : formChanges.GridProperties.keySet()) {

            serializePropertyView(outStream, propertyView);

            Map<GroupObjectValue, Object> gridProperties = formChanges.GridProperties.get(propertyView);

            outStream.writeInt(gridProperties.size());
            for (GroupObjectValue groupObjectValue : gridProperties.keySet()) {
                serializeGroupObjectValue(outStream, propertyView.ToDraw, groupObjectValue);
                serializeObjectValue(outStream, gridProperties.get(groupObjectValue));
            }
        }
//        System.out.println("GridProperties : " + outStream.size());

        //PanelProperties
        outStream.writeInt(formChanges.PanelProperties.size());
        for (PropertyView propertyView : formChanges.PanelProperties.keySet()) {

            serializePropertyView(outStream, propertyView);
            serializeObjectValue(outStream, formChanges.PanelProperties.get(propertyView));
        }
//        System.out.println("PanelProperties : " + outStream.size());

        //DropProperties
        outStream.writeInt(formChanges.DropProperties.size());
        for (PropertyView propertyView : formChanges.DropProperties) {
            serializePropertyView(outStream, propertyView);
        }
//        System.out.println("DropProperties : " + outStream.size());

//        System.out.println(outStream.size());

    }

    public static ClientFormChanges deserializeClientFormChanges(DataInputStream inStream, ClientFormView clientFormView) throws IOException {

        ClientFormChanges clientFormChanges = new ClientFormChanges();
        int count;

        int all = inStream.available();
//        System.out.println("Available : " + all);

        //Objects
        clientFormChanges.Objects = new HashMap();

        count = inStream.readInt();
        for (int i = 0; i < count; i++) {

            ClientGroupObjectImplement clientGroupObject = deserializeClientGroupObjectImplement(inStream, clientFormView);

            clientFormChanges.Objects.put(clientGroupObject,
                                          deserializeClientGroupObjectValue(inStream, clientGroupObject));
        }
//        System.out.println("Objects read : " + (all-inStream.available()));

        //GridObjects
        clientFormChanges.GridObjects = new HashMap();

        count = inStream.readInt();
        for (int i = 0; i < count; i++) {

            ClientGroupObjectImplement clientGroupObject = deserializeClientGroupObjectImplement(inStream, clientFormView);

            List<ClientGroupObjectValue> clientGridObjects = new ArrayList();

            int listCount = inStream.readInt();
            for (int j = 0; j < listCount; j++) {
                clientGridObjects.add(deserializeClientGroupObjectValue(inStream, clientGroupObject));
            }

            clientFormChanges.GridObjects.put(clientGroupObject, clientGridObjects);
        }
//        System.out.println("GridObjects read : " + (all-inStream.available()));

        //GridProperties
        clientFormChanges.GridProperties = new HashMap();

        count = inStream.readInt();
        for (int i = 0; i < count; i++) {

            ClientPropertyView clientPropertyView = deserializeClientPropertyView(inStream, clientFormView);
            ClientGroupObjectImplement clientGroupObject = clientPropertyView.groupObject;

            Map<ClientGroupObjectValue, Object> gridProperties = new HashMap();

            int mapCount = inStream.readInt();
            for (int j = 0; j < mapCount; j++) {
                gridProperties.put(deserializeClientGroupObjectValue(inStream, clientGroupObject),
                                   deserializeObjectValue(inStream));
            }

            clientFormChanges.GridProperties.put(clientPropertyView, gridProperties);
        }
//        System.out.println("GridProperties read : " + (all-inStream.available()));

        //PanelProperties
        clientFormChanges.PanelProperties = new HashMap();

        count = inStream.readInt();
        for (int i = 0; i < count; i++) {

            ClientPropertyView clientPropertyView = deserializeClientPropertyView(inStream, clientFormView);
            clientFormChanges.PanelProperties.put(clientPropertyView,
                                                  deserializeObjectValue(inStream));
        }
//        System.out.println("PanelProperties read : " + (all-inStream.available()));

        //DropProperties
        clientFormChanges.DropProperties = new HashSet();

        count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            clientFormChanges.DropProperties.add(deserializeClientPropertyView(inStream, clientFormView));
        }
//        System.out.println("DropProperties read : " + (all-inStream.available()));

        return clientFormChanges;

    }

    public static void serializeGroupObjectImplement(DataOutputStream outStream, GroupObjectImplement groupObject) throws IOException {
        outStream.writeInt(groupObject.GID);
    }

    private static ClientGroupObjectImplement deserializeClientGroupObjectImplement(DataInputStream inStream, ClientFormView clientFormView) throws IOException {
        return clientFormView.getGroupObject(inStream.readInt());
    }

    public static void serializeGroupObjectValue(DataOutputStream outStream, GroupObjectImplement groupObject, GroupObjectValue groupObjectValue) throws IOException {

        for (ObjectImplement object : groupObject) {
            outStream.writeInt(groupObjectValue.get(object));
        }

    }

    public static GroupObjectValue deserializeGroupObjectValue(DataInputStream inStream, GroupObjectImplement groupObject) throws IOException {

        GroupObjectValue value = new GroupObjectValue();

        for (ObjectImplement object : groupObject) {
            value.put(object, inStream.readInt());
        }

        return value;

    }

    public static void serializeClientGroupObjectValue(DataOutputStream outStream, ClientGroupObjectValue clientGroupObjectValue) throws IOException {

        for (ClientObjectImplement object : clientGroupObjectValue.keySet()) {
            outStream.writeInt(clientGroupObjectValue.get(object));
        }

    }

    private static ClientGroupObjectValue deserializeClientGroupObjectValue(DataInputStream inStream, ClientGroupObjectImplement clientGroupObject) throws IOException {

        ClientGroupObjectValue clientValue = new ClientGroupObjectValue();

        for (ClientObjectImplement clientObject : clientGroupObject) {
            clientValue.put(clientObject, inStream.readInt());
        }

        return clientValue;
    }

    public static void serializePropertyView(DataOutputStream outStream, PropertyView propertyView) throws IOException {
        outStream.writeInt(propertyView.ID);
    }

    private static ClientPropertyView deserializeClientPropertyView(DataInputStream inStream, ClientFormView clientFormView) throws IOException {
        return clientFormView.getPropertyView(inStream.readInt());
    }

    public static void serializeObjectValue(DataOutputStream outStream, Object object) throws IOException {

        if (object == null) {
            outStream.writeByte(0);
            return;
        }

        if (object instanceof Integer) {
            outStream.writeByte(1);
            outStream.writeInt((Integer)object);
            return;
        }

        if (object instanceof String) {
            outStream.writeByte(2);
            outStream.writeUTF(((String)object).trim());
            return;
        }

        throw new IOException();
    }

    public static Object deserializeObjectValue(DataInputStream inStream) throws IOException {

        int objectType = inStream.readByte();

        if (objectType == 0) {
            return null;
        }

        if (objectType == 1) {

            return (Integer)inStream.readInt();
        }

        if (objectType == 2) {
            return inStream.readUTF();
        }

        throw new IOException();
    }

    // -------------------------------------- Сериализация классов -------------------------------------------- //
    public static void serializeListClass(DataOutputStream outStream, List<Class> classes) throws IOException {

        outStream.writeInt(classes.size());
        for (Class cls : classes) {
            serializeClass(outStream, cls);
        }

    }

    public static List<ClientClass> deserializeListClientClass(DataInputStream inStream) throws IOException {

        List<ClientClass> classes = new ArrayList();

        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientClass cls = deserializeClientClass(inStream);
            classes.add(cls);
        }

        return classes;
    }

    public static void serializeClass(DataOutputStream outStream, Class cls) throws IOException {
        outStream.writeInt(cls.ID);
        outStream.writeUTF(cls.caption);
        outStream.writeBoolean(!cls.Childs.isEmpty());
    }

    public static ClientClass deserializeClientClass(DataInputStream inStream) throws IOException {

        ClientClass cls = new ClientClass();

        cls.ID = inStream.readInt();
        cls.caption = inStream.readUTF();
        cls.hasChilds = inStream.readBoolean();

        return cls;
    }


    // -------------------------------------- Сериализация навигатора -------------------------------------------- //

    public static void serializeListNavigatorElement(DataOutputStream outStream, List<NavigatorElement> listElements) throws IOException {

        outStream.writeInt(listElements.size());

        for (NavigatorElement element : listElements) {
            if (element instanceof NavigatorGroup)
                outStream.writeByte(0);
            else
                outStream.writeByte(1);
            outStream.writeInt(element.ID);
            outStream.writeUTF(element.caption);
        }

    }

    public static List<ClientNavigatorElement> deserializeListClientNavigatorElement(DataInputStream inStream) throws IOException {

        List<ClientNavigatorElement> listElements = new ArrayList();

        int count = inStream.readInt();

        for (int i = 0; i < count; i++) {

            int type = inStream.readByte();

            ClientNavigatorElement element;
            if (type == 0)
                element = new ClientNavigatorGroup();
            else
                element = new ClientNavigatorForm();

            element.ID = inStream.readInt();
            element.caption = inStream.readUTF();

            listElements.add(element);
        }

        return listElements;
    }

}

class ByteArraySerializer extends Serializer {

    // -------------------------------------- Сериализация самой формы -------------------------------------------- //

    public static byte[] serializeFormChanges(FormChanges formChanges) {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeFormChanges(dataStream, formChanges);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static ClientFormChanges deserializeClientFormChanges(byte[] state, ClientFormView clientFormView) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return Serializer.deserializeClientFormChanges(dataStream, clientFormView);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static GroupObjectValue deserializeGroupObjectValue(byte[] state, GroupObjectImplement groupObject) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeGroupObjectValue(dataStream, groupObject);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static byte[] serializeClientGroupObjectValue(ClientGroupObjectValue clientGroupObjectValue) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeClientGroupObjectValue(dataStream, clientGroupObjectValue);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static byte[] serializeObjectValue(Object value) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeObjectValue(dataStream, value);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static Object deserializeObjectValue(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeObjectValue(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }
    
    // -------------------------------------- Сериализация классов -------------------------------------------- //
    public static byte[] serializeListClass(List<Class> classes) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeListClass(dataStream, classes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static List<ClientClass> deserializeListClientClass(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeListClientClass(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static byte[] serializeClass(Class cls) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeClass(dataStream, cls);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static ClientClass deserializeClientClass(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeClientClass(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    // -------------------------------------- Сериализация навигатора -------------------------------------------- //

    public static byte[] serializeListNavigatorElement(List<NavigatorElement> listElements) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeListNavigatorElement(dataStream, listElements);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static List<ClientNavigatorElement> deserializeListClientNavigatorElement(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeListClientNavigatorElement(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

}