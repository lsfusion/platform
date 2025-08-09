package lsfusion.server.logics.form.interactive.changed;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.base.ResourceUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.file.*;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.converters.KeyStrokeConverter;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.file.*;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.ContainerViewExtraType;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.design.ContainerViewInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.*;

// появляется по сути для отделения клиента, именно он возвращается назад клиенту
public class FormChanges {

    // current (panel) objects
    private final ImMap<GroupObjectInstance, ImMap<ObjectInstance, ? extends ObjectValue>> objects;

    // list (grid) objects
    private final ImMap<GroupObjectInstance, ImOrderSet<ImMap<ObjectInstance, DataObject>>> gridObjects;

    // tree objects
    private final ImMap<GroupObjectInstance, ImList<ImMap<ObjectInstance, DataObject>>> parentObjects;
    // tree object has + 
    private final ImMap<GroupObjectInstance, ImMap<ImMap<ObjectInstance, DataObject>, Long>> expandables;

    // properties
    private final ImMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties;
    // property has to be hidden
    private final ImSet<PropertyDrawInstance> dropProperties;

    private final ImList<ComponentView> activateTabs;
    private final ImList<PropertyDrawInstance> activateProps;
    
    private final ImList<ContainerView> collapseContainers;
    private final ImList<ContainerView> expandContainers;

    // current (panel) objects
    private final ImMap<GroupObjectInstance, Boolean> updateStateObjects;

    private final boolean needConfirm;

    public static FormChanges EMPTY = new MFormChanges().immutable();

    public FormChanges(ImMap<GroupObjectInstance, ImMap<ObjectInstance, ? extends ObjectValue>> objects,
                       ImMap<GroupObjectInstance, ImOrderSet<ImMap<ObjectInstance, DataObject>>> gridObjects,
                       ImMap<GroupObjectInstance, ImList<ImMap<ObjectInstance, DataObject>>> parentObjects,
                       ImMap<GroupObjectInstance, ImMap<ImMap<ObjectInstance, DataObject>, Long>> expandables,
                       ImMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties,
                       ImSet<PropertyDrawInstance> dropProperties,
                       ImMap<GroupObjectInstance, Boolean> updateStateObjects, ImList<ComponentView> activateTabs, 
                       ImList<PropertyDrawInstance> activateProps, ImList<ContainerView> collapseContainers, 
                       ImList<ContainerView> expandContainers, boolean needConfirm) {
        this.objects = objects;
        this.gridObjects = gridObjects;
        this.parentObjects = parentObjects;
        this.expandables = expandables;
        this.properties = properties;
        this.dropProperties = dropProperties;
        this.updateStateObjects = updateStateObjects;
        this.activateTabs = activateTabs;
        this.activateProps = activateProps;
        this.collapseContainers = collapseContainers;
        this.expandContainers = expandContainers;
        this.needConfirm = needConfirm;
    }

    void out(FormInstance bv) {
        System.out.println(" ------- GROUPOBJECTS ---------------");
        for (GroupObjectInstance group : bv.getGroups()) {
            ImList<ImMap<ObjectInstance, DataObject>> groupGridObjects = gridObjects.get(group);
            if (groupGridObjects != null) {
                System.out.println(group.getID() + " - GRID Changes");
                for (ImMap<ObjectInstance, DataObject> value : groupGridObjects)
                    System.out.println(value);
            }

            ImMap<ObjectInstance, ? extends ObjectValue> value = objects.get(group);
            if (value != null)
                System.out.println(group.getID() + " - Object Changes " + value);
        }

        System.out.println(" ------- PROPERTIES ---------------");
        System.out.println(" ------- Group ---------------");
        for (PropertyReaderInstance property : properties.keyIt()) {
            ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue> propertyValues = properties.get(property);
            System.out.println(property + " ---- property");
            for (ImMap<ObjectInstance, DataObject> gov : propertyValues.keyIt())
                System.out.println(gov + " - " + propertyValues.get(gov));
        }

        System.out.println(" ------- Drop ---------------");
        for (PropertyDrawInstance property : dropProperties)
            System.out.println(property);

        System.out.println(" ------- Activate tab ---------------");
        for (ComponentView tab : activateTabs)
            System.out.println(tab);

        System.out.println(" ------- Activate property ---------------");
        for (PropertyDrawInstance prop : activateProps)
            System.out.println(prop);

        System.out.println(" ------- Collapse containers ---------------");
        for (ContainerView container : collapseContainers)
            System.out.println(container);

        System.out.println(" ------- Expand containers ---------------");
        for (ContainerView container : expandContainers)
            System.out.println(container);
    }

    public void serialize(DataOutputStream outStream, FormInstanceContext context) throws IOException {

        outStream.writeInt(objects.size());
        for (int i=0,size=objects.size();i<size;i++) {
            outStream.writeInt(objects.getKey(i).getID());
            serializeGroupObjectValue(outStream, objects.getValue(i));
        }

        serializeKeyObjectsMap(outStream, gridObjects);
        serializeKeyObjectsMap(outStream, parentObjects);

        outStream.writeInt(expandables.size());
        for (int i = 0; i < expandables.size(); ++i) {
            outStream.writeInt(expandables.getKey(i).getID());

            ImMap<ImMap<ObjectInstance, DataObject>, Long> groupExpandables = expandables.getValue(i);
            outStream.writeInt(groupExpandables.size());
            for (int j = 0; j < groupExpandables.size(); ++j) {
                serializeGroupObjectValue(outStream, groupExpandables.getKey(j));
                outStream.writeInt(groupExpandables.getValue(j).intValue());
            }
        }

        outStream.writeInt(properties.size());
        for (int i=0,size=properties.size();i<size;i++) {
            PropertyReaderInstance propertyReadInstance = properties.getKey(i);
            ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue> rows = properties.getValue(i);

            // сериализация PropertyReadInterface
            outStream.writeByte(propertyReadInstance.getTypeID());
            outStream.writeInt(propertyReadInstance.getID());
            if(propertyReadInstance instanceof PropertyDrawInstance.LastReaderInstance)
                outStream.writeInt(((PropertyDrawInstance.LastReaderInstance) propertyReadInstance).index);

            ConvertData convertData = getConvertData(propertyReadInstance, context);

            outStream.writeInt(rows.size());
            for (int j=0,sizeJ=rows.size();j<sizeJ;j++) {
                ImMap<ObjectInstance, DataObject> objectValues = rows.getKey(j);

                serializeGroupObjectValue(outStream, objectValues);

                Object value = rows.getValue(j).getValue();

                serializeConvertFileValue(outStream, convertData, value, context);
            }
        }

        outStream.writeInt(dropProperties.size());
        for (PropertyDrawInstance propertyView : dropProperties) {
            outStream.writeInt(propertyView.getID());
        }

        outStream.writeInt(updateStateObjects.size());
        for (int i=0,size=updateStateObjects.size();i<size;i++) {
            outStream.writeInt(updateStateObjects.getKey(i).getID());
            outStream.writeBoolean(updateStateObjects.getValue(i));
        }

        outStream.writeInt(activateTabs.size());
        for (ComponentView activateTab : activateTabs) {
            outStream.writeInt(activateTab.getID());
        }

        outStream.writeInt(activateProps.size());
        for (PropertyDrawInstance propertyView : activateProps) {
            outStream.writeInt(propertyView.getID());
        }
        
        outStream.writeInt(collapseContainers.size());
        for (ContainerView container : collapseContainers) {
            outStream.writeInt(container.getID());
        }
        
        outStream.writeInt(expandContainers.size());
        for (ContainerView container : expandContainers) {
            outStream.writeInt(container.getID());
        }

        outStream.writeBoolean(needConfirm);
    }

    public static class ConvertData {
        public final Type type;

        public ConvertData(Type type) {
            this.type = type;
        }
    }
    public static class NeedImage extends ConvertData {
        public final Function<String, AppServerImage.Reader> imageSupplier;

        public NeedImage(Type type, Function<String, AppServerImage.Reader> imageSupplier) {
            super(type);
            this.imageSupplier = imageSupplier;
        }
    }
    public static class NeedFile extends ConvertData {
        public NeedFile(Type type) {
            super(type);
        }
    }
    public static class NeedInputEvent extends ConvertData {
        private boolean mouse;
        public NeedInputEvent(Type type, boolean mouse) {
            super(type);
            this.mouse = mouse;
        }
    }

    public static byte[] serializeConvertFileValue(Object value, ExecutionContext context) throws IOException {
        return serializeConvertFileValue(value, context.getRemoteContext());
    }
    public static byte[] serializeConvertFileValue(Object value, ConnectionContext context) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        serializeConvertFileValue(value, new DataOutputStream(outStream), context);
        return outStream.toByteArray();
    }
    public static void serializeConvertFileValue(Object value, DataOutputStream outStream, ConnectionContext context) throws IOException {
        serializeConvertFileValue(outStream, null, value, context);
    }
    public static void serializeConvertFileValue(DataOutputStream outStream, ConvertData convertData, Object value, ConnectionContext context) throws IOException {
        serializeObject(outStream, convertFileValue(convertData, value, context));
    }
    public static Object convertFileValue(ConvertData convertData, Object value, ConnectionContext context) throws IOException {
        if(value instanceof FileData && convertData != null && ((FileData)value).getExtension().equals("resourceImage"))
            value = ((FileData) value).getRawFile().convertString();

        if(value instanceof NamedFileData || value instanceof FileData || value instanceof RawFileData) {
            if(convertData != null) {
                value = convertRawFileData(convertData.type, (Serializable) value, convertData instanceof NeedImage);
                if(value instanceof FileData) // here is small problem that NamedFileData won't be converted, but we'll ignore that for now
                    value = convertFileData(context, (FileData) value);
                return value;
            }

            return true;
        }

        if(value instanceof String && convertData instanceof NeedImage) {
            return AppServerImage.getAppImage(((NeedImage) convertData).imageSupplier.apply((String)value).get(context));
        }

        if(value instanceof String && convertData instanceof NeedInputEvent) {
            return KeyStrokeConverter.parseInputBindingEvent((String) value, ((NeedInputEvent) convertData).mouse);
        }

        if(value instanceof String)
            return convertString(context, (String) value);

        return value;
    }

    private static Serializable convertRawFileData(Type type, Serializable value, boolean needImage) {
        if(value instanceof RawFileData && type instanceof StaticFormatFileClass)
            value = ((StaticFormatFileClass) type).getFileData((RawFileData) value);
        if(needImage)
            return new AppFileDataImage(value);
        return value;
    }

    public static Object convertFileValue(String value, ConnectionContext context) {
        try {
            return convertString(context, value);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static Object convertFileValue(FileData fileData, ConnectionContext context) {
        try {
            return convertFileData(context, fileData);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static Object convertFileValue(Object value, ConnectionContext context) {
        assert value instanceof String || value instanceof FileData; // assert that it is the result of formatHTTP

        if(value instanceof FileData)
            return convertFileValue((FileData)value, context);
        if(value instanceof String)
            return convertFileValue((String) value, context);

        return value;
    }

    private static Serializable convertFileData(ConnectionContext context, FileData fileData) throws IOException {
        String extension = fileData.getExtension();
        // somewhy json uses mime type application/json and not considered to be text
        if(!HumanReadableFileClass.is(extension) && !ExternalUtils.isTextExtension(extension))
            return fileData;

        RawFileData rawFile = fileData.getRawFile();
        if(!BaseUtils.containsInBytes(rawFile.getBytes(), inlineFileSeparator)) // optimization
            return fileData;

        return new FileStringWithFiles((StringWithFiles) convertString(context, rawFile.convertString()), extension);
    }

    @NotNull
    private static Serializable convertString(ConnectionContext context, String string) throws IOException {
        if (!string.contains(inlineFileSeparator)) // optimization
            return string;

        String[] parts = string.split(inlineFileSeparator, -1);
        int length = parts.length / 2;
        String[] prefixes = new String[length + 1];
        Serializable[] files = new Serializable[length];
        boolean[] removeRaw = null;
        for (int k = 0; k < length + 1; k++) {
            prefixes[k] = parts[k * 2];
            if (k * 2 + 1 < parts.length) {
                String name = parts[k * 2 + 1];
                if (name.startsWith(inlineSerializedImageSeparator)) {
                    files[k] = IOUtils.deserializeAppImage(name.substring(inlineSerializedImageSeparator.length()));
                } else if (name.startsWith(inlineImageSeparator)) {
                    files[k] = AppServerImage.getAppImage(AppServerImage.createActionImage(name.substring(inlineImageSeparator.length())).get(context));
                } else if (name.startsWith(inlineDataFileSeparator)) {
                    int separatorLength = inlineDataFileSeparator.length();
                    int endFileType = name.indexOf(inlineDataFileSeparator, separatorLength);
                    boolean needImage = name.charAt(separatorLength) == '1';
                    FileClass file = FileClass.deserializeString(name.substring(separatorLength + 1, endFileType));
                    files[k] = convertRawFileData(file, (Serializable) file.parseCast(name.substring(endFileType + separatorLength)), needImage);

                    if(removeRaw == null)
                        removeRaw = new boolean[parts.length];
                    removeRaw[k * 2 + 1] = true;
                } else { // resource file
                    Result<String> fullPath = new Result<>();
                    files[k] = new StringWithFiles.Resource(ResourceUtils.findResourceAsFileData(name, false, true, fullPath, null), fullPath.result);
                }
            }
        }

        return new StringWithFiles(prefixes, files, removeRaw != null ? getRawString(parts, removeRaw) : string);
    }

    // we need to remove files from strings, because otherwise writeUTF will fail
    private static String getRawString(String[] parts, boolean[] removeRaw) {
        StringBuilder rawString = new StringBuilder();
        for(int i = 0; i < parts.length; i++) {
            if(i > 0)
                rawString.append(inlineFileSeparator);
            if(!removeRaw[i])
                rawString.append(parts[i]);
        }
        return rawString.toString();
    }

    private static ConvertData getConvertData(PropertyReaderInstance reader, FormInstanceContext context) {
        Supplier<Type> readType = () -> reader.getReaderProperty().getType();
        Type readerType;
        if (reader instanceof PropertyDrawInstance && ((PropertyDrawInstance<?>) reader).isProperty(context)) {
            PropertyDrawEntity<?> propertyDraw = ((PropertyDrawInstance<?>) reader).entity;
            readerType = readType.get();
            if (readerType instanceof RenderedClass || (propertyDraw.isPredefinedImage() && propertyDraw.needImage(context)))
                return getNeedImage(readerType, propertyDraw, context);
            else if (readerType instanceof FileClass && propertyDraw.needFile(context)) // if there is a custom function, we want file to be send to web-server as a link
                return new NeedFile(readerType);
        } else if (reader instanceof PropertyDrawInstance.ExtraReaderInstance && reader.getTypeID() == PropertyDrawExtraType.IMAGE.getPropertyReadType()) {
            return getNeedImage(readType.get(), ((PropertyDrawInstance<?>.ExtraReaderInstance) reader).getPropertyDraw().entity, context);
        } else if (reader instanceof ContainerViewInstance.ExtraReaderInstance && reader.getTypeID() == ContainerViewExtraType.IMAGE.getContainerReadType()) {
            ContainerView containerView = ((ContainerViewInstance.ExtraReaderInstance) reader).getContainerView();
            return new NeedImage(readType.get(), imagePath -> AppServerImage.createContainerImage(imagePath, containerView, context.view));
        } else if (reader instanceof PropertyDrawInstance.ExtraReaderInstance && (reader.getTypeID() == PropertyDrawExtraType.CHANGEKEY.getPropertyReadType())) {
            return getNeedInputEvent(readType.get(), false);
        } else if (reader instanceof PropertyDrawInstance.ExtraReaderInstance && (reader.getTypeID() == PropertyDrawExtraType.CHANGEMOUSE.getPropertyReadType())) {
            return getNeedInputEvent(readType.get(), true);
        }
        return null;
    }

    private static NeedImage getNeedImage(Type type, PropertyDrawEntity<?> propertyDraw, FormInstanceContext context) {
        return new NeedImage(type, imagePath -> AppServerImage.createPropertyImage(imagePath, context.view.get(propertyDraw)));
    }

    private static NeedInputEvent getNeedInputEvent(Type type, boolean mouse) {
        return new NeedInputEvent(type, mouse);
    }

    public static void serializeGroupObjectValue(DataOutputStream outStream, ImMap<ObjectInstance,? extends ObjectValue> values) throws IOException {
        outStream.writeInt(values.size());
        for (int i=0,size=values.size();i<size;i++) {
            outStream.writeInt(values.getKey(i).getID());
            serializeObjectValue(outStream, values.getValue(i));
        }
    }

    // should match ClientGroupObjectValue.deserializeObjectValue
    public static void serializeObjectValue(DataOutputStream outStream, ObjectValue value) throws IOException {
        ConcreteClass concreteClass;
        if(value instanceof DataObject && (concreteClass = ((DataObject) value).objectClass) instanceof ConcreteObjectClass) {
            outStream.writeByte(87);
            DataObject objectValue = (DataObject) value;
            outStream.writeLong((Long) objectValue.getValue());
            Long idClass = concreteClass instanceof ConcreteCustomClass ? ((ConcreteCustomClass) concreteClass).ID : null;
            outStream.writeBoolean(idClass != null);
            if(idClass != null)
                outStream.writeLong(idClass);
            return;
        }

        serializeObject(outStream, value.getValue());
    }

    // should match ClientGroupObjectValue.serializeObjectValue
    public static ObjectValue deserializeObjectValue(DataInputStream inStream, ValueClass valueClass) throws IOException {
        byte type = inStream.readByte();
        if(type == 87) {
            long id = inStream.readLong();
            Long idClass = null;
            if(inStream.readBoolean())
                idClass = inStream.readLong();
            return new DataObject(id, ((CustomClass)valueClass).getBaseClass().findConcreteClassID(idClass));
        }

        Serializable value = (Serializable) BaseUtils.deserializeObject(inStream, type);
        if(valueClass instanceof CustomClass) {
            assert value == null;
            return NullValue.instance;
        }

        return ObjectValue.getValue(value, (DataClass)valueClass);
    }

    private void serializeKeyObjectsMap(DataOutputStream outStream, ImMap<GroupObjectInstance, ? extends ImList<ImMap<ObjectInstance, DataObject>>> keyObjects) throws IOException {
        outStream.writeInt(keyObjects.size());
        for (int i=0,size=keyObjects.size();i<size;i++) {

            outStream.writeInt(keyObjects.getKey(i).getID());

            ImList<ImMap<ObjectInstance, DataObject>> rows = keyObjects.getValue(i);
            outStream.writeInt(rows.size());
            for (ImMap<ObjectInstance, DataObject> groupObjectValue : rows) {
                serializeGroupObjectValue(outStream, groupObjectValue);
            }
        }
    }

    public byte[] serialize(FormInstanceContext context) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            serialize(new DataOutputStream(outStream), context);
            return outStream.toByteArray();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    // assert that it was filtered by filterPropertiesExternal
    private void serializePropertiesExternal(FormInstanceContext context, JSONObject jsonObject, ImSet<PropertyDrawInstance> serializeProps, ImMap<ObjectInstance, DataObject> gridObjectRow) {
        for(PropertyDrawInstance<?> property : serializeProps)
            jsonObject.put(property.getIntegrationSID(), RemoteForm.formatJSONNull(property.entity.getExternalType(context), properties.get(property).get(gridObjectRow).getValue()));
    } 

    private ImSet<PropertyDrawInstance> filterPropertiesExternal(FormInstanceContext context, ImSet<PropertyDrawInstance> serializeProps, final boolean panel) {
        return serializeProps.filterFn(property -> !property.isList() == panel && property.isProperty(context) && property.getIntegrationSID() != null);
    } 

    public JSONObject serializeExternal(FormInstanceContext context) {

        // modify
        JSONObject modifyJSON = new JSONObject();
        ImMap<GroupObjectInstance, ImSet<PropertyDrawInstance>> groupProperties = getGroupProperties();
        for (int i=0,size=groupProperties.size();i<size;i++) {
            GroupObjectInstance groupObject = groupProperties.getKey(i);
            if(groupObject == GroupObjectInstance.NULL)
                groupObject = null;
            ImSet<PropertyDrawInstance> properties = groupProperties.getValue(i);

            JSONObject groupObjectJSON = modifyJSON;
            if(groupObject != null) {
                groupObjectJSON = new JSONObject();

                // grid
                boolean updateGridObjects = true;
                ImOrderSet<ImMap<ObjectInstance, DataObject>> rows = gridObjects.get(groupObject);
                if(rows == null) {
                    updateGridObjects = false;
                    rows = groupObject.keys.keyOrderSet();
                }
                ImSet<PropertyDrawInstance> gridProperties = filterPropertiesExternal(context, properties, false);
                if(rows != null && (updateGridObjects || !gridProperties.isEmpty())) { // has grid and props or keys
                    JSONArray rowsJSON = new JSONArray();
                    for (ImMap<ObjectInstance, DataObject> gridObjectRow : rows) {
                        JSONObject rowJSON = new JSONObject();
                        // grid props
                        serializePropertiesExternal(context, rowJSON, gridProperties, gridObjectRow);
                        // grid keys (we'll need it anyway, for async deletes)
                        rowJSON.put("value", RemoteForm.formatJSON(groupObject, gridObjectRow));
                        rowsJSON.put(rowJSON);
                    }
                    groupObjectJSON.put("list", rowsJSON);
                }

                // current
                ImMap<ObjectInstance, ? extends ObjectValue> currentObjects = objects.get(groupObject);
                if(currentObjects != null)
                    groupObjectJSON.put("value", RemoteForm.formatJSON(groupObject, currentObjects));

                modifyJSON.put(groupObject.getIntegrationSID(), groupObjectJSON);
            }

            // panel props 
            ImSet<PropertyDrawInstance> panelProperties = filterPropertiesExternal(context, properties, true);
            serializePropertiesExternal(context, groupObjectJSON, panelProperties, MapFact.EMPTY());
        }

        // drop props
        JSONArray dropJSON = new JSONArray();
        for (PropertyDrawInstance dropProperty : dropProperties) {
            String integrationSID = dropProperty.getIntegrationSID();
            if (integrationSID != null) {
                dropJSON.put(integrationSID);
            }
        }

        JSONObject response = new JSONObject();
        response.put("modify", modifyJSON);
        response.put("drop", dropJSON);
        return response;
    }

    public ImMap<GroupObjectInstance, ImSet<PropertyDrawInstance>> getGroupProperties() {
        MExclMap<GroupObjectInstance, MExclSet<PropertyDrawInstance>> mGroupProperties = MapFact.mExclMap();
        for(GroupObjectInstance group : gridObjects.keys().merge(objects.keys()))
            mGroupProperties.exclAdd(group, SetFact.mExclSet());
        for (PropertyReaderInstance property : properties.keyIt()) {
            if (property instanceof PropertyDrawInstance) {
                GroupObjectInstance toDraw = ((PropertyDrawInstance) property).toDraw;
                if(toDraw == null)
                    toDraw = GroupObjectInstance.NULL;
                MExclSet<PropertyDrawInstance> mProperties = mGroupProperties.get(toDraw);
                if (mProperties == null) {
                    mProperties = SetFact.mExclSet();
                    mGroupProperties.exclAdd(toDraw, mProperties);
                }
                mProperties.exclAdd((PropertyDrawInstance) property);
            }
        }
        return MapFact.immutable(mGroupProperties);
    }

    public void logChanges(FormInstance bv, Logger logger) {
        logger.trace("  GROUPOBJECTS ---------------");
        for (GroupObjectInstance group : bv.getGroups()) {
            ImOrderSet<ImMap<ObjectInstance, DataObject>> groupGridObjects = gridObjects.get(group);
            if (groupGridObjects != null) {
                logger.trace("   " + group.getID() + " - Current grid objects changed to:");
                for (ImMap<ObjectInstance, DataObject> value : groupGridObjects)
                    logger.trace("     " + value);
            }

            ImMap<ObjectInstance, ? extends ObjectValue> value = objects.get(group);
            if (value != null) {
                logger.trace("   " + group.getID() + " - Current object changed to:  " + value);
            }
        }

        logger.trace("  PROPERTIES ---------------");
        logger.trace("   Values ---------------");
        for (PropertyReaderInstance property : properties.keyIt()) {
            ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue> propertyValues = properties.get(property);
            logger.trace("    " + property + " ---- property");
            for (int i=0,size=propertyValues.size();i<size;i++)
                logger.trace("      " + propertyValues.getKey(i) + " -> " + propertyValues.getValue(i));
        }

        logger.trace("   Dropped ---------------");
        for (PropertyDrawInstance property : dropProperties)
            logger.trace("     " + property);

        logger.trace("   Activate tabs ---------------");
        for (ComponentView tab : activateTabs) {
            logger.trace("     " + tab);
        }

        logger.trace("   Activate props ---------------");
        for (PropertyDrawInstance property : activateProps)
            logger.trace("     " + property);

        logger.trace("   Collapse containers ---------------");
        for (ContainerView container : collapseContainers)
            logger.trace("     " + container);

        logger.trace("   Expand containers ---------------");
        for (ContainerView container : expandContainers)
            logger.trace("     " + container);
    }
}
