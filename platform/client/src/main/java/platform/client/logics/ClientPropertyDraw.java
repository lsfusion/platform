package platform.client.logics;

import platform.base.BaseUtils;
import platform.base.context.ApplicationContext;
import platform.client.ClientResourceBundle;
import platform.client.SwingUtils;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.PropertyObjectInterfaceDescriptor;
import platform.client.form.*;
import platform.client.form.cell.CellView;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientType;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.gwt.view.GPropertyDraw;
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

import static platform.base.BaseUtils.isRedundantString;
import static platform.base.BaseUtils.nullTrim;

@SuppressWarnings({"UnusedDeclaration"})
public class ClientPropertyDraw extends ClientComponent implements ClientPropertyReader, ClientIdentitySerializable {
    public CaptionReader captionReader = new CaptionReader();
    public HighlightReader highlightReader = new HighlightReader();
    public FooterReader footerReader = new FooterReader();

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public ClientType baseType;
    public ClientType changeType;
    public ClientClass[] interfacesTypes;
    public ClientClass returnClass;

    public String[] interfacesCaptions;

    public Color highlightColor;

    public String caption;
    public String regexp;
    public String regexpMessage;

    public KeyStroke editKey;
    public boolean showEditKey;

    public Boolean focusable;
    public boolean readOnly;

    public boolean panelLabelAbove;

    public ClientExternalScreen externalScreen;
    public ExternalScreenConstraints externalScreenConstraints;

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

    public boolean autoHide;
    public boolean showTableFirst;
    public boolean clearText;
    public String tableName;
    public String eventSID;
    public boolean editOnSingleClick;
    public boolean hide;

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

    public ClientGroupObject getGroupObject() {
        return groupObject;
    }

    public PropertyEditorComponent getEditorComponent(Component ownerComponent, ClientFormController form, ClientGroupObjectValue key, Object value) throws IOException, ClassNotFoundException {
        ClientType changeType = getPropertyChangeType(form, key, true);
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

        return changeType.getEditorComponent(ownerComponent, form, this, value);
    }

    public PropertyEditorComponent getObjectEditorComponent(Component ownerComponent, ClientFormController form, ClientGroupObjectValue key, Object value) throws IOException, ClassNotFoundException {
        ClientType changeType = getPropertyChangeType(form, key, true);
        return changeType == null
                ? null
                : changeType.getObjectEditorComponent(ownerComponent, form, this, value);
    }

    public PropertyEditorComponent getClassComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException {
        return baseType.getClassComponent(form, this, value);
    }

    public RemoteDialogInterface createEditorForm(RemoteFormInterface form) throws RemoteException {
        return form.createEditorPropertyDialog(getID());
    }

    public RemoteDialogInterface createClassForm(RemoteFormInterface form, Integer value) throws RemoteException {
        return form.createClassPropertyDialog(getID(), BaseUtils.objectToInt(value));
    }

    public int getMinimumWidth(JComponent comp) {
        if (minimumSize != null && minimumSize.width > -1) {
            return minimumSize.width;
        }
        return baseType.getMinimumWidth(minimumCharWidth, comp.getFontMetrics(design.getFont(comp)));
    }

    public int getMinimumHeight(JComponent comp) {
        if (minimumSize != null && minimumSize.height > -1) {
            return minimumSize.height;
        }
        return getPreferredHeight(comp);
    }


    public Dimension getMinimumSize(JComponent comp) {
        if (minimumSize != null) {
            return new Dimension(minimumSize.width != -1 ? minimumSize.width : getMinimumWidth(comp),
                    minimumSize.height != -1 ? minimumSize.height : getMinimumHeight(comp));
        }
        return new Dimension(getMinimumWidth(comp), getMinimumHeight(comp));
    }

    public int getPreferredWidth(JComponent comp) {
        if (preferredSize != null && preferredSize.width > -1) {
            return preferredSize.width;
        }
        return baseType.getPreferredWidth(preferredCharWidth, comp.getFontMetrics(design.getFont(comp)));
    }

    public int getPreferredHeight(JComponent comp) {
        if (preferredSize != null && preferredSize.height > -1) {
            return preferredSize.height;
        }
        int height = baseType.getPreferredHeight(comp.getFontMetrics(design.getFont(comp)));
        if (design.getImage() != null) // предпочитаемую высоту берем исходя из размера иконки
            height = Math.max(design.getImage().getIconHeight() + 6, height);
        return height;
    }

    public Dimension getPreferredSize(JComponent comp) {
        if (preferredSize != null) {
            return new Dimension(preferredSize.width != -1 ? preferredSize.width : getPreferredWidth(comp),
                    preferredSize.height != -1 ? preferredSize.height : getPreferredHeight(comp));
        }
        return new Dimension(getPreferredWidth(comp), getPreferredHeight(comp));
    }

    public int getMaximumWidth(JComponent comp) {
        if (maximumSize != null && maximumSize.width > -1) {
            return maximumSize.width;
        }
        return baseType.getMaximumWidth(maximumCharWidth, comp.getFontMetrics(design.getFont(comp)));
    }

    public int getMaximumHeight(JComponent comp) {
        if (maximumSize != null && maximumSize.width > -1) {
            return maximumSize.height;
        }
        return baseType.getMaximumHeight(comp.getFontMetrics(design.getFont(comp)));
    }

    public Dimension getMaximumSize(JComponent comp) {
        if (maximumSize != null) {
            return new Dimension(maximumSize.width != -1 ? maximumSize.width : getMaximumWidth(comp),
                    maximumSize.height != -1 ? maximumSize.height : getMaximumHeight(comp));
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

    public Format getFormat() {
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

    public ClientType getPropertyChangeType(ClientFormController form, ClientGroupObjectValue key, boolean aggValue) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(form.remoteForm.getPropertyChangeType(getID(), key.serialize(), aggValue)));
        if (inStream.readBoolean()) {
            return null;
        }

        return ClientTypeSerializer.deserialize(inStream);
    }

    public Object parseString(ClientFormController form, ClientGroupObjectValue key, String s, boolean isDataChanging) throws ParseException {
        ClientType changeType;
        try {
            changeType = isDataChanging ? getPropertyChangeType(form, key, false) : baseType;
            if (changeType == null) {
                throw new ParseException(ClientResourceBundle.getString("logics.propertyview.can.not.be.changed"), 0);
            }

            return changeType.parseString(s);
        } catch (IOException e) {
            throw new ParseException(ClientResourceBundle.getString("logics.failed.to.retrieve.data.propertychangetype"), 0);
        }
    }

    public String formatString(Object obj) throws ParseException {
      return baseType.formatString(obj);
    }

    public boolean shouldBeDrawn(ClientFormController form) {
        return baseType.shouldBeDrawn(form);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.writeString(outStream, caption);
        pool.writeString(outStream,regexp);
        pool.writeString(outStream,regexpMessage);
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
        outStream.writeBoolean(showTableFirst);
        outStream.writeBoolean(editOnSingleClick);
        outStream.writeBoolean(hide);

        pool.writeObject(outStream, highlightColor);

        outStream.writeInt(ID);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        caption = pool.readString(inStream);
        regexp = pool.readString(inStream);
        regexpMessage = pool.readString(inStream);
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
        showTableFirst = inStream.readBoolean();
        editOnSingleClick = inStream.readBoolean();
        hide = inStream.readBoolean();

        highlightColor = pool.readObject(inStream);

        baseType = ClientTypeSerializer.deserialize(inStream);
        changeType = ClientTypeSerializer.deserialize(inStream);

        sID = pool.readString(inStream);

        toolTip = pool.readString(inStream);

        groupObject = pool.deserializeObject(inStream);

        columnGroupObjects = pool.deserializeList(inStream);

        checkEquals = inStream.readBoolean();
        askConfirm = inStream.readBoolean();
        clearText = inStream.readBoolean();

        tableName = pool.readString(inStream);

        int n = inStream.readInt();
        interfacesCaptions = new String[n];
        interfacesTypes = new ClientClass[n];
        for (int i = 0; i < n; ++i) {
            interfacesCaptions[i] = pool.readString(inStream);
            interfacesTypes[i] = ClientTypeSerializer.deserializeClientClass(inStream);
        }

        returnClass = ClientTypeSerializer.deserializeClientClass(inStream);
        eventSID = inStream.readUTF();
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
        if (!isRedundantString(caption))
            return caption + " (" + getID() + ")";

        if (descriptor != null && descriptor.getPropertyObject() != null &&
                descriptor.getPropertyObject().property.caption != null) {
            return descriptor.getPropertyObject().property.caption + " (" + getID() + ")";
        }

        return ClientResourceBundle.getString("logics.undefined.property");
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

    public static final String toolTipFormat =
            "<html><b>%1$s</b><br><hr>" +
                    "<b>sID:</b> %2$s<br>" +
                    "<b>" + ClientResourceBundle.getString("logics.grid") + ":</b> %3$s<br>" +
                    "<b>" + ClientResourceBundle.getString("logics.objects") + " :</b> %4$s<br>" +
                    "<b>" + ClientResourceBundle.getString("logics.signature") + ":</b> %6$s <i>%2$s</i> (%5$s)" +
                    "</html>";

    public String getTooltipText(String caption) {
        String propCaption = nullTrim(!isRedundantString(toolTip) ? toolTip : caption);
        String sid = getSID();
        String tableName = this.tableName != null ? this.tableName : "&lt;none&gt;";
        String ifaceObjects = BaseUtils.toString(", ", interfacesCaptions);
        String ifaceClasses = BaseUtils.toString(", ", interfacesTypes);
        String returnClass = this.returnClass.toString();

        return String.format(toolTipFormat, propCaption, sid, tableName, ifaceObjects, ifaceClasses, returnClass);
    }

    private GPropertyDraw gwtPropertyDraw;

    public GPropertyDraw getGwtPropertyDraw() {
        return getGwtComponent();
    }

    public GPropertyDraw getGwtComponent() {
        if (gwtPropertyDraw == null) {
            gwtPropertyDraw = new GPropertyDraw();

            initGwtComponent(gwtPropertyDraw);

            gwtPropertyDraw.ID = ID;
            gwtPropertyDraw.sID = sID;
            gwtPropertyDraw.caption = caption;
            gwtPropertyDraw.groupObject = groupObject == null ? null : groupObject.getGwtGroupObject();
            gwtPropertyDraw.baseType = baseType.getGwtType();
            gwtPropertyDraw.changeType = changeType.getGwtType();
            gwtPropertyDraw.iconPath = design.iconPath;
            gwtPropertyDraw.readOnly = readOnly;
            gwtPropertyDraw.focusable = focusable;
            gwtPropertyDraw.checkEquals = checkEquals;
        }
        return gwtPropertyDraw;
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

    public class FooterReader implements ClientPropertyReader {
        public List<ClientObject> getKeysObjectsList(Set<ClientPropertyReader> panelProperties, Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers) {
            return ClientPropertyDraw.this.getKeysObjectsList(classViews, controllers);
        }

        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public boolean shouldBeDrawn(ClientFormController form) {
            return false;
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, GroupObjectController controller) {
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
