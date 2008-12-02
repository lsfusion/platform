package platformlocal;

import net.sf.jasperreports.engine.design.JasperDesign;

import java.io.*;
import java.util.*;

class Serializer {

    // -------------------------------------- Сериализация данных формы -------------------------------------------- //

    public static void serializeFormChanges(DataOutputStream outStream, FormChanges formChanges) throws IOException {

        //ClassViews
        outStream.writeInt(formChanges.ClassViews.size());
        for (GroupObjectImplement groupObject : formChanges.ClassViews.keySet()) {
            serializeGroupObjectImplement(outStream, groupObject);
            outStream.writeBoolean(formChanges.ClassViews.get(groupObject));
        }

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
                serializeObject(outStream, gridProperties.get(groupObjectValue));
            }
        }
//        System.out.println("GridProperties : " + outStream.size());

        //PanelProperties
        outStream.writeInt(formChanges.PanelProperties.size());
        for (PropertyView propertyView : formChanges.PanelProperties.keySet()) {

            serializePropertyView(outStream, propertyView);
            serializeObject(outStream, formChanges.PanelProperties.get(propertyView));
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

        //ClassViews
        clientFormChanges.ClassViews = new HashMap();
        count = inStream.readInt();
        for (int i = 0; i < count; i++) {

            ClientGroupObjectImplement clientGroupObject = deserializeClientGroupObjectImplement(inStream, clientFormView);

            clientFormChanges.ClassViews.put(clientGroupObject,
                                             inStream.readBoolean());
        }

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
                                   deserializeObject(inStream));
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
                                                  deserializeObject(inStream));
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
        outStream.writeInt(groupObject.ID);
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

    public static void serializeObject(DataOutputStream outStream, Object object) throws IOException {

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

        if (object instanceof Double) {
            outStream.writeByte(3);
            outStream.writeDouble((Double)object);
            return;
        }

        if (object instanceof Long) {
            outStream.writeByte(4);
            outStream.writeLong((Long)object);
            return;
        }

        if (object instanceof Boolean) {
            outStream.writeByte(5);
            outStream.writeBoolean((Boolean)object);
            return;
        }

        throw new IOException();
    }

    public static Object deserializeObject(DataInputStream inStream) throws IOException {

        int objectType = inStream.readByte();

        if (objectType == 0) {
            return null;
        }

        if (objectType == 1) {
            return inStream.readInt();
        }

        if (objectType == 2) {
            return inStream.readUTF();
        }

        if (objectType == 3) {
            return inStream.readDouble();
        }

        if (objectType == 4) {
            return inStream.readLong();
        }

        if (objectType == 5) {
            return inStream.readBoolean();
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

        if (cls == null) { outStream.writeByte(127); return; }

        if (cls instanceof ObjectClass) outStream.writeByte(0);
        if (cls instanceof StringClass) outStream.writeByte(1);
        if (cls instanceof IntegerClass) outStream.writeByte(2);
        if (cls instanceof DateClass) outStream.writeByte(3);
        if (cls instanceof BitClass) outStream.writeByte(4);
        if (cls instanceof DoubleClass) outStream.writeByte(5);
        if (cls instanceof LongClass) outStream.writeByte(6);

        outStream.writeInt(cls.ID);
        outStream.writeUTF(cls.caption);
        outStream.writeBoolean(!cls.Childs.isEmpty());
    }

    public static ClientClass deserializeClientClass(DataInputStream inStream) throws IOException {

        ClientClass cls;

        int clsType = inStream.readByte();

        switch (clsType) {
            case 127 : return null;
            case 0 : cls = new ClientObjectClass(); break;
            case 1 : cls = new ClientStringClass(); break;
            case 2 : cls = new ClientIntegerClass(); break;
            case 3 : cls = new ClientDateClass(); break;
            case 4 : cls = new ClientBitClass(); break;
            case 5 : cls = new ClientDoubleClass(); break;
            case 6 : cls = new ClientLongClass(); break;
            default : throw new IOException();
        }

        cls.ID = inStream.readInt();
        cls.caption = inStream.readUTF();
        cls.hasChilds = inStream.readBoolean();

        return cls;
    }

    public static void serializeObjectValue(DataOutputStream outStream, ObjectValue objectValue) throws IOException {
        serializeClass(outStream, objectValue.Class);
        serializeObject(outStream, objectValue.Object);
    }

    public static ClientObjectValue deserializeClientObjectValue(DataInputStream inStream) throws IOException {

        ClientObjectValue objectValue = new ClientObjectValue();
        objectValue.cls = deserializeClientClass(inStream);
        objectValue.object = deserializeObject(inStream);
        return objectValue;
    }


    public static void serializeChangeValue(DataOutputStream outStream, ChangeValue objectValue) throws IOException {

        serializeClass(outStream, (objectValue == null)? null : objectValue.Class);

        if (objectValue instanceof ChangeObjectValue) {
            outStream.writeByte(0);
            serializeObject(outStream, ((ChangeObjectValue)objectValue).Value);
        }

        if (objectValue instanceof ChangeCoeffValue) {
            outStream.writeByte(1);
            outStream.writeInt(((ChangeCoeffValue)objectValue).Coeff);
        }

    }

    public static ClientChangeValue deserializeClientChangeValue(DataInputStream inStream) throws IOException {


        ClientClass cls = deserializeClientClass(inStream);

        if (cls == null) return null;

        int changeType = inStream.readByte();

        if (changeType == 0) {
            return new ClientChangeObjectValue(cls, deserializeObject(inStream));
        }

        if (changeType == 1) {
            return new ClientChangeCoeffValue(cls, inStream.readInt());
        }

        throw new IOException();
    }

    // -------------------------------------- Сериализация фильтров -------------------------------------------- //
    public static void serializeClientFilter(DataOutputStream outStream, ClientFilter filter) throws IOException {

        outStream.writeInt(filter.property.ID);

        outStream.writeInt(filter.compare);

        if (filter.value instanceof ClientUserValueLink) {

            outStream.writeByte(0);
            serializeObject(outStream, ((ClientUserValueLink)filter.value).value);
        }

        if (filter.value instanceof ClientObjectValueLink) {

            outStream.writeByte(1);
            outStream.writeInt(((ClientObjectValueLink)filter.value).object.ID);
        }

        if (filter.value instanceof ClientPropertyValueLink) {

            outStream.writeByte(2);
            outStream.writeInt(((ClientPropertyValueLink)filter.value).property.ID);
        }

    }

    public static Filter deserializeFilter(DataInputStream inStream, RemoteForm remoteForm) throws IOException {

        PropertyObjectImplement property = remoteForm.getPropertyView(inStream.readInt()).View;

        int compare = inStream.readInt();

        int classValueLink = inStream.readByte();

        ValueLink valueLink = null;

        if (classValueLink == 0) {
            valueLink = new UserValueLink(deserializeObject(inStream));
        }

        if (classValueLink == 1) {
            valueLink = new ObjectValueLink(remoteForm.getObjectImplement(inStream.readInt()));
        }

        if (classValueLink == 2) {
            valueLink = new PropertyValueLink(remoteForm.getPropertyView(inStream.readInt()).View);
        }

        return new Filter(property, compare, valueLink);

    }

    // -------------------------------------- Сериализация навигатора -------------------------------------------- //

    public static void serializeListNavigatorElement(DataOutputStream outStream, List<NavigatorElement> listElements) throws IOException {

        outStream.writeInt(listElements.size());

        for (NavigatorElement element : listElements) {
            if (element instanceof NavigatorForm) {
                outStream.writeByte(0);
                outStream.writeBoolean(((NavigatorForm)element).isPrintForm);
            }
            else
                outStream.writeByte(1);
            outStream.writeInt(element.ID);
            outStream.writeUTF(element.caption);
            outStream.writeBoolean(element.allowChildren());
        }

    }

    public static List<ClientNavigatorElement> deserializeListClientNavigatorElement(DataInputStream inStream) throws IOException {

        List<ClientNavigatorElement> listElements = new ArrayList();

        int count = inStream.readInt();

        for (int i = 0; i < count; i++) {

            int type = inStream.readByte();

            ClientNavigatorElement element;
            if (type == 0) {
                element = new ClientNavigatorForm();
                element.isPrintForm = inStream.readBoolean();
            }
            else
                element = new ClientNavigatorElement();

            element.ID = inStream.readInt();
            element.caption = inStream.readUTF();
            element.hasChilds = inStream.readBoolean();

            listElements.add(element);
        }

        return listElements;
    }

}

class ByteArraySerializer extends Serializer {

    public static byte[] serializeClientFormView(ClientFormView clientFormView) {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            objectStream.writeObject(clientFormView);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();
    }

    public static ClientFormView deserializeClientFormView(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);

        try {
            ObjectInputStream dataStream = new ObjectInputStream(inStream);
            try {
                return (ClientFormView)dataStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] serializeReportDesign(JasperDesign jasperDesign) {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            objectStream.writeObject(jasperDesign);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static JasperDesign deserializeReportDesign(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);

        try {
            ObjectInputStream dataStream = new ObjectInputStream(inStream);
            try {
                return (JasperDesign)dataStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] serializeReportData(ReportData reportData) {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            objectStream.writeObject(reportData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static ReportData deserializeReportData(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);

        try {
            ObjectInputStream dataStream = new ObjectInputStream(inStream);
            try {
                return (ReportData)dataStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // -------------------------------------- Сериализация данных формы -------------------------------------------- //

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

    public static byte[] serializeObject(Object value) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeObject(dataStream, value);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static Object deserializeObject(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeObject(dataStream);
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

    public static byte[] serializeObjectValue(ObjectValue objectValue) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeObjectValue(dataStream, objectValue);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static ClientObjectValue deserializeClientObjectValue(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeClientObjectValue(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static byte[] serializeChangeValue(ChangeValue changeValue) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeChangeValue(dataStream, changeValue);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static ClientChangeValue deserializeClientChangeValue(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeClientChangeValue(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    // -------------------------------------- Сериализация фильтров -------------------------------------------- //
    public static byte[] serializeClientFilter(ClientFilter filter) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeClientFilter(dataStream, filter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static Filter deserializeFilter(byte[] state, RemoteForm remoteForm) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeFilter(dataStream, remoteForm);
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