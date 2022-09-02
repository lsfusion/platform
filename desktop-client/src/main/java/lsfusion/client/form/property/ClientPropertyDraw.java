package lsfusion.client.form.property;

import lsfusion.base.BaseUtils;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.*;
import lsfusion.client.classes.data.ClientFormatClass;
import lsfusion.client.classes.data.ClientIntegralClass;
import lsfusion.client.classes.data.ClientLogicalClass;
import lsfusion.client.classes.data.ClientLongClass;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.remote.serialization.ClientIdentitySerializable;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.property.async.*;
import lsfusion.client.form.property.cell.EditBindingMap;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.FormatPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.panel.view.PanelView;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.interop.form.property.PropertyReadType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.*;
import java.util.List;
import java.util.*;

import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.base.BaseUtils.nullTrim;
import static lsfusion.base.EscapeUtils.escapeLineBreakHTML;
import static lsfusion.client.ClientResourceBundle.getString;

@SuppressWarnings({"UnusedDeclaration"})
public class ClientPropertyDraw extends ClientComponent implements ClientPropertyReader, ClientIdentitySerializable {

    public CaptionReader captionReader = new CaptionReader();
    public ShowIfReader showIfReader = new ShowIfReader();
    public BackgroundReader backgroundReader = new BackgroundReader();
    public ForegroundReader foregroundReader = new ForegroundReader();
    public FooterReader footerReader = new FooterReader();
    public ReadOnlyReader readOnlyReader = new ReadOnlyReader();
    public ImageReader imageReader = new ImageReader();
    public boolean hasDynamicImage;

    public boolean autoSize;
    public boolean boxed;

    // for pivoting
    public String formula;
    public ClientPropertyDraw[] formulaOperands;

    public String aggrFunc;
    public List<LastReader> lastReaders = new ArrayList<>();
    public boolean lastAggrDesc;

    public ClientPropertyDraw quickFilterProperty;

    public boolean isList;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public ClientType baseType;
    public ClientClass returnClass;

    public ClientType externalChangeType;
    public Map<String, ClientAsyncEventExec> asyncExecMap;
    public boolean askConfirm;
    public String askConfirmMessage;

    public boolean hasEditObjectAction;
    public boolean hasChangeAction;

    public boolean disableInputList;

    public String[] interfacesCaptions;
    public ClientClass[] interfacesTypes;

    public String caption;
    public String regexp;
    public String regexpMessage;
    public Long maxValue;
    public boolean echoSymbols;
    public boolean noSort;
    public Compare defaultCompare;

    public KeyInputEvent changeKey;
    public Integer changeKeyPriority;
    public boolean showChangeKey;
    public MouseInputEvent changeMouse;
    public Integer changeMousePriority;

    public boolean drawAsync; // рисовать асинхронность на этой кнопке

    public Boolean focusable;
    public PropertyEditType editType = PropertyEditType.EDITABLE;

    public boolean panelCaptionVertical;
    public Boolean panelCaptionLast;
    public FlexAlignment panelCaptionAlignment;

    public boolean panelColumnVertical;

    public FlexAlignment valueAlignment;

    public int charWidth;
    public int charHeight;

    public int valueWidth;
    public int valueHeight;

    public int captionWidth;
    public int captionHeight;

    public transient EditBindingMap editBindingMap;
    private transient PropertyRenderer renderer;

    protected Format format;
    private Format defaultFormat;

    public boolean checkEquals;

    protected String namespace;
    protected String canonicalName;
    protected String propertyFormName; // PropertyDrawEntity.sID
    protected String integrationSID;

    public String toolTip;

    public ClientGroupObject groupObject;
    public String columnsName;
    public List<ClientGroupObject> columnGroupObjects = new ArrayList<>();

    public boolean clearText;
    public boolean notSelectAll;
    public String tableName;
    public String eventID;
    public Boolean changeOnSingleClick;
    public boolean hide;

    public String customRenderFunction;

    public String creationScript;
    public String creationPath;
    public String path;
    public String formPath;
    
    public boolean notNull;

    public boolean sticky;

    public boolean hasFooter;

    public ClientPropertyDraw() {
    }

    public Compare getDefaultCompare() {
        return defaultCompare != null ? defaultCompare : Compare.EQUALS;
    }

    public Compare[] getFilterCompares() {
        return baseType.getFilterCompares();
    }

    public KeyInputEvent getChangeKey() {
        return changeKey;
    }

    public void setChangeKey(KeyInputEvent key) {
        this.changeKey = key;
        updateDependency(this, "changeKey");
    }

    public boolean getShowChangeKey() {
        return showChangeKey;
    }

    public void setShowChangeKey(boolean showKey) {
        showChangeKey = showKey;
        updateDependency(this, "showChangeKey");
    }

    public boolean isEditableNotNull() {
        return notNull && !isReadOnly();
    }

    public boolean isEditableChangeAction() {
        return hasChangeAction && !isReadOnly();
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

    public PropertyEditor getValueEditorComponent(ClientFormController form, AsyncChangeInterface asyncChange, Object value) {
        return baseType.getValueEditorComponent(form, this, asyncChange, value);
    }

    public PropertyRenderer getRendererComponent() {
        if (renderer == null) {
            renderer = baseType.getRendererComponent(this);
        }
        return renderer;
    }

    public PanelView getPanelView(ClientFormController form, ClientGroupObjectValue columnKey, LinearCaptionContainer captionContainer) {
        return baseType.getPanelView(this, columnKey, form, captionContainer);
    }

    // padding has to be included for grid column for example, and not for panel property (since flex, width, min-width, etc. doesn't include padding)
    public int getValueWidthWithPadding(JComponent component) {
        return getValueWidth(component) + 1 * 2;
    }
    public int getValueHeightWithPadding(JComponent component) {
        return getValueHeight(component) + 1 * 2;
    }

    public int getValueWidth() {
        return valueWidth;
    }

    public int getValueWidth(JComponent comp) {
        if (valueWidth > -1) {
            return valueWidth;
        }
        FontMetrics fontMetrics = comp.getFontMetrics(design.getFont(comp));

        String widthString = null;
        if(charWidth != 0)
            return baseType.getFullWidthString(BaseUtils.replicate('0', charWidth), fontMetrics, this);

        return baseType.getDefaultWidth(fontMetrics, this);
    }

    public int getValueHeight(JComponent comp) {
        return getValueHeight(comp, null);
    }

    public int getValueHeight(JComponent comp, Integer userFontSize) {
        if (valueHeight > -1) {
            return valueHeight;
        }
        
        Insets insets = SwingDefaults.getTableCellMargins(); // suppose buttons have the same padding. to have equal height
        int insetsHeight = insets.top + insets.bottom;
        int lines = charHeight == 0 ? baseType.getDefaultCharHeight() : charHeight;
        int height;
        int fontSize = userFontSize != null && userFontSize > 0 ? userFontSize : (design.font != null ? design.font.fontSize : -1);
        if (fontSize > 0 || lines > 1) {
            int lineHeight = comp.getFontMetrics(design.getFont(comp)).getHeight();
            height = lineHeight * lines + insetsHeight;
        } else {
            height = SwingDefaults.getValueHeight();
        }
        
        ImageIcon image = ClientImages.getImage(design.getImageHolder());
        if (image != null) // предпочитаемую высоту берем исходя из размера иконки
            height = Math.max(image.getIconHeight() + insetsHeight, height);
        return height;
    }

    public Integer getCaptionWidth() {
        if(captionWidth >= 0)
            return captionWidth;

        return null;
    }

    public Integer getCaptionHeight() {
        if(captionHeight >= 0)
            return captionHeight;

        return null;
    }

    @Override
    public double getFlex() {
        if (flex == -2) {
            return getValueWidth(new JLabel());
        }
        return flex;
    }

    @Override
    public FlexAlignment getAlignment() {
        return alignment;
    }
    
    public Integer getSwingValueAlignment() {
        if (valueAlignment != null) {
            switch (valueAlignment) {
                case CENTER:
                case STRETCH:
                    return SwingConstants.CENTER;
                case END:
                    return SwingConstants.RIGHT;
                case START:
                    return SwingConstants.LEFT;
            }
        }
        return null;
    }

    public Format getFormat() {
        ClientType formatType = this.baseType;
        if(formatType instanceof ClientObjectType)
            formatType = ClientLongClass.instance;

        Format result = format;
        if(formatType instanceof ClientFormatClass) {
            Format defaultFormat = ((ClientFormatClass) formatType).getDefaultFormat();
            if (result == null)
                return defaultFormat;
            if (formatType instanceof ClientIntegralClass) {
                ((NumberFormat) result).setParseIntegerOnly(((NumberFormat) defaultFormat).isParseIntegerOnly());
                ((NumberFormat) result).setMaximumIntegerDigits(((NumberFormat) defaultFormat).getMaximumIntegerDigits());
                ((NumberFormat) result).setGroupingUsed(((NumberFormat) defaultFormat).isGroupingUsed());
            }
        }
        return result;
    }

    public boolean hasMask() {
        return format != null;
    }

    public String getFormatPattern() {
        if (format != null) {
            if (format instanceof DecimalFormat)
                return ((DecimalFormat) format).toPattern();
            else if (format instanceof SimpleDateFormat)
                return ((SimpleDateFormat) format).toPattern();
        }
        return null;
    }

    public void setUserFormat(String pattern) {
        if(baseType instanceof ClientFormatClass) {
            Format setFormat = null;
            if (pattern != null && !pattern.isEmpty())
                setFormat = ((ClientFormatClass) baseType).createUserFormat(pattern);
            else
                setFormat = defaultFormat;

            format = setFormat;
            PropertyRenderer renderer = getRendererComponent();
            if (renderer instanceof FormatPropertyRenderer) {
                ((FormatPropertyRenderer) renderer).updateFormat();
            } else
                assert false;
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public String getCanonicalName() {
        return canonicalName;
    } 
    
    public String getPropertyFormName() {
        return propertyFormName;
    }

    public String getIntegrationSID() {
        return integrationSID;
    }

    public Object parsePaste(String s) {
        if (externalChangeType == null) {
            return null;
        }
        try {
            return externalChangeType.parseString(s);
        } catch (ParseException pe) {
            return null;
        }
    }

    public Object parseBasePaste(String s) throws ParseException {
        return baseType.parseString(s);
    }

    public boolean canUsePasteValueForRendering() {
        return externalChangeType != null && baseType.getTypeClass() == externalChangeType.getTypeClass();
    }

    public boolean canUseChangeValueForRendering() {
        ClientType changeType = getChangeType();
        return changeType != null && baseType.getTypeClass() == changeType.getTypeClass();
    }

    public boolean isPanelCaptionLast() {
        return panelCaptionLast != null ? panelCaptionLast : (baseType instanceof ClientLogicalClass && !panelCaptionVertical && !container.horizontal);
    }

    public FlexAlignment getPanelCaptionAlignment() {
        return (panelCaptionAlignment != null && panelCaptionAlignment != FlexAlignment.STRETCH) ? panelCaptionAlignment : FlexAlignment.CENTER;
    }

    public String formatString(Object obj) throws ParseException {
        if (obj != null) {
            if (format != null) {
                return format.format(obj);
            }
            return baseType.formatString(obj);
        }
        return "";
    }

    public byte getType() {
        return PropertyReadType.DRAW;
    }

    public Integer getInputActionIndex(KeyEvent editEvent) {
        ClientInputList inputList = getInputList();
        if (inputList != null) {
            KeyStroke eventKeyStroke = KeyStroke.getKeyStrokeForEvent(editEvent);
            for (int i = 0; i < inputList.actions.length; i++) {
                if (eventKeyStroke.equals(inputList.actions[i].keyStroke)) {
                    return i;
                }
            }
        }
        return null;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(autoSize);
        outStream.writeBoolean(boxed);

        pool.writeString(outStream, caption);
        pool.writeString(outStream, regexp);
        pool.writeString(outStream, regexpMessage);
        pool.writeLong(outStream, maxValue);
        outStream.writeBoolean(echoSymbols);
        outStream.writeBoolean(noSort);
        defaultCompare.serialize(outStream);

        outStream.writeInt(charHeight);
        outStream.writeInt(charWidth);
        
        outStream.writeInt(valueWidth);
        outStream.writeInt(valueHeight);

        outStream.writeInt(captionWidth);
        outStream.writeInt(captionHeight);

        pool.writeObject(outStream, changeKey);
        pool.writeInt(outStream, changeKeyPriority);
        outStream.writeBoolean(showChangeKey);
        pool.writeObject(outStream, changeMouse);
        pool.writeInt(outStream, changeMousePriority);

        pool.writeObject(outStream, format);
        pool.writeObject(outStream, focusable);

        outStream.writeBoolean(panelCaptionVertical);
        pool.writeObject(outStream, panelCaptionLast);
        pool.writeObject(outStream, panelCaptionAlignment);

        outStream.writeBoolean(panelColumnVertical);

        pool.writeObject(outStream, valueAlignment);
        
        pool.writeObject(outStream, changeOnSingleClick);
        outStream.writeBoolean(hide);

        outStream.writeInt(ID);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        autoSize = inStream.readBoolean();
        boxed = inStream.readBoolean();

        caption = pool.readString(inStream);
        regexp = pool.readString(inStream);
        regexpMessage = pool.readString(inStream);
        maxValue = pool.readLong(inStream);
        echoSymbols = inStream.readBoolean();
        noSort = inStream.readBoolean();
        defaultCompare = Compare.deserialize(inStream);
        charHeight = inStream.readInt();
        charWidth = inStream.readInt();

        valueWidth = inStream.readInt();
        valueHeight = inStream.readInt();

        captionWidth = inStream.readInt();
        captionHeight = inStream.readInt();

        changeKey = pool.readObject(inStream);
        changeKeyPriority = pool.readInt(inStream);
        showChangeKey = inStream.readBoolean();
        changeMouse = pool.readObject(inStream);
        changeMousePriority = pool.readInt(inStream);

        drawAsync = inStream.readBoolean();

        format = pool.readObject(inStream);
        defaultFormat = format;

        isList = inStream.readBoolean();

        focusable = pool.readObject(inStream);
        editType = PropertyEditType.deserialize(inStream.readByte());

        panelCaptionVertical = inStream.readBoolean();
        panelCaptionLast = pool.readObject(inStream);
        panelCaptionAlignment = pool.readObject(inStream);

        panelColumnVertical = inStream.readBoolean();

        valueAlignment = pool.readObject(inStream);

        changeOnSingleClick = pool.readObject(inStream);
        hide = inStream.readBoolean();

        baseType = ClientTypeSerializer.deserializeClientType(inStream);

        if (inStream.readBoolean()) {
            externalChangeType = ClientTypeSerializer.deserializeClientType(inStream);
        }
        asyncExecMap = new HashMap<>();
        int asyncExecSize = inStream.readInt();
        for (int i = 0; i < asyncExecSize; ++i) {
            String key = pool.readString(inStream);
            ClientAsyncEventExec value = ClientAsyncSerializer.deserializeEventExec(inStream);
            asyncExecMap.put(key, value);
        }

        askConfirm = inStream.readBoolean();
        if(askConfirm)
            askConfirmMessage = pool.readString(inStream);
        
        hasEditObjectAction = inStream.readBoolean();
        hasChangeAction = inStream.readBoolean();
        hasDynamicImage = inStream.readBoolean();

        disableInputList = inStream.readBoolean();

        namespace = pool.readString(inStream);
        sID = pool.readString(inStream);
        canonicalName = pool.readString(inStream);
        propertyFormName = pool.readString(inStream);
        integrationSID = pool.readString(inStream);

        toolTip = pool.readString(inStream);

        groupObject = pool.deserializeObject(inStream);

        columnsName = pool.readString(inStream);
        columnGroupObjects = pool.deserializeList(inStream);

        checkEquals = inStream.readBoolean();
        clearText = inStream.readBoolean();
        notSelectAll = inStream.readBoolean();

        // for pivoting
        formula = pool.readString(inStream);
        if(formula != null) {
            int size = inStream.readInt();
            formulaOperands = new ClientPropertyDraw[size];
            for (int i = 0; i < size; i++)
                formulaOperands[i] = pool.deserializeObject(inStream);
        }

        aggrFunc = pool.readString(inStream);
        int size = inStream.readInt();
        for (int i = 0; i < size; i++)
            lastReaders.add(new LastReader(i));
        lastAggrDesc = inStream.readBoolean();

        quickFilterProperty = pool.deserializeObject(inStream);

        tableName = pool.readString(inStream);

        int n = inStream.readInt();
        interfacesCaptions = new String[n];
        interfacesTypes = new ClientClass[n];
        for (int i = 0; i < n; ++i) {
            interfacesCaptions[i] = pool.readString(inStream);
            interfacesTypes[i] = inStream.readBoolean()
                                 ? ClientTypeSerializer.deserializeClientClass(inStream)
                                 : null;
        }

        returnClass = inStream.readBoolean() ? ClientTypeSerializer.deserializeClientClass(inStream) : null;
        
        customRenderFunction = pool.readString(inStream);

        eventID = pool.readString(inStream);

        creationScript = pool.readString(inStream);
        creationPath = pool.readString(inStream);
        path = pool.readString(inStream);
        formPath = pool.readString(inStream);

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
        
        notNull = inStream.readBoolean();

        sticky = inStream.readBoolean();

        hasFooter = inStream.readBoolean();
    }

    public boolean hasColumnGroupObjects() {
        return columnGroupObjects != null && !columnGroupObjects.isEmpty();
    }

    public ClientAsyncEventExec getAsyncEventExec(String actionSID) {
        return asyncExecMap.get(actionSID);
    }

    public ClientType getChangeType() {
        ClientAsyncEventExec asyncExec = asyncExecMap.get(ServerResponse.CHANGE);
        ClientAsyncInput changeType = asyncExec instanceof ClientAsyncInput ? (ClientAsyncInput) asyncExec : null;
        return changeType != null ? changeType.changeType : null;
    }

    public ClientInputList getInputList() {
        ClientAsyncEventExec asyncExec = asyncExecMap.get(ServerResponse.CHANGE);
        ClientAsyncInput changeType = asyncExec instanceof ClientAsyncInput ? (ClientAsyncInput) asyncExec : null;
        return changeType != null ? changeType.inputList : null;
    }

    private void initEditBindingMap() {
        if (editBindingMap == null) {
            editBindingMap = new EditBindingMap(false);
        }
    }

    public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
        controller.updateDrawPropertyValues(this, readKeys, updateKeys);
    }

    @Override
    public String getCaption() {
        return getPropertyCaption();
    }

    public String getChangeCaption(String caption) {
        if (caption == null) {
            caption = getCaptionOrEmpty();
        }

        return showChangeKey && changeKey != null
                ? caption + " (" + getChangeKeyCaption() + ")"
                : caption;
    }

    private String getChangeKeyCaption() {
        return SwingUtils.getKeyStrokeCaption(changeKey.keyStroke);
    }

    public String getChangeCaption() {
        return getChangeCaption(caption);
    }

    public String getPropertyCaption() {
        return getCaptionOrEmpty();
    }

    public String getHTMLCaption() {
        return escapeHTML(getCaptionOrEmpty());
    }

    public String getDynamicCaption(Object captionValue) {
        return BaseUtils.toCaption(captionValue);
    }

    public String getCaptionOrEmpty() {
        return caption == null ? "" : caption;
    }

    @Override
    public String toString() {
        if (!isRedundantString(caption))
            return caption + " (" + getID() + ")";

        return getString("logics.undefined.property");
    }

    public static final String TOOL_TIP_FORMAT =
            "<html><b>%1$s</b><br>" +
                    "%2$s";

    public static final String DETAILED_TOOL_TIP_FORMAT =
            "<hr>" +
                    "<b>" + getString("logics.property.canonical.name") + ":</b> %3$s<br>" +
                    "<b>" + getString("logics.grid") + ":</b> %4$s<br>" +
                    "<b>" + getString("logics.objects") + ":</b> %5$s<br>" +
                    "<b>" + getString("logics.signature") + ":</b> %7$s (%6$s)<br>" +
                    "<b>" + getString("logics.script") + ":</b> %8$s<br>" +
                    "<b>" + getString("logics.scriptpath") + ":</b> %9$s<br>" +
                    "<hr>" + 
                    "<b>" + getString("logics.form.property.name") + ":</b> %10$s<br>" +
                    "<b>" + getString("logics.formpath") + ":</b> %11$s" +
                    "</html>";

    public static final String DETAILED_ACTION_TOOL_TIP_FORMAT =
            "<hr>" +
                    "<b>" + getString("logics.property.canonical.name") + ":</b> %3$s<br>" +
                    "<b>" + getString("logics.objects") + ":</b> %4$s<br>" +
                    "<b>" + getString("logics.scriptpath") + ":</b> %5$s<br>" +
                    "<hr>" + 
                    "<b>" + getString("logics.form.property.name") + ":</b> %6$s<br>" +
                    "<b>" + getString("logics.formpath") + ":</b> %7$s" +
                    "</html>";

    public static final String hotkey = getString("logics.property.hotkey");
    public static final String EDIT_KEY_TOOL_TIP_FORMAT = "<hr><b>" + hotkey + ":</b> %1$s<br>";

    public String getQuickActionTooltipText(KeyStroke keyStroke) {
        return keyStroke == null ? "" : String.format("<html><b>" + hotkey + ":</b> %1$s</html>", keyStroke);
    }
    public String getTooltipText(String caption) {
        String propCaption = nullTrim(!isRedundantString(toolTip) ? toolTip : caption);
        String changeKeyText = changeKey == null ? "" : String.format(EDIT_KEY_TOOL_TIP_FORMAT, getChangeKeyCaption());

        if (!MainController.showDetailedInfo) {
            return String.format(TOOL_TIP_FORMAT, propCaption, changeKeyText);
        } else {
            String ifaceObjects = BaseUtils.toString(", ", interfacesCaptions);
            String scriptPath = creationPath != null ? escapeLineBreakHTML(creationPath) : "";
            String scriptFormPath = formPath != null ? escapeLineBreakHTML(formPath) : "";
            
            if (isAction()) {
                return String.format(TOOL_TIP_FORMAT + DETAILED_ACTION_TOOL_TIP_FORMAT,
                        propCaption, changeKeyText, canonicalName, ifaceObjects, scriptPath, propertyFormName, scriptFormPath);
            } else {
                String tableName = this.tableName != null ? this.tableName : "&lt;none&gt;";
                String ifaceClasses = BaseUtils.toString(", ", interfacesTypes);
                String returnClass = this.returnClass != null ? this.returnClass.toString() : "";
                String script = creationScript != null ? escapeLineBreakHTML(escapeHTML(creationScript)) : "";
                
                return String.format(TOOL_TIP_FORMAT + DETAILED_TOOL_TIP_FORMAT,
                        propCaption, changeKeyText, canonicalName, tableName, ifaceObjects, ifaceClasses, returnClass,
                        script, scriptPath, propertyFormName, scriptFormPath);
            }
        }
    }

    private String escapeHTML(String value) {
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }

    public boolean isAutoDynamicHeight() {
        return getRendererComponent().isAutoDynamicHeight();
    }

    public boolean isAction() {
        return baseType instanceof ClientActionClass;
    }

    public class CaptionReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
            controller.updatePropertyCaptions(ClientPropertyDraw.this, readKeys);
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

        public void update(Map<ClientGroupObjectValue, Object> values, boolean updateKeys, TableController controller) {
            controller.updateShowIfValues(ClientPropertyDraw.this, values);
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

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
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

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
            controller.updateReadOnlyValues(ClientPropertyDraw.this, readKeys);
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.READONLY;
        }
    }

    public class LastReader implements ClientPropertyReader {

        public final int index;

        public LastReader(int index) {
            this.index = index;
        }

        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
//            controller.updateReadOnlyValues(ClientPropertyDraw.this, readKeys);
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.LAST;
        }
    }

    public class BackgroundReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
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

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
            controller.updateCellForegroundValues(ClientPropertyDraw.this, readKeys);
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.CELL_FOREGROUND;
        }
    }

    public class ImageReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
            controller.updateImageValues(ClientPropertyDraw.this, readKeys);
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.IMAGE;
        }
    }
}
