package lsfusion.client.form.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.file.AppImage;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.*;
import lsfusion.client.classes.data.*;
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
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.interop.form.property.PropertyReadType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.*;
import java.util.List;
import java.util.*;

import static lsfusion.base.BaseUtils.*;
import static lsfusion.base.EscapeUtils.escapeLineBreakHTML;
import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.base.SwingUtils.getEventCaption;
import static lsfusion.interop.form.property.PropertyReadType.*;

@SuppressWarnings({"UnusedDeclaration"})
public class ClientPropertyDraw extends ClientComponent implements ClientPropertyReader, ClientIdentitySerializable {

    public CaptionReader captionReader = new CaptionReader();
    public ShowIfReader showIfReader = new ShowIfReader();
    public GridElementClassReader gridElementClassReader = new GridElementClassReader();
    public ValueElementClassReader valueElementClassReader = new ValueElementClassReader();
    public CaptionElementClassReader captionElementClassReader = new CaptionElementClassReader();
    public ExtraPropReader fontReader = new ExtraPropReader(CELL_FONT);
    public BackgroundReader backgroundReader = new BackgroundReader();
    public ForegroundReader foregroundReader = new ForegroundReader();
    public FooterReader footerReader = new FooterReader();
    public ReadOnlyReader readOnlyReader = new ReadOnlyReader();
    public ImageReader imageReader = new ImageReader();
    public boolean hasDynamicImage;
    public boolean hasDynamicCaption;

    public ExtraPropReader commentReader = new ExtraPropReader(COMMENT);
    public ExtraPropReader commentElementClassReader = new ExtraPropReader(COMMENTELEMENTCLASS);
    public ExtraPropReader placeholderReader = new ExtraPropReader(PLACEHOLDER);
    public ExtraPropReader patternReader = new ExtraPropReader(PATTERN);
    public ExtraPropReader regexpReader = new ExtraPropReader(REGEXP);
    public ExtraPropReader regexpMessageReader = new ExtraPropReader(REGEXPMESSAGE);
    public ExtraPropReader tooltipReader = new ExtraPropReader(TOOLTIP);
    public ExtraPropReader valueTooltipReader = new ExtraPropReader(VALUETOOLTIP);
    public ExtraPropReader propertyCustomOptionsReader = new ExtraPropReader(PROPERTY_CUSTOM_OPTIONS);
    public ExtraPropReader changeKeyReader = new ExtraPropReader(CHANGEKEY);
    public ExtraPropReader changeMouseReader = new ExtraPropReader(CHANGEMOUSE);

    public boolean boxed;

    public boolean isAutoSize() {
        return valueHeight == -1 || valueWidth == -1;
    }

    // for pivoting
    public String formula;
    public ClientPropertyDraw[] formulaOperands;

    public String aggrFunc;
    public List<LastReader> lastReaders = new ArrayList<>();
    public boolean lastAggrDesc;

    public ClientPropertyDraw quickFilterProperty;

    public Boolean inline;

    public boolean isList;

    public ClientType baseType; // cellType
    public ClientType valueType;
    public ClientClass returnClass;

    public String tag;
    public String inputType;
    public String valueElementClass;
    public String captionElementClass;
    public boolean toolbar;
    public boolean toolbarActions;

    public boolean ignoreHasHeaders;

    public ClientType externalChangeType;
    public Map<String, ClientAsyncEventExec> asyncExecMap;
    public boolean askConfirm;
    public String askConfirmMessage;

    public boolean hasEditObjectAction;
    public boolean hasChangeAction;
    public boolean hasUserChangeAction;

    public boolean disableInputList;

    public String[] interfacesCaptions;
    public ClientClass[] interfacesTypes;

    public String caption;
    public AppImage image;
    public Long maxValue;
    public boolean echoSymbols;
    public boolean noSort;
    public Compare defaultCompare;

    public InputBindingEvent changeKey;
    public boolean showChangeKey;
    public InputBindingEvent changeMouse;
    public boolean showChangeMouse;

    public boolean drawAsync; // рисовать асинхронность на этой кнопке

    public Boolean focusable;
    public PropertyEditType editType = PropertyEditType.EDITABLE;

    public boolean panelColumnVertical;
    public boolean panelCustom;

    public FlexAlignment valueAlignmentHorz;
    public FlexAlignment valueAlignmentVert;

    public boolean highlightDuplicateValue;

    public String valueOverflowHorz;
    public String valueOverflowVert;

    public boolean valueShrinkHorz;
    public boolean valueShrinkVert;

    public String comment;
    public String commentElementClass;
    public boolean panelCommentVertical;
    public boolean panelCommentFirst;
    public FlexAlignment panelCommentAlignment;

    public String placeholder;

    public String pattern;
    public String userPattern;

    public String regexp;
    public String regexpMessage;

    public String tooltip;
    public String valueTooltip;

    public int charWidth;
    public int charHeight;

    public int valueWidth;
    public int valueHeight;

    public int captionWidth;
    public int captionHeight;
    public int captionCharHeight;

    public transient EditBindingMap editBindingMap;
    private transient PropertyRenderer renderer;

    public boolean checkEquals;

    protected String namespace;
    protected String canonicalName;
    protected String propertyFormName; // PropertyDrawEntity.sID
    protected String integrationSID;

    public ClientGroupObject groupObject;
    public String columnsName;
    public List<ClientGroupObject> columnGroupObjects = new ArrayList<>();

    public boolean wrap;
    public boolean wrapWordBreak;
    public boolean collapse;
    public boolean ellipsis;

    public boolean captionWrap;
    public boolean captionWrapWordBreak;
    public boolean captionCollapse;
    public boolean captionEllipsis;

    public boolean clearText;
    public boolean notSelectAll;
    public String tableName;
    public String eventID;
    public Boolean changeOnSingleClick;
    public boolean hide;
    public boolean remove;

    public String customRenderFunction;
    public boolean customCanBeRenderedInTD;
    public boolean customNeedPlaceholder;
    public boolean customNeedReadonly;

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
        return editType == PropertyEditType.READONLY || editType == PropertyEditType.DISABLE;
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
        if(charWidth != -1)
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
        int lines = charHeight == -1 ? baseType.getDefaultCharHeight() : charHeight;
        int height;
        int fontSize = userFontSize != null && userFontSize > 0 ? userFontSize : (design.font != null ? design.font.fontSize : -1);
        if (fontSize > 0 || lines > 1) {
            int lineHeight = comp.getFontMetrics(design.getFont(comp)).getHeight();
            height = lineHeight * lines + insetsHeight;
        } else {
            height = SwingDefaults.getValueHeight();
        }
        
        ImageIcon image = ClientImages.getImage(this.image);
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
    
    public Integer getSwingValueAlignmentHorz() {
        if (valueAlignmentHorz != null) {
            switch (valueAlignmentHorz) {
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

        Format result = getPatternFormat();
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

    public void setUserPattern(String userPattern) {
        if(baseType instanceof ClientFormatClass) {
            this.userPattern = userPattern;
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
        ClientType externalChangeType = this.externalChangeType;
        if(externalChangeType == null)
            externalChangeType = baseType;
        return parsePaste(s, externalChangeType);
    }
    public Object parsePaste(String s, ClientType parseType) {
        if(s == null)
            return null;
        if (parseType == null) {
            return null;
        }
        try {
            return parseType.parseString(s);
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

    public boolean isCaptionLast() {
        return captionLast;
    }

    public FlexAlignment getCaptionAlignmentHorz() {
        return captionAlignmentHorz;
    }

    public boolean isPanelCommentFirst() {
        return panelCommentFirst;
    }

    public FlexAlignment getPanelCommentAlignment() {
        return panelCommentAlignment;
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

    private Format format;
    private String prevPattern;
    private Format getPatternFormat() {
        String curPattern = nvl(userPattern, pattern);
        if(curPattern != null) {
            if (baseType instanceof ClientFormatClass && (prevPattern == null || !prevPattern.equals(curPattern))) {
                prevPattern = curPattern;
                format = ((ClientFormatClass) baseType).createUserFormat(curPattern);
            }
        } else {
            format = null;
        }
        return format;
    }

    public byte getType() {
        return PropertyReadType.DRAW;
    }

    public Integer getInputActionIndex(KeyEvent editEvent) {
        ClientInputListAction[] inputListActions = getInputListActions();
        if (inputListActions != null) {
            KeyStroke eventKeyStroke = KeyStroke.getKeyStrokeForEvent(editEvent);
            for (int i = 0; i < inputListActions.length; i++) {
                ClientInputListAction action = inputListActions[i];
                if (eventKeyStroke.equals(action.keyStroke)) {
                    return action.index;
                }
            }
        }
        return null;
    }

    public Integer getDialogInputActionIndex() {
        ClientInputListAction[] inputListActions = getInputListActions();
        if (inputListActions != null) {
            return getDialogInputActionIndex(inputListActions);
        }
        return null;
    }

    public Integer getDialogInputActionIndex(ClientInputListAction[] actions) {
        if (actions != null && disableInputList) {
            for (int i = 0; i < actions.length; i++) {
                ClientInputListAction action = actions[i];
                //addDialogInputAProp from server
                if (action.id != null && action.id.equals(AppImage.INPUT_DIALOG)) {
                    return action.index;
                }
            }
        }
        return null;
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        caption = pool.readString(inStream);
        image = pool.readImageIcon(inStream);

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
        captionCharHeight = inStream.readInt();

        changeKey = pool.readObject(inStream);
        showChangeKey = inStream.readBoolean();
        changeMouse = pool.readObject(inStream);
        showChangeMouse = inStream.readBoolean();

        drawAsync = inStream.readBoolean();

        inline = pool.readObject(inStream);
        isList = inStream.readBoolean();

        focusable = pool.readObject(inStream);
        editType = PropertyEditType.deserialize(inStream.readByte());

        panelCustom = inStream.readBoolean();
        panelColumnVertical = inStream.readBoolean();

        valueAlignmentHorz = pool.readObject(inStream);
        valueAlignmentVert = pool.readObject(inStream);

        highlightDuplicateValue = pool.readBoolean(inStream);

        valueOverflowHorz = pool.readString(inStream);
        valueOverflowVert = pool.readString(inStream);

        valueShrinkHorz = pool.readBoolean(inStream);
        valueShrinkVert = pool.readBoolean(inStream);

        comment = pool.readString(inStream);
        commentElementClass = pool.readString(inStream);
        panelCommentVertical = inStream.readBoolean();
        panelCommentFirst = inStream.readBoolean();
        panelCommentAlignment = pool.readObject(inStream);

        placeholder = pool.readString(inStream);
        pattern = pool.readString(inStream);
        regexp = pool.readString(inStream);
        regexpMessage = pool.readString(inStream);

        tooltip = pool.readString(inStream);
        valueTooltip = pool.readString(inStream);

        changeOnSingleClick = pool.readObject(inStream);
        hide = inStream.readBoolean();
        remove = inStream.readBoolean();

        baseType = ClientTypeSerializer.deserializeClientType(inStream);
        if(inStream.readBoolean())
            valueType = ClientTypeSerializer.deserializeClientType(inStream);

        tag = pool.readString(inStream);
        inputType = pool.readString(inStream);
        valueElementClass = pool.readString(inStream);
        captionElementClass = pool.readString(inStream);
        toolbar = pool.readBoolean(inStream);
        toolbarActions = pool.readBoolean(inStream);

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

        ignoreHasHeaders = inStream.readBoolean();

        askConfirm = inStream.readBoolean();
        if(askConfirm)
            askConfirmMessage = pool.readString(inStream);
        
        hasEditObjectAction = inStream.readBoolean();
        hasChangeAction = inStream.readBoolean();
        hasUserChangeAction = inStream.readBoolean();
        hasDynamicImage = inStream.readBoolean();
        hasDynamicCaption = inStream.readBoolean();

        disableInputList = inStream.readBoolean();

        namespace = pool.readString(inStream);
        sID = pool.readString(inStream);
        canonicalName = pool.readString(inStream);
        propertyFormName = pool.readString(inStream);
        integrationSID = pool.readString(inStream);

        groupObject = pool.deserializeObject(inStream);

        columnsName = pool.readString(inStream);
        columnGroupObjects = pool.deserializeList(inStream);

        checkEquals = inStream.readBoolean();

        wrap = inStream.readBoolean();
        wrapWordBreak = inStream.readBoolean();
        collapse = inStream.readBoolean();
        ellipsis = inStream.readBoolean();

        captionWrap = inStream.readBoolean();
        captionWrapWordBreak = inStream.readBoolean();
        captionCollapse = inStream.readBoolean();
        captionEllipsis = inStream.readBoolean();

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
        customCanBeRenderedInTD = pool.readBoolean(inStream);
        customNeedPlaceholder = pool.readBoolean(inStream);
        customNeedReadonly = pool.readBoolean(inStream);

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

    public ClientInputListAction[] getInputListActions() {
        ClientAsyncEventExec asyncExec = asyncExecMap.get(ServerResponse.CHANGE);
        ClientAsyncInput changeType = asyncExec instanceof ClientAsyncInput ? (ClientAsyncInput) asyncExec : null;
        return changeType != null ? changeType.inputListActions : null;
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

        String eventCaption = getEventCaption(changeKey, showChangeKey, changeMouse, showChangeMouse);
        return caption + (eventCaption != null ? " (" + eventCaption + ")" : "");
    }

    private String getChangeKeyCaption() {
        return SwingUtils.getKeyStrokeCaption(((KeyInputEvent) changeKey.inputEvent).keyStroke);
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
        String propCaption = nullTrim(!isRedundantString(tooltip) ? tooltip : caption);

        String eventCaption = getEventCaption(changeKey, showChangeKey, changeMouse, showChangeMouse);
        String bindingText = eventCaption != null ? String.format(EDIT_KEY_TOOL_TIP_FORMAT, eventCaption) : "";

        if (!MainController.showDetailedInfo) {
            return propCaption.isEmpty() ? null : String.format(TOOL_TIP_FORMAT, propCaption, bindingText);
        } else {
            String ifaceObjects = BaseUtils.toString(", ", interfacesCaptions);
            String scriptPath = creationPath != null ? escapeLineBreakHTML(creationPath) : "";
            String scriptFormPath = formPath != null ? escapeLineBreakHTML(formPath.substring(formPath.lastIndexOf("/") + 1).replace(".lsf", "")) : "";
            
            if (isAction()) {
                return String.format(TOOL_TIP_FORMAT + DETAILED_ACTION_TOOL_TIP_FORMAT,
                        propCaption, bindingText, canonicalName, ifaceObjects, scriptPath, propertyFormName, scriptFormPath);
            } else {
                String tableName = this.tableName != null ? this.tableName : "&lt;none&gt;";
                String ifaceClasses = BaseUtils.toString(", ", interfacesTypes);
                String returnClass = this.returnClass != null ? this.returnClass.toString() : "";
                String script = creationScript != null ? escapeLineBreakHTML(escapeHTML(creationScript)) : "";
                
                return String.format(TOOL_TIP_FORMAT + DETAILED_TOOL_TIP_FORMAT,
                        propCaption, bindingText, canonicalName, tableName, ifaceObjects, ifaceClasses, returnClass,
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

    public boolean hideOrRemove() {
        return hide || remove;
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

    public class GridElementClassReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.CELL_GRIDELEMENTCLASS;
        }
    }

    public class ValueElementClassReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.CELL_VALUEELEMENTCLASS;
        }
    }

    public class CaptionElementClassReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return PropertyReadType.CAPTIONELEMENTCLASS;
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

    public class ExtraPropReader implements ClientPropertyReader {
        final byte type;

        public ExtraPropReader(byte type) {
            this.type = type;
        }

        public ClientGroupObject getGroupObject() {
            return ClientPropertyDraw.this.getGroupObject();
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
        }

        public int getID() {
            return ClientPropertyDraw.this.getID();
        }

        public byte getType() {
            return type;
        }
    }
}
