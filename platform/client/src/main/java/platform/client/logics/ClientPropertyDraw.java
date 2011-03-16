package platform.client.logics;

import platform.base.BaseUtils;
import platform.client.SwingUtils;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.base.context.ApplicationContext;
import platform.client.descriptor.PropertyObjectInterfaceDescriptor;
import platform.client.form.*;
import platform.client.form.cell.CellView;
import platform.client.logics.classes.ClientType;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.layout.SimplexConstraints;
import platform.interop.form.screen.ExternalScreenConstraints;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.Format;
import java.text.ParseException;
import java.util.*;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration"})
public class ClientPropertyDraw extends ClientComponent implements ClientPropertyReader, ClientIdentitySerializable {
    public CaptionReader captionReader = new CaptionReader();
    public HighlightReader highlightReader = new HighlightReader();

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public ClientType baseType;

    public Color highlightColor;

    public String caption;

    public KeyStroke editKey;
    public boolean showEditKey;

    public Boolean focusable;
    public boolean readOnly;

    public boolean panelLabelAbove;

    public ClientExternalScreen externalScreen;
    public ExternalScreenConstraints externalScreenConstraints;

    protected Dimension minimumSize;
    protected Dimension maximumSize;
    protected Dimension preferredSize;

    public int minimumCharWidth;
    public int maximumCharWidth;
    public int preferredCharWidth;

    protected transient PropertyRendererComponent renderer;

    // на данный момент ClientFormController нужна для 2-х целей : как owner, создаваемых диалогов и как провайдер RemoteFormInterface, для получения того, что мы вообще редактируем

    protected Format format;

    public boolean checkEquals;
    public boolean askConfirm;

    protected String sID;

    public String toolTip;

    public ClientGroupObject groupObject;
    public List<ClientGroupObject> columnGroupObjects = new ArrayList<ClientGroupObject>();

    public boolean autoHide = false;
    public boolean clearText;

    public ClientPropertyDraw() {
    }

    public ClientPropertyDraw(int ID, ApplicationContext context) {
        super(ID, context);
    }

    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return SimplexConstraints.getPropertyDrawDefaultConstraints(super.getDefaultConstraints());
    }

    @Override
    public boolean shouldBeDeclared() {
        return super.shouldBeDeclared() || editKey != null || !columnGroupObjects.isEmpty();
    }

    public KeyStroke getEditKey() {
        return (editKey != null) ? editKey : null;
    }

    public void setEditKey(KeyStroke key) {
        this.editKey = key;
        updateDependency(this, "editKey");
    }

    public boolean getShowEditKey() {
        return showEditKey;
    }

    public void setShowEditKey(boolean showKey) {
        showEditKey = showKey;
        updateDependency(this, "showEditKey");
    }

    private ClientGroupObject getClientGroupObject(Collection<ClientGroupObject> groups, int groupID) {
        for (ClientGroupObject group : groups) {
            if (group.getID() == groupID) {
                return group;
            }
        }
        return null;
    }

    public ClientGroupObject getKeyBindingGroup() {
        return BaseUtils.nvl(keyBindingGroup, getGroupObject());
    }

    public ClientGroupObject getGroupObject() {
        return groupObject;
    }

    public PropertyEditorComponent getEditorComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException {
        ClientType changeType = getPropertyChangeType(form);
        if (changeType == null) {
            return null;
        }

        if (askConfirm) {
            int n = JOptionPane.showConfirmDialog(
                    null,
                    baseType.getConformedMessage() + " \"" + caption + "\"?",
                    "LS Fusion",
                    JOptionPane.YES_NO_OPTION);
            if (n != JOptionPane.YES_OPTION) {
                return null;
            }
        }

        return changeType.getEditorComponent(form, this, value, getFormat(), design);
    }

    public PropertyEditorComponent getObjectEditorComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException {
        ClientType changeType = getPropertyChangeType(form);
        return changeType == null
                ? null
                : changeType.getObjectEditorComponent(form, this, value, getFormat(), design);
    }

    public PropertyEditorComponent getClassComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException {
        return baseType.getClassComponent(form, this, value, getFormat());
    }

    public RemoteDialogInterface createEditorForm(RemoteFormInterface form) throws RemoteException {
        return form.createEditorPropertyDialog(getID());
    }

    public RemoteDialogInterface createClassForm(RemoteFormInterface form, Integer value) throws RemoteException {
        return form.createClassPropertyDialog(getID(), BaseUtils.objectToInt(value));
    }

    public int getMinimumWidth(JComponent comp) {
        if (minimumSize != null) {
            return minimumSize.width;
        }
        return baseType.getMinimumWidth(minimumCharWidth, comp.getFontMetrics(design.getFont(comp)));
    }

    public int getMinimumHeight(JComponent comp) {
        if (minimumSize != null) {
            return minimumSize.height;
        }
        return getPreferredHeight(comp);
    }


    public Dimension getMinimumSize(JComponent comp) {
        if (minimumSize != null) {
            return minimumSize;
        }
        return new Dimension(getMinimumWidth(comp), getMinimumHeight(comp));
    }

    public int getPreferredWidth(JComponent comp) {
        if (preferredSize != null) {
            return preferredSize.width;
        }
        return baseType.getPreferredWidth(preferredCharWidth, comp.getFontMetrics(design.getFont(comp)));
    }

    public int getPreferredHeight(JComponent comp) {
        if (preferredSize != null) {
            return preferredSize.height;
        }
        return baseType.getPreferredHeight(comp.getFontMetrics(design.getFont(comp)));
    }

    public Dimension getPreferredSize(JComponent comp) {
        if (preferredSize != null) {
            return preferredSize;
        }
        return new Dimension(getPreferredWidth(comp), getPreferredHeight(comp));
    }

    public int getMaximumWidth(JComponent comp) {
        if (maximumSize != null) {
            return maximumSize.width;
        }
        return baseType.getMaximumWidth(maximumCharWidth, comp.getFontMetrics(design.getFont(comp)));
    }

    public int getMaximumHeight(JComponent comp) {
        if (maximumSize != null) {
            return maximumSize.height;
        }
        return getPreferredHeight(comp);
    }

    public Dimension getMaximumSize(JComponent comp) {
        if (maximumSize != null) {
            return maximumSize;
        }
        return new Dimension(getMaximumWidth(comp), getMaximumHeight(comp));
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(Color highlightColor) {
        this.highlightColor = highlightColor;
    }

    public PropertyRendererComponent getRendererComponent() {
        if (renderer == null) {
            renderer = baseType.getRendererComponent(getFormat(), caption, design);
        }
        return renderer;
    }

    public CellView getPanelComponent(ClientFormController form, ClientGroupObjectValue columnKey) {
        return baseType.getPanelComponent(this, columnKey, form);
    }

    Format getFormat() {
        if (format == null) {
            return baseType.getDefaultFormat();
        }
        return format;
    }

    public String getFullCaption() {

        String fullCaption = caption;
        if (showEditKey && editKey != null) {
            fullCaption += " (" + SwingUtils.getKeyStrokeCaption(editKey) + ")";
        }
        return fullCaption;
    }

    public String getSID() {
        return sID;
    }

    public ClientType getPropertyChangeType(ClientFormController form) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(form.remoteForm.getPropertyChangeType(getID())));
        if (inStream.readBoolean()) {
            return null;
        }

        return ClientTypeSerializer.deserialize(inStream);
    }

    public Object parseString(ClientFormController form, String s) throws ParseException {
        ClientType changeType;
        try {
            changeType = getPropertyChangeType(form);
            if (changeType == null) {
                throw new ParseException("PropertyView не может быть изменено.", 0);
            }

            return changeType.parseString(s);
        } catch (IOException e) {
            throw new ParseException("Ошибка получения данных о propertyChangeType.", 0);
        }
    }

    public boolean shouldBeDrawn(ClientFormController form) {
        return baseType.shouldBeDrawn(form);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.writeString(outStream, caption);

        pool.writeObject(outStream, minimumSize);
        pool.writeObject(outStream, maximumSize);
        pool.writeObject(outStream, preferredSize);

        outStream.writeInt(minimumCharWidth);
        outStream.writeInt(maximumCharWidth);
        outStream.writeInt(preferredCharWidth);

        pool.writeObject(outStream, editKey);

        outStream.writeBoolean(showEditKey);

        pool.writeObject(outStream, format);
        pool.writeObject(outStream, focusable);

        outStream.writeBoolean(panelLabelAbove);

        outStream.writeBoolean(externalScreen != null);
        if (externalScreen != null) {
            outStream.writeInt(externalScreen.getID());
        }
        pool.writeObject(outStream, externalScreenConstraints);

        outStream.writeBoolean(autoHide);

        pool.writeObject(outStream, highlightColor);

        outStream.writeInt(ID);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        caption = pool.readString(inStream);

        minimumSize = pool.readObject(inStream);
        maximumSize = pool.readObject(inStream);
        preferredSize = pool.readObject(inStream);

        minimumCharWidth = inStream.readInt();
        maximumCharWidth = inStream.readInt();
        preferredCharWidth = inStream.readInt();

        editKey = pool.readObject(inStream);
        showEditKey = inStream.readBoolean();

        format = pool.readObject(inStream);

        focusable = pool.readObject(inStream);
        readOnly = inStream.readBoolean();

        panelLabelAbove = inStream.readBoolean();

        if (inStream.readBoolean()) {
            externalScreen = ClientExternalScreen.getScreen(inStream.readInt());
        }

        externalScreenConstraints = pool.readObject(inStream);

        autoHide = inStream.readBoolean();

        highlightColor = pool.readObject(inStream);

        baseType = ClientTypeSerializer.deserialize(inStream);

        sID = pool.readString(inStream);

        toolTip = pool.readString(inStream);

        groupObject = pool.deserializeObject(inStream);

        columnGroupObjects = pool.deserializeList(inStream);

        checkEquals = inStream.readBoolean();
        askConfirm = inStream.readBoolean();
        clearText = inStream.readBoolean();
    }

    public List<ClientObject> getKeysObjectsList(Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers) {
        List<ClientObject> result = new ArrayList<ClientObject>();
        for (ClientGroupObject columnGroupObject : columnGroupObjects) {
            result.addAll(columnGroupObject.getKeysObjectsList(classViews, controllers));
        }
        return result;
    }

    public List<ClientObject> getKeysObjectsList(Set<ClientPropertyReader> panelProperties, Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers) {
        List<ClientObject> result = getKeysObjectsList(classViews, controllers);
        if (!panelProperties.contains(this)) {
            result = BaseUtils.mergeList(ClientGroupObject.getObjects(groupObject.getUpTreeGroups()), result);
        }
        return result;
    }

    public void update(Map<ClientGroupObjectValue, Object> readKeys, GroupObjectController controller) {
        controller.updateDrawPropertyValues(this, readKeys);
    }

    @Override
    public String getCaption() {
        return caption;
    }

    @Override
    public String toString() {
        if (!BaseUtils.isRedundantString(caption))
            return caption + " (" + getID() + ")";

        if (descriptor != null && descriptor.getPropertyObject() != null &&
                descriptor.getPropertyObject().property.caption != null) {
            return descriptor.getPropertyObject().property.caption + " (" + getID() + ")";
        }

        return "Неопределённое свойство";
    }

    // приходится держать ссылку на Descriptor, чтобы правильно отображать caption в Настройка бизнес-логики
    private PropertyDrawDescriptor descriptor;

    public void setDescriptor(PropertyDrawDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public PropertyDrawDescriptor getDescriptor() {
        return descriptor;
    }

    public String getCodeClass() {
        return "PropertyDrawView";
    }

    @Override
    public String getCodeConstructor() {
        StringBuilder result = new StringBuilder("design.createPropertyDraw(");

        if (descriptor.getPropertyObject().property.isField) {
            result.append("prop").append(descriptor.getSID());
        } else {
            if (getCaption() != null) {
                result.append("\"").append(getCaption()).append("\", ");
            }
            String grObject = groupObject == null ? "" : "grObj" + groupObject.getSID() + ", ";

            result.append(grObject).append(descriptor.getPropertyObject().property.code);

            Set<PropertyObjectInterfaceDescriptor> values = new HashSet<PropertyObjectInterfaceDescriptor>(descriptor.getPropertyObject().mapping.values());

            for (PropertyObjectInterfaceDescriptor objectDescriptorInt : values) {
                if (objectDescriptorInt instanceof ObjectDescriptor) {
                    ObjectDescriptor object = (ObjectDescriptor) objectDescriptorInt;
                    result.append(", ").append(object.getVariableName());
                }
            }
        }
        result.append(")");
        return result.toString();
    }

    public String getCodeEditKey(String name) {
        return "design.setEditKey(" + name + ", KeyStroke.getKeyStroke(\"" + editKey + "\"));\n";
    }

    @Override
    public String getVariableName(FormDescriptor form) {
        return "propertyView" + getID();
    }

    public class CaptionReader implements ClientPropertyReader {
        public List<ClientObject> getKeysObjectsList(Set<ClientPropertyReader> panelProperties, Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers) {
            return ClientPropertyDraw.this.getKeysObjectsList(classViews, controllers);
        }

        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public boolean shouldBeDrawn(ClientFormController form) {
            return ClientPropertyDraw.this.shouldBeDrawn(form);
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, GroupObjectController controller) {
            controller.updateDrawPropertyCaptions(ClientPropertyDraw.this, readKeys);
        }
    }

    public class HighlightReader implements ClientPropertyReader {
        public List<ClientObject> getKeysObjectsList(Set<ClientPropertyReader> panelProperties, Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers) {
            List<ClientObject> result = ClientPropertyDraw.this.getKeysObjectsList(classViews, controllers);
            if (!panelProperties.contains(this)) {
                result = BaseUtils.mergeList(ClientGroupObject.getObjects(groupObject.getUpTreeGroups()), result);
            }
            return result;
        }

        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public boolean shouldBeDrawn(ClientFormController form) {
            return ClientPropertyDraw.this.shouldBeDrawn(form);
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, GroupObjectController controller) {
            controller.updateCellHighlightValues(ClientPropertyDraw.this, readKeys);
        }
    }
}
