package lsfusion.gwt.client.form.property;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.base.ImageHolder;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.classes.GClass;
import lsfusion.gwt.client.classes.GObjectType;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.classes.data.GLongType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.async.GAsyncChange;
import lsfusion.gwt.client.form.property.async.GAsyncEventExec;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.view.FormatCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CustomCellRenderer;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;
import lsfusion.interop.action.ServerResponse;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static lsfusion.gwt.client.base.EscapeUtils.escapeLineBreakHTML;
import static lsfusion.gwt.client.base.GwtClientUtils.createTooltipHorizontalSeparator;

public class GPropertyDraw extends GComponent implements GPropertyReader, Serializable {
    public int ID;
    public String nativeSID;
    public String sID;
    public String namespace;
    public String caption;
    public String canonicalName;
    public String propertyFormName;
    public String integrationSID;
    
    public String customRenderFunction;
    public String customChangeFunction;

    public String toolTip;
    public boolean clearText;
    public String tableName;
    public String[] interfacesCaptions;
    public GClass[] interfacesTypes;
    public String creationScript;
    public String creationPath;
    public String path;
    public String formPath;

    public GGroupObject groupObject;
    public String columnsName;
    public ArrayList<GGroupObject> columnGroupObjects;

    public boolean isList;

    public GType baseType;
    public String pattern;
    public String defaultPattern;
    public GClass returnClass;

    public GType externalChangeType;
    public Map<String, GAsyncEventExec> asyncExecMap;

    // custom renderers, simple state views, paste
    public static final GType externalChangeTypeUsage = null;
    public GType getExternalChangeType() {
        return externalChangeType;
    }

    public boolean hasColumnGroupObjects() {
        return columnGroupObjects != null && !columnGroupObjects.isEmpty();
    }

    public GType getChangeType() {
        GAsyncEventExec asyncExec = getAsyncEventExec(ServerResponse.CHANGE);
        return asyncExec instanceof GAsyncChange ? ((GAsyncChange) asyncExec).changeType : null;
    }

    public GAsyncEventExec getAsyncEventExec(String actionSID) {
        return asyncExecMap.get(actionSID);
    }

    public boolean askConfirm;
    public String askConfirmMessage;

    public boolean hasEditObjectAction;
    public boolean hasChangeAction;

    public GEditBindingMap editBindingMap;

    public ImageHolder imageHolder;
    public boolean hasDynamicImage;
    public Boolean focusable;
    public boolean checkEquals;
    public GPropertyEditType editType = GPropertyEditType.EDITABLE;

    public boolean echoSymbols;
    public boolean noSort;
    public GCompare defaultCompare;

    public GCompare getDefaultCompare() {
        return defaultCompare != null ? defaultCompare : baseType.getDefaultCompare();
    }

    public GCompare[] getFilterCompares() {
        return baseType.getFilterCompares();
    }

    public boolean hasStaticImage() {
        return imageHolder != null;
    }
    public boolean hasDynamicImage() { // when it's an action and has dynamic image
        return isAction() && hasDynamicImage;
    }

    public ArrayList<GInputBindingEvent> bindingEvents = new ArrayList<>();
    public boolean showChangeKey;

    public boolean hasKeyBinding() {
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GKeyInputEvent)
                return true;
        return false;
    }
    public String getKeyBindingText() {
        assert hasKeyBinding();
        String result = "";
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GKeyInputEvent) {
                result = (result.isEmpty() ? "" : result + ",") + ((GKeyInputEvent) bindingEvent.inputEvent).keyStroke;
            }
        return result;
    }

    public boolean drawAsync;

    public GCaptionReader captionReader;
    public GShowIfReader showIfReader;
    public GFooterReader footerReader;
    public GReadOnlyReader readOnlyReader;
    public GBackgroundReader backgroundReader;
    public GForegroundReader foregroundReader;
    public GImageReader imageReader;

    // for pivoting
    public String formula;
    public ArrayList<GPropertyDraw> formulaOperands;

    public String aggrFunc;
    public ArrayList<GLastReader> lastReaders;
    public boolean lastAggrDesc;

    public GPropertyDraw quickFilterProperty;

    public int charWidth;
    public int charHeight;

    public int valueWidth = -1;
    public int valueHeight = -1;

    public boolean panelCaptionVertical;
    public Boolean panelCaptionLast;
    public GFlexAlignment panelCaptionAlignment;

    public boolean panelColumnVertical;
    
    public GFlexAlignment valueAlignment;

    public Boolean changeOnSingleClick;

    public boolean hide;

    private transient CellRenderer cellRenderer;
    
    public boolean notNull;

    // eventually gets to PropertyDrawEntity.getEventAction (which is symmetrical to this)
    public String getEventSID(Event editEvent) {
        String actionSID = null;
        if (editBindingMap != null) { // property bindings
            actionSID = editBindingMap.getEventSID(editEvent);
        }
        // not sure that it should be done like this since enter is used as a tab
//        if (actionSID == null && baseType instanceof GActionType && GKeyStroke.isEnterKeyEvent(editEvent)) {
//            return GEditBindingMap.CHANGE;
//        }
        if (actionSID == null) {
            GType changeType = getChangeType();
            actionSID = GEditBindingMap.getDefaultEventSID(editEvent, changeType == null ? null : changeType.getEditEventFilter(), hasEditObjectAction, hasChangeAction);
        }
        return actionSID;
    }
    public boolean isFilterChange(Event editEvent) {
        return GEditBindingMap.isDefaultFilterChange(editEvent, baseType.getEditEventFilter());
    }

    public GPropertyDraw(){}

    public PanelRenderer createPanelRenderer(GFormController form, ActionOrPropertyValueController controller, GGroupObjectValue columnKey, LinearCaptionContainer captionContainer) {
        return baseType.createPanelRenderer(form, controller, this, columnKey, captionContainer);
    }

    public boolean isAction() {
        return baseType instanceof GActionType;
    }

    public CellRenderer getCellRenderer() {
        if (cellRenderer == null) {
            if (customRenderFunction != null) {
                cellRenderer = new CustomCellRenderer(this, customRenderFunction);
            } else {
                cellRenderer = baseType.createGridCellRenderer(this);
            }
        }
        return cellRenderer;
    }

    public void setUserPattern(String pattern) {
        if(baseType instanceof GFormatType) {
            this.pattern = pattern != null ? pattern : defaultPattern;

            CellRenderer renderer = getCellRenderer();
            if (renderer instanceof FormatCellRenderer) {
                ((FormatCellRenderer) renderer).updateFormat();
            } else
                assert renderer instanceof CustomCellRenderer;
        }
    }

    public Object parsePaste(String s, GType parseType) {
        if (s == null) {
            return null;
        }
        if(parseType == null)
            return null;
        try {
            return parseType.parseString(s, pattern);
        } catch (ParseException pe) {
            return null;
        }
    }

    public boolean canUseChangeValueForRendering(GType type) {
        return type != null && baseType.getClass() == type.getClass();
    }

    public String getCaptionOrEmpty() {
        return caption == null ? "" : caption;
    }

    public String getDynamicCaption(Object caption) {
        return caption == null ? "" : caption.toString().trim();
    }

    public String getEditCaption(String caption) {
        if (caption == null) {
            caption = this.caption;
        }

        if(showChangeKey && hasKeyBinding())
            caption += " (" + getKeyBindingText() + ")";
        return caption;
    }

    public String getEditCaption() {
        return getEditCaption(caption);
    }

    public String getNotEmptyCaption() {
        if (caption == null || caption.trim().length() == 0) {
            return getMessages().propertyEmptyCaption();
        } else {
            return caption;
        }
    }

    public String getFilterCaption() {
        return getNotEmptyCaption() + " (" + (groupObject != null ? groupObject.getCaption() : "") + ")";
    }

    private static ClientMessages getMessages() {
        return ClientMessages.Instance.get();
    }
    
    public static final String TOOL_TIP_FORMAT =
            "<html><b>%s</b><br>%s";

    public static String getDetailedToolTipFormat() {
        return  createTooltipHorizontalSeparator() +
                "<b>" + getMessages().propertyTooltipCanonicalName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipTable() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipObjects() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipSignature() + ":</b> %s (%s)<br>" +
                "<b>" + getMessages().propertyTooltipScript() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipPath() + ":</b> %s<br>" +
                createTooltipHorizontalSeparator() +
                "<b>" + getMessages().propertyTooltipFormPropertyName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipFormPropertyDeclaration() + ":</b> %s" +
                "</html>";
    }  
    
    public static String getDetailedActionToolTipFormat() {
        return  createTooltipHorizontalSeparator() +
                "<b>sID:</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipObjects() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipPath() + ":</b> %s<br>" +
                createTooltipHorizontalSeparator() +
                "<b>" + getMessages().propertyTooltipFormPropertyName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipFormPropertyDeclaration() + ":</b> %s" +
                "</html>";
    }
    
    public static String getChangeKeyToolTipFormat() {
        return createTooltipHorizontalSeparator() + "<b>" + getMessages().propertyTooltipHotkey() + ":</b> %s<br>";
    }
            
    public String getTooltipText(String caption) {
        String propCaption = GwtSharedUtils.nullTrim(!GwtSharedUtils.isRedundantString(toolTip) ? toolTip : caption);
        String keyBindingText = hasKeyBinding() ? GwtSharedUtils.stringFormat(getChangeKeyToolTipFormat(), getKeyBindingText()) : "";

        if (!MainFrame.showDetailedInfo) {
            return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT, propCaption, keyBindingText);
        } else {
            String ifaceObjects = GwtSharedUtils.toString(", ", interfacesCaptions);
            String scriptPath = creationPath != null ? escapeLineBreakHTML(creationPath) : "";
            String scriptFormPath = formPath != null ? escapeLineBreakHTML(formPath) : "";
            
            if (isAction()) {
                return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + getDetailedActionToolTipFormat(),
                        propCaption, keyBindingText, canonicalName, ifaceObjects, scriptPath, propertyFormName, scriptFormPath);
            } else {
                String tableName = this.tableName != null ? this.tableName : "&lt;none&gt;";
                String returnClass = this.returnClass.toString();
                String ifaceClasses = GwtSharedUtils.toString(", ", interfacesTypes);
                String script = creationScript != null ? escapeLineBreakHTML(escapeHTML(creationScript)) : "";
                
                return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + getDetailedToolTipFormat(),
                        propCaption, keyBindingText, canonicalName, tableName, ifaceObjects, returnClass, ifaceClasses,
                        script, scriptPath, propertyFormName, scriptFormPath);
            }
        }
    }

    private String escapeHTML(String value) {
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }

    public ImageDescription getImage() {
        return getImage(true);
    }

    public ImageDescription getImage(boolean enabled) {
        ImageDescription image = imageHolder != null ? imageHolder.getImage() : null;
        if (!enabled && image != null && image.url != null) {
            int dotInd = image.url.lastIndexOf(".");
            if (dotInd != -1) {
                return new ImageDescription(image.url.substring(0, dotInd) + "_Disabled" + image.url.substring(dotInd), image.width, image.height);
            }
        }
        return image;
    }

    @Override
    public String getNativeSID() {
        return nativeSID;
    }

    public boolean isReadOnly() {
        return editType == GPropertyEditType.READONLY;
    }

    public boolean isEditableNotNull() {
        return notNull && !isReadOnly();
    }

    public double getFlex() {
        if (flex == -2) {
            return getValueWidth(null);
        }
        return flex;
    }

    public boolean isPanelCaptionLast() {
        return panelCaptionLast != null ? panelCaptionLast : (baseType instanceof GLogicalType && !panelCaptionVertical && !container.horizontal);
    }

    public GFlexAlignment getPanelCaptionAlignment() {
        return (panelCaptionAlignment != null && panelCaptionAlignment != GFlexAlignment.STRETCH) ? panelCaptionAlignment : GFlexAlignment.CENTER;
    }

    public GFlexAlignment getAlignment() {
        return alignment;
    }
    
    public Style.TextAlign getTextAlignStyle() {
        if (valueAlignment != null) {
            switch (valueAlignment) {
                case START:
                    return Style.TextAlign.LEFT;
                case CENTER:
                case STRETCH:
                    return Style.TextAlign.CENTER;
                case END:
                    return Style.TextAlign.RIGHT;
            }
        }
        return null;
    }

    public static ArrayList<GGroupObjectValue> getColumnKeys(GPropertyDraw property, NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> currentGridObjects) {
        ArrayList<GGroupObjectValue> columnKeys = GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
        if (property.columnGroupObjects != null) {
            LinkedHashMap<GGroupObject, ArrayList<GGroupObjectValue>> groupColumnKeys = new LinkedHashMap<>();
            for (GGroupObject columnGroupObject : property.columnGroupObjects) {
                ArrayList<GGroupObjectValue> columnGroupKeys = currentGridObjects.get(columnGroupObject);
                if (columnGroupKeys != null) {
                    groupColumnKeys.put(columnGroupObject, columnGroupKeys);
                }
            }

            columnKeys = GGroupObject.mergeGroupValues(groupColumnKeys);
        }
        return columnKeys;
    }

    @Override
    public void update(GFormController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.getPropertyController(this).updateProperty(this, getColumnKeys(this, controller.getCurrentGridObjects()), updateKeys, values);
    }

    // padding has to be included for grid column for example, and not for panel property (since flex, width, min-width, etc. doesn't include padding)
    public int getValueWidthWithPadding(GFont parentFont) {
        return getValueWidth(parentFont) + getCellRenderer().getWidthPadding() * 2;
    }
    public int getValueHeightWithPadding(GFont parentFont) {
        return getValueHeight(parentFont) + getCellRenderer().getHeightPadding() * 2;
    }
    public boolean isAutoDynamicHeight() {
        return getCellRenderer().isAutoDynamicHeight();
    }

    public int getValueWidth(GFont parentFont) {
        if (valueWidth != -1) {
            return valueWidth;
        }

        GFont font = this.font != null ? this.font : parentFont;

        String widthString = null;
        if(widthString == null && charWidth != 0)
            widthString = GwtSharedUtils.replicate('0', charWidth);
        if(widthString != null)
            return baseType.getFullWidthString(widthString, font);

        return baseType.getDefaultWidth(font, this);
    }

    public Object getFormat() {
        return (baseType instanceof GObjectType ? GLongType.instance : ((GFormatType)baseType)).getFormat(pattern);
    }

    public int getValueHeight(GFont parentFont) {
        if (valueHeight != -1) {
            return valueHeight;
        }

        int lineHeight;
        GFont usedFont = font != null ? font : parentFont;
        if (usedFont != null && usedFont.size > 0)
            lineHeight = GFontMetrics.getSymbolHeight(usedFont);
        else
            lineHeight = StyleDefaults.VALUE_HEIGHT;
        int lines = charHeight > 0 ? charHeight : baseType.getDefaultCharHeight();

        int height = lineHeight * lines;

        final ImageDescription image = getImage();
        if (image != null && image.height >= 0) {
            height = Math.max(image.height, height);
        }
        return height;
    }

    public LinkedHashMap<String, String> getContextMenuItems() {
        return editBindingMap == null ? null : editBindingMap.getContextMenuItems();
    }

    public boolean isFocusable() {
        return focusable == null || focusable;
    }

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public String toString() {
        return sID + " " + caption;
    }

    @Override
    public boolean isAlignCaption() {
        return !hasColumnGroupObjects() && !isAction() && !panelCaptionVertical;
    }
}
