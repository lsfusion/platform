package lsfusion.client.logics;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.context.ApplicationContext;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;
import lsfusion.client.descriptor.PropertyDrawDescriptor;
import lsfusion.client.form.*;
import lsfusion.client.form.cell.PanelView;
import lsfusion.client.logics.classes.ClientClass;
import lsfusion.client.logics.classes.ClientType;
import lsfusion.client.logics.classes.ClientTypeSerializer;
import lsfusion.client.serialization.ClientIdentitySerializable;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.PropertyEditType;
import lsfusion.interop.form.PropertyReadType;
import lsfusion.interop.form.layout.SimplexConstraints;
import lsfusion.interop.form.screen.ExternalScreenConstraints;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.*;
import static lsfusion.client.ClientResourceBundle.getString;

@SuppressWarnings({"UnusedDeclaration"})
public class ClientPropertyDraw extends ClientComponent implements ClientPropertyReader, ClientIdentitySerializable {
    public CaptionReader captionReader = new CaptionReader();
    public ShowIfReader showIfReader = new ShowIfReader();
    public BackgroundReader backgroundReader = new BackgroundReader();
    public ForegroundReader foregroundReader = new ForegroundReader();
    public FooterReader footerReader = new FooterReader();
    public ReadOnlyReader readOnlyReader = new ReadOnlyReader();

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public ClientType baseType;
    public ClientClass returnClass;

    // асинхронные интерфейсы
    public ClientType changeType;
    public ClientType changeWYSType;
    public Pair<ClientObject, Boolean> addRemove;
    public boolean askConfirm;
    public String askConfirmMessage;

    public String[] interfacesCaptions;
    public ClientClass[] interfacesTypes;

    public String caption;
    public String regexp;
    public String regexpMessage;
    public boolean echoSymbols;

    public KeyStroke editKey;
    public boolean showEditKey;

    public boolean drawAsync; // рисовать асинхронность на этой кнопке

    public EditBindingMap editBindingMap;

    public Boolean focusable;
    public PropertyEditType editType = PropertyEditType.EDITABLE;

    public boolean panelLabelAbove;

    public ClientExternalScreen externalScreen;
    public ExternalScreenConstraints externalScreenConstraints;

    public int minimumCharWidth;
    public int maximumCharWidth;
    public int preferredCharWidth;

    private transient PropertyRenderer renderer;

    protected Format format;

    public boolean checkEquals;

    protected String sID;

    public String toolTip;

    public ClientGroupObject groupObject;
    public List<ClientGroupObject> columnGroupObjects = new ArrayList<ClientGroupObject>();

    public boolean autoHide;
    public boolean showTableFirst;
    public boolean clearText;
    public String tableName;
    public String eventID;
    public boolean editOnSingleClick;
    public boolean hide;

    public Boolean hideUser;
    public Integer widthUser;
    public Integer orderUser;
    public Integer sortUser;
    public Boolean ascendingSortUser;

    public String creationScript;
    public String creationPath;
    
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
        return editKey;
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

    public boolean isReadOnly() {
        return editType == PropertyEditType.READONLY;
    }

    public ClientGroupObject getGroupObject() {
        return groupObject;
    }

    public LinkedHashMap<String, String> getContextMenuItems() {
        return editBindingMap == null ? null : editBindingMap.getContextMenuItems();
    }

    public PropertyEditor getValueEditorComponent(ClientFormController form, Object value) {
        return baseType.getValueEditorComponent(form, this, value);
    }

    public PropertyRenderer getRendererComponent() {
        if (renderer == null) {
            renderer = baseType.getRendererComponent(this);
        }
        return renderer;
    }

    public PanelView getPanelView(ClientFormController form, ClientGroupObjectValue columnKey) {
        return baseType.getPanelView(this, columnKey, form);
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
        return new Dimension(max(getMaximumWidth(comp), getPreferredWidth(comp)), max(getMaximumHeight(comp), getPreferredHeight(comp)));
    }

    public Format getFormat() {
        if (format == null) {
            return baseType.getDefaultFormat();
        }
        return format;
    }

    public String getEditCaption(String caption) {
        if (caption == null) {
            caption = this.caption;
        }

        return showEditKey && editKey != null
               ? caption + " (" + SwingUtils.getKeyStrokeCaption(editKey) + ")"
               : caption;
    }

    public String getEditCaption() {
        return getEditCaption(caption);
    }

    public String getSID() {
        return sID;
    }

    public Object parseChangeValue(String s) throws ParseException {
        if (changeWYSType == null) {
            throw new ParseException("parsing isn't supported for that property", 0);
        }
        return changeWYSType.parseString(s);
    }

    public Object parseBaseValue(String s) throws ParseException {
        return baseType.parseString(s);
    }

    public String formatString(Object obj) throws ParseException {
      return baseType.formatString(obj);
    }

    public boolean shouldBeDrawn(ClientFormController form) {
        return baseType.shouldBeDrawn(form);
    }

    public byte getType() {
        return PropertyReadType.DRAW;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.writeString(outStream, caption);
        pool.writeString(outStream, regexp);
        pool.writeString(outStream, regexpMessage);
        outStream.writeBoolean(echoSymbols);
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

        outStream.writeInt(ID);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        caption = pool.readString(inStream);
        regexp = pool.readString(inStream);
        regexpMessage = pool.readString(inStream);
        echoSymbols = inStream.readBoolean();
        minimumCharWidth = inStream.readInt();
        maximumCharWidth = inStream.readInt();
        preferredCharWidth = inStream.readInt();

        editKey = pool.readObject(inStream);
        showEditKey = inStream.readBoolean();
        drawAsync = inStream.readBoolean();

        format = pool.readObject(inStream);

        focusable = pool.readObject(inStream);
        editType = PropertyEditType.deserialize(inStream.readByte());

        panelLabelAbove = inStream.readBoolean();

        if (inStream.readBoolean()) {
            externalScreen = ClientExternalScreen.getScreen(inStream.readInt());
        }

        externalScreenConstraints = pool.readObject(inStream);

        autoHide = inStream.readBoolean();
        showTableFirst = inStream.readBoolean();
        editOnSingleClick = inStream.readBoolean();
        hide = inStream.readBoolean();

        baseType = ClientTypeSerializer.deserializeClientType(inStream);
        if (inStream.readBoolean()) {
            changeType = ClientTypeSerializer.deserializeClientType(inStream);
        }
        if (inStream.readBoolean()) {
            changeWYSType = ClientTypeSerializer.deserializeClientType(inStream);
        }

        if(inStream.readBoolean()) {
            addRemove = new Pair<ClientObject, Boolean>(pool.<ClientObject>deserializeObject(inStream), inStream.readBoolean());
        }

        askConfirm = inStream.readBoolean();
        if(askConfirm)
            askConfirmMessage = pool.readString(inStream);

        sID = pool.readString(inStream);

        toolTip = pool.readString(inStream);

        groupObject = pool.deserializeObject(inStream);

        columnGroupObjects = pool.deserializeList(inStream);

        checkEquals = inStream.readBoolean();
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
        eventID = pool.readString(inStream);

        creationScript = pool.readString(inStream);
        creationPath = pool.readString(inStream);

        String mouseBinding = pool.readString(inStream);
        if (mouseBinding != null) {
            initEditBindingMap();
            editBindingMap.setMouseAction(mouseBinding);
        }

        int keyBindingSize = inStream.readInt();
        if (keyBindingSize > 0) {
            initEditBindingMap();
            for (int i = 0; i < keyBindingSize; ++i) {
                KeyStroke keyStroke = pool.readObject(inStream);
                String actionSID = pool.readString(inStream);
                editBindingMap.setKeyAction(keyStroke, actionSID);
            }
        }

        int contextMenuBindingsSize = inStream.readInt();
        if (contextMenuBindingsSize > 0) {
            initEditBindingMap();
            for (int i = 0; i < contextMenuBindingsSize; ++i) {
                String actionSID = pool.readString(inStream);
                String caption = pool.readString(inStream);
                editBindingMap.setContextMenuAction(actionSID, caption);
            }
        }
    }

    private void initEditBindingMap() {
        if (editBindingMap == null) {
            editBindingMap = new EditBindingMap();
        }
    }

    public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, GroupObjectLogicsSupplier controller) {
        controller.updateDrawPropertyValues(this, readKeys, updateKeys);
    }

    @Override
    public String getCaption() {
        return caption;
    }

    public String getDynamicCaption(Object captionValue) {
        return BaseUtils.toCaption(captionValue);
    }

    @Override
    public String toString() {
        if (!isRedundantString(caption))
            return caption + " (" + getID() + ")";

        if (descriptor != null && descriptor.getPropertyObject() != null &&
                descriptor.getPropertyObject().property.caption != null) {
            return descriptor.getPropertyObject().property.caption + " (" + getID() + ")";
        }

        return getString("logics.undefined.property");
    }

    // приходится держать ссылку на Descriptor, чтобы правильно отображать caption в Настройка бизнес-логики
    private PropertyDrawDescriptor descriptor;

    public void setDescriptor(PropertyDrawDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public PropertyDrawDescriptor getDescriptor() {
        return descriptor;
    }

    public static final String TOOL_TIP_FORMAT =
            "<html><b>%1$s</b><br>" +
                    "%2$s";

    public static final String DETAILED_TOOL_TIP_FORMAT =
            "<hr>" +
                    "<b>sID:</b> %3$s<br>" +
                    "<b>" + getString("logics.grid") + ":</b> %4$s<br>" +
                    "<b>" + getString("logics.objects") + ":</b> %5$s<br>" +
                    "<b>" + getString("logics.signature") + ":</b> %7$s <i>%3$s</i> (%6$s)<br>" +
                    "<b>" + getString("logics.script") + ":</b> %8$s<br>" +
                    "<b>" + getString("logics.scriptpath") + ":</b> %9$s" +
                    "</html>";

    public static final String EDIT_KEY_TOOL_TIP_FORMAT =
            "<hr><b>" + getString("logics.property.edit.key") + ":</b> %1$s<br>";

    public String getTooltipText(String caption) {
        boolean configurationAccessAllowed;
        configurationAccessAllowed = Main.configurationAccessAllowed;

        String propCaption = nullTrim(!isRedundantString(toolTip) ? toolTip : caption);
        String editKeyText = editKey == null ? "" : String.format(EDIT_KEY_TOOL_TIP_FORMAT, SwingUtils.getKeyStrokeCaption(editKey));

        if (!configurationAccessAllowed) {
            return String.format(TOOL_TIP_FORMAT, propCaption, editKeyText);
        } else {
            String sid = getSID();
            String tableName = this.tableName != null ? this.tableName : "&lt;none&gt;";
            String ifaceObjects = BaseUtils.toString(", ", interfacesCaptions);
            String ifaceClasses = BaseUtils.toString(", ", interfacesTypes);
            String returnClass = this.returnClass.toString();

            String script = creationScript != null ? creationScript.replace("\n", "<br>") : "";
            String scriptPath = creationPath != null ? creationPath.replace("\n", "<br>") : "";
            return String.format(TOOL_TIP_FORMAT + DETAILED_TOOL_TIP_FORMAT, propCaption, editKeyText, sid, tableName, ifaceObjects, ifaceClasses, returnClass, script, scriptPath);
        }
    }

    public class CaptionReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public boolean shouldBeDrawn(ClientFormController form) {
            return ClientPropertyDraw.this.shouldBeDrawn(form);
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, GroupObjectLogicsSupplier controller) {
            controller.updateDrawPropertyCaptions(ClientPropertyDraw.this, readKeys);
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.CAPTION;
        }
    }

    public class ShowIfReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public boolean shouldBeDrawn(ClientFormController form) {
            return ClientPropertyDraw.this.shouldBeDrawn(form);
        }

        public void update(Map<ClientGroupObjectValue, Object> values, boolean updateKeys, GroupObjectLogicsSupplier controller) {
            controller.updateShowIfs(ClientPropertyDraw.this, values);
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.SHOWIF;
        }
    }

    public class FooterReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public boolean shouldBeDrawn(ClientFormController form) {
            return false;
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, GroupObjectLogicsSupplier controller) {
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.FOOTER;
        }
    }

    public class ReadOnlyReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public boolean shouldBeDrawn(ClientFormController form) {
            return ClientPropertyDraw.this.shouldBeDrawn(form);
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, GroupObjectLogicsSupplier controller) {
            controller.updateReadOnlyValues(ClientPropertyDraw.this, readKeys);
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.READONLY;
        }
    }

    public class BackgroundReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public boolean shouldBeDrawn(ClientFormController form) {
            return ClientPropertyDraw.this.shouldBeDrawn(form);
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, GroupObjectLogicsSupplier controller) {
            controller.updateCellBackgroundValues(ClientPropertyDraw.this, readKeys);
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.CELL_BACKGROUND;
        }
    }

    public class ForegroundReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public boolean shouldBeDrawn(ClientFormController form) {
            return ClientPropertyDraw.this.shouldBeDrawn(form);
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, GroupObjectLogicsSupplier controller) {
            controller.updateCellForegroundValues(ClientPropertyDraw.this, readKeys);
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.CELL_FOREGROUND;
        }
    }
}
