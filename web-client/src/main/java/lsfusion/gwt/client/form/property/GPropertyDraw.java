package lsfusion.gwt.client.form.property;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.classes.GClass;
import lsfusion.gwt.client.classes.GObjectType;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.*;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.async.*;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.view.SimpleTextBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CustomCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.interop.action.ServerResponse;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;
import static lsfusion.gwt.client.base.EscapeUtils.escapeLineBreakHTML;
import static lsfusion.gwt.client.base.GwtClientUtils.createTooltipHorizontalSeparator;
import static lsfusion.gwt.client.form.event.GKeyStroke.*;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.CHANGE;

public class GPropertyDraw extends GComponent implements GPropertyReader, Serializable {
    public int ID;
    public String nativeSID;
    public String sID;
    public String namespace;

    public String caption;
    public String captionElementClass;
    public AppStaticImage appImage;

    public String canonicalName;
    public String propertyFormName;
    public String integrationSID;
    
    public String customRenderFunction;
    public boolean customCanBeRenderedInTD;
    public boolean customNeedPlaceholder;
    public boolean customNeedReadonly;

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

    public GType getCellType() {
        return cellType;
    }

    public GType getValueType() {
        return valueType;
    }

    public GType getRenderType(RendererType type) {
        if(type == RendererType.CELL)
            return getCellType();
        else
            return getValueType();
    }
    public GType getPasteType() {
        return getValueType();
    }
    public GType getEventType() {
        return getCellType();
    }

    public GType cellType;
    public GType valueType;
    public boolean differentValue;
    public String pattern;
    public String defaultPattern;
    public GClass returnClass;

    public String tag;
    public String valueElementClass;
    public boolean toolbar;

    public GType externalChangeType;
    public Map<String, GAsyncEventExec> asyncExecMap;

    public GType getExternalChangeType() {
        return externalChangeType;
    }

    public boolean hasColumnGroupObjects() {
        return columnGroupObjects != null && !columnGroupObjects.isEmpty();
    }

    public GType getFilterBaseType() {
        GType filterType = getRenderType(RendererType.FILTER);
        return getDefaultCompare().escapeSeparator() ? filterType.getFilterMatchType() : filterType;
    }

    public GType getChangeType() {
        GAsyncEventExec asyncExec = getAsyncEventExec(ServerResponse.CHANGE);
        return asyncExec instanceof GAsyncInput ? ((GAsyncInput) asyncExec).changeType : null;
    }

    public GInputList getInputList() {
        GAsyncEventExec asyncExec = getAsyncEventExec(ServerResponse.CHANGE);
        return asyncExec instanceof GAsyncInput ? ((GAsyncInput) asyncExec).inputList : null;
    }

    public GGroupObjectValue filterColumnKeys(GGroupObjectValue fullCurrentKey) {
        return fullCurrentKey.filter(columnGroupObjects != null ? columnGroupObjects : Collections.emptyList());
    }

    public Boolean loadingReplaceImage;
    public boolean isLoadingReplaceImage() {
        if(loadingReplaceImage != null)
            return loadingReplaceImage;

        return !hasDynamicImage(); // move to server afterwards
    }

    public static class QuickAccessAction implements CellRenderer.ToolbarAction {
        public final BaseStaticImage action;
        public final GKeyStroke keyStroke;
        public final int index;
        public final boolean hover;

        @Override
        public boolean isHover() {
            return hover;
        }

        @Override
        public GKeyStroke getKeyStroke() {
            return keyStroke;
        }

        @Override
        public BaseStaticImage getImage() {
            return action;
        }

        @Override
        public void setOnPressed(Element actionImgElement, UpdateContext updateContext) {
            setToolbarAction(actionImgElement, index);
//            setToolbarAction(actionImgElement, () -> updateContext.executeContextAction(index));
        }

        @Override
        public boolean matches(CellRenderer.ToolbarAction action) {
            if(!(action instanceof QuickAccessAction))
                return false;

            return isHover() == action.isHover() && index == ((QuickAccessAction) action).index;
        }

        public QuickAccessAction(BaseStaticImage action, GKeyStroke keyStroke, int index, boolean hover) {
            this.action = action;
            this.keyStroke = keyStroke;
            this.index = index;
            this.hover = hover;
        }
    }

    private transient QuickAccessAction[] quickAccessSelectedFocusedActions;
    private transient QuickAccessAction[] quickAccessSelectedActions;
    private transient QuickAccessAction[] quickAccessActions;
    // manual caching
    public QuickAccessAction[] getQuickAccessActions(boolean isSelected, boolean isFocused) {
        if(isSelected) {
            if(isFocused) {
                if (quickAccessSelectedFocusedActions == null) {
                    quickAccessSelectedFocusedActions = calculateQuickAccessActions(true, true);
                }
                return quickAccessSelectedFocusedActions;
            }
            if (quickAccessSelectedActions == null) {
                quickAccessSelectedActions = calculateQuickAccessActions(true, false);
            }
            return quickAccessSelectedActions;
        }

        if(quickAccessActions == null) {
            quickAccessActions = calculateQuickAccessActions(false, false);
        }
        return quickAccessActions;
    }

    private QuickAccessAction[] calculateQuickAccessActions(boolean isSelected, boolean isFocused) {
        GInputList inputList = getInputList();

        List<QuickAccessAction> actions = new ArrayList<>();
        if(inputList != null) {
            for (int i = 0; i < inputList.actions.length; i++) {
                List<GQuickAccess> quickAccessList = inputList.actions[i].quickAccessList;

                boolean enable = false;
                boolean hover = true;
                for (GQuickAccess quickAccess : quickAccessList) {
                    switch (quickAccess.mode) {
                        case ALL:
                            enable = true;
                            if (!quickAccess.hover) {
                                hover = false;
                            }
                            break;
                        case SELECTED:
                            if (isSelected) {
                                enable = true;
                                if (!quickAccess.hover) {
                                    hover = false;
                                }
                            }
                            break;
                        case FOCUSED:
                            if (isSelected && isFocused) {
                                enable = true;
                                if (!quickAccess.hover) {
                                    hover = false;
                                }
                            }
                    }
                }

                GInputListAction action = inputList.actions[i];
                if (enable) {
                    actions.add(new QuickAccessAction(action.action, action.keyStroke, action.index, hover));
                }
            }
        }
        return actions.toArray(new QuickAccessAction[0]);
    }

    public GAsyncEventExec getAsyncEventExec(String actionSID) {
        return asyncExecMap.get(actionSID);
    }

    public boolean askConfirm;
    public String askConfirmMessage;

    public boolean hasEditObjectAction;
    public boolean hasChangeAction; // programmatic or user
    public boolean hasUserChangeAction() { // user
        if(!hasChangeAction)
            return false;

        if (getEventType() instanceof GHTMLTextType)
            return externalChangeType instanceof GHTMLTextType;

        // if custom render change is the input of some type, then probably it is a programmatic change (i.e. custom renderer uses changeValue to set this value, and should not be replaced with the input)
        return customRenderFunction == null || externalChangeType == null;
    }

    public boolean disableInputList;

    public GEditBindingMap editBindingMap;

    public boolean hasDynamicImage;
    public Boolean focusable;
    public boolean checkEquals;
    public GPropertyEditType editType = GPropertyEditType.EDITABLE;

    public boolean echoSymbols;
    public boolean noSort;
    public GCompare defaultCompare;

    public GCompare getDefaultCompare() {
        return defaultCompare != null ? defaultCompare : GCompare.EQUALS;
    }

    public GCompare[] getFilterCompares() {
        return getRenderType(RendererType.FILTER).getFilterCompares();
    }

    public boolean hasStaticImage() {
        assert isAction();
        return appImage != null;
    }
    public boolean hasDynamicImage() { // when it's an action and has dynamic image
        return hasDynamicImage;
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
    public GLoadingReader loadingReader;
    public GShowIfReader showIfReader;
    public GFooterReader footerReader;
    public GReadOnlyReader readOnlyReader;
    public GValueElementClassReader valueElementClassReader;

    public GCaptionElementClassReader captionElementClassReader;
    public GBackgroundReader backgroundReader;
    public GForegroundReader foregroundReader;
    public GImageReader imageReader;

    public GCommentReader commentReader;
    public GCommentElementClassReader commentElementClassReader;
    public GPlaceholderReader placeholderReader;

    // for pivoting
    public String formula;
    public ArrayList<GPropertyDraw> formulaOperands;

    public String aggrFunc;
    public ArrayList<GLastReader> lastReaders;
    public boolean lastAggrDesc;

    public GPropertyDraw quickFilterProperty;

    public int charWidth;
    public int charHeight;

    public int valueWidth;
    public int valueHeight;

    public int captionWidth;
    public int captionHeight;

    public boolean panelCaptionVertical;
    public Boolean panelCaptionLast;
    public GFlexAlignment panelCaptionAlignment;

    public boolean panelColumnVertical;
    
    public GFlexAlignment valueAlignment;

    public String comment;
    public String commentElementClass;
    public boolean panelCommentVertical;
    public Boolean panelCommentFirst;
    public GFlexAlignment panelCommentAlignment;

    public String placeholder;

    public Boolean changeOnSingleClick;

    public boolean hide;

    private transient CellRenderer cellRenderer;
    private transient CellRenderer valueRenderer;

    public boolean notNull;

    public boolean sticky;

    public boolean hasFooter;

    private boolean isSuppressOnFocusChange(Element element) {
        FocusUtils.Reason focusReason = FocusUtils.getFocusReason(element);

        if(focusReason != null) { // system (probably navigate), so we will not suppress it
            switch (focusReason) {
                // for input setting focus will lead to starting change event handling "in between" (inside focus) with unpredictable consequences, so we'll not do that
                // we could not set focus at all (it will work because in SimpleTextBasedEditor we consume the event propagating to native (so focus will be set anyway)), but for now we'll do this way
                case MOUSECHANGE:
                // we don't focus to be set and rely on mouse event handling
                case MOUSENAVIGATE:
                // it's really odd to start editing while scrolling, and other navigating
                case SCROLLNAVIGATE:
                case KEYMOVENAVIGATE:
                // CHANGE will be started anyway
                case BINDING:
                // really odd behaviour to start editing (dropdown list) when focus is returned
                case RESTOREFOCUS:
                // not sure about SHOW, but it seems that this way is better
                case SHOW:
                // after applying filter, start editing does not make much sense
                case APPLYFILTER:
                // because there is a manual startediting
//                case NEWFILTER:
                // unknown reason, it's better to suppress
                case OTHER:
                    return true;
            }
        }

        return false;
    }

    // eventually gets to PropertyDrawEntity.getEventAction (which is symmetrical to this)
    public String getEventSID(Event editEvent, ExecuteEditContext editContext, Result<Integer> contextAction) {
        if (editBindingMap != null) { // property bindings
            String actionSID = editBindingMap.getEventSID(editEvent);
            if(actionSID != null)
                return actionSID;
        }

        Integer inputActionIndex = getInputActionIndex(editEvent, false);
        if(inputActionIndex != null) {
            contextAction.set(inputActionIndex);
            return CHANGE;
        }

        if (isEditObjectEvent(editEvent, hasEditObjectAction, hasUserChangeAction())) // has to be before isChangeEvent, since also handles MOUSE CHANGE event
            return GEditBindingMap.EDIT_OBJECT;

        // starting change on focus, or any key pressed when focus is on input
        boolean isFocus = BrowserEvents.FOCUS.equals(editEvent.getType());
        Element focusElement;
        if((isFocus || GKeyStroke.isInputKeyEvent(editEvent, () -> SimpleTextBasedCellRenderer.isMultiLineInput(editContext.getEditElement())))
                && (focusElement = SimpleTextBasedCellRenderer.getFocusEventTarget(editContext.getEditElement(), editEvent)) != null &&
                !(isFocus && isSuppressOnFocusChange(focusElement)))
            return CHANGE;

        if (GMouseStroke.isChangeEvent(editEvent)) {
            Integer actionIndex = (Integer) GEditBindingMap.getToolbarAction(editEvent);
            if(actionIndex == null) {
                actionIndex = getDialogInputActionIndex();
            }
            contextAction.set(actionIndex);
            return CHANGE;
        }

        if (isGroupChangeKeyEvent(editEvent))
            return GEditBindingMap.GROUP_CHANGE;

        GType changeType = getChangeType();
        if (isCharModifyKeyEvent(editEvent, changeType == null ? null : changeType.getEditEventFilter()) ||
                isDropEvent(editEvent) || isChangeAppendKeyEvent(editEvent))
            return CHANGE;

        return null;
    }

    public Integer getInputActionIndex(Event editEvent, boolean isEditing) {
        GInputList inputList;
        if (KEYDOWN.equals(editEvent.getType()) && (inputList = getInputList()) != null) {
            GKeyStroke keyStroke = null;
            for (int i = 0; i < inputList.actions.length; i++) {
                GInputListAction action = inputList.actions[i];
                if (action.keyStroke != null) {
                    if (keyStroke == null)
                        keyStroke = getKeyStroke(editEvent);
                    if (keyStroke.equals(action.keyStroke) && bindEditing(action.editingBindingMode, isEditing)) {
                        return action.index;
                    }
                }
            }
        }
        return null;
    }

    public Integer getDialogInputActionIndex() {
        GInputList inputList = getInputList();
        if (inputList != null) {
            return getDialogInputActionIndex(inputList.actions);
        }
        return null;
    }

    public Integer getDialogInputActionIndex(GInputListAction[] actions) {
        if (actions != null && (disableInputList || FormsController.isDialogMode())) {
            for (int i = 0; i < actions.length; i++) {
                GInputListAction action = actions[i];
                //addDialogInputAProp from server
                if (action.id != null && action.id.equals(AppStaticImage.INPUT_DIALOG)) {
                    return action.index;
                }
            }
        }
        return null;
    }

    private boolean bindEditing(GBindingMode bindEditing, boolean isEditing) {
        switch (bindEditing) {
            case AUTO:
            case ALL:
                return true;
            case NO:
                return !isEditing;
            case ONLY:
                return isEditing;
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + bindEditing);
        }
    }

    public boolean isFilterChange(Event editEvent, Result<Boolean> contextAction) {
        return GEditBindingMap.isDefaultFilterChange(editEvent, contextAction, getRenderType(RendererType.FILTER).getEditEventFilter());
    }

    public GPropertyDraw(){}

    public PanelRenderer createPanelRenderer(GFormController form, ActionOrPropertyValueController controller, GGroupObjectValue columnKey, Result<CaptionWidget> captionContainer) {
        return getRenderType(RendererType.PANEL).createPanelRenderer(form, controller, this, columnKey, captionContainer);
    }

    public boolean isAction() {
        return getValueType() instanceof GActionType;
    }

    public boolean isPanelBoolean() {
        return getRenderType(RendererType.PANEL) instanceof GLogicalType;
    }

    public CellRenderer getCellRenderer(RendererType rendererType) {
        if(rendererType == RendererType.CELL || !differentValue) {
            if (cellRenderer == null) {
                cellRenderer = createCellRenderer(rendererType, false);
            }
            return cellRenderer;
        } else {
            if (valueRenderer == null)
                valueRenderer = createCellRenderer(rendererType, true);
            return valueRenderer;
        }
    }

    public boolean isCustom(RendererType rendererType) {
        return getCellRenderer(rendererType) instanceof CustomCellRenderer;
    }

    private CellRenderer createCellRenderer(RendererType rendererType, boolean noCustom) {
        if (customRenderFunction != null && !noCustom) // we don't want custom renderer if value type differs from the cell type
            return new CustomCellRenderer(this, customRenderFunction);

        return getRenderType(rendererType).createCellRenderer(this);
    }

    public void setUserPattern(String pattern) {
//        if(baseType instanceof GFormatType)
        this.pattern = pattern != null ? pattern : defaultPattern;
    }

    public PValue parsePaste(String s, GType parseType) {
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

    public boolean canUseChangeValueForRendering(GType type, RendererType rendererType) {
        GType renderType = getRenderType(rendererType);
        return type != null && renderType.getClass() == type.getClass() && !(renderType instanceof GJSONType) && !(renderType instanceof GFileType);
    }

    public String getPanelCaption(String caption) {
        if(caption == null)
            return null;

        if(showChangeKey && hasKeyBinding())
            caption += " (" + getKeyBindingText() + ")";
        return caption;
    }

    public String getNotEmptyCaption(String caption) {
        if (GwtSharedUtils.isRedundantString(caption)) {
            caption = propertyFormName;
//            return getMessages().propertyEmptyCaption();
        }
        return caption;
    }
    public String getNotEmptyCaption() {
        return getNotEmptyCaption(caption);
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
                "<b>" + getMessages().propertyTooltipPath() + ":</b> %s<a class='lsf-tooltip-path'></a> &ensp; <a class='lsf-tooltip-help'></a><br>" +
                createTooltipHorizontalSeparator() +
                "<b>" + getMessages().propertyTooltipFormPropertyName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipFormPropertyDeclaration() + ":</b> %s<a class='lsf-form-property-declaration'></a> &ensp; <a class='lsf-tooltip-form-decl-help'></a>&ensp;" +
                "</html>";
    }  
    
    public static String getDetailedActionToolTipFormat() {
        return  createTooltipHorizontalSeparator() +
                "<b>sID:</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipObjects() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipPath() + ":</b> %s<a class='lsf-tooltip-path'></a> &ensp; <a class='lsf-tooltip-help'></a><br>" +
                createTooltipHorizontalSeparator() +
                "<b>" + getMessages().propertyTooltipFormPropertyName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipFormPropertyDeclaration() + ":</b> %s<a class='lsf-form-property-declaration'></a> &ensp; <a class='lsf-tooltip-form-decl-help'></a>&ensp;" +
                "</html>";
    }
    
    public static String getChangeKeyToolTipFormat() {
        return createTooltipHorizontalSeparator() + "<b>" + getMessages().propertyTooltipHotkey() + ":</b> %s<br>";
    }
    public String getQuickActionTooltipText(GKeyStroke keyStroke) {
        return keyStroke != null ? ("<b>" + getMessages().propertyTooltipHotkey() + ":</b> " + keyStroke) : null;
    }

    public String getTooltip(String caption) {
        String propCaption = GwtSharedUtils.nullTrim(!GwtSharedUtils.isRedundantString(toolTip) ? toolTip : caption);
        String keyBindingText = hasKeyBinding() ? GwtSharedUtils.stringFormat(getChangeKeyToolTipFormat(), getKeyBindingText()) : "";

        if (!MainFrame.showDetailedInfo) {
            return propCaption.isEmpty() ? null : GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT, propCaption, keyBindingText);
        } else {
            String ifaceObjects = GwtSharedUtils.toString(", ", interfacesCaptions);
            String scriptPath = creationPath != null ? escapeLineBreakHTML(creationPath) : "";
            String scriptFormPath = formPath != null ? escapeLineBreakHTML(formPath) : "";
            
            if (isAction()) {
                return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + getDetailedActionToolTipFormat(),
                        propCaption, keyBindingText, canonicalName, ifaceObjects, scriptPath, propertyFormName, scriptFormPath);
            } else {
                String tableName = this.tableName != null ? this.tableName : "&lt;none&gt;";
                String returnClass = this.returnClass != null ? this.returnClass.toString() : "";
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

    public GSize getImageWidth(GFont font) {
        assert isAction();
        return appImage != null ? appImage.getWidth(font) : null;
    }
    public GSize getImageHeight(GFont font) {
        assert isAction();
        return appImage != null ? appImage.getHeight(font) : null;
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

    public boolean isTagInput() {
        return tag != null && tag.equals("input");
    }

    public boolean isFlex() {
        return flex == -2 || super.isFlex();
    }
    public double getFlex(RendererType rendererType) {
        if (flex == -2) {
            return getValueWidth(null, true, false, rendererType).getValueFlexSize();
        }
        return super.getFlex(rendererType);
    }

    public boolean isPanelCaptionLast() {
        return panelCaptionLast != null ? panelCaptionLast : (isPanelBoolean() && !panelCaptionVertical);
    }

    public boolean isPanelCommentFirst() {
        return panelCommentFirst != null ? panelCommentFirst : (isPanelBoolean() && !panelCommentVertical);
    }

    public GFlexAlignment getPanelCaptionAlignment() {
        return (panelCaptionAlignment != null && panelCaptionAlignment != GFlexAlignment.STRETCH) ? panelCaptionAlignment : GFlexAlignment.CENTER;
    }

    public GFlexAlignment getPanelValueAlignment() {
        return getRenderType(RendererType.PANEL) instanceof GLogicalType && isTagInput() ? GFlexAlignment.CENTER : GFlexAlignment.STRETCH; // we don't want to stretch input, since it's usually has fixed size
    }

    public GFlexAlignment getPanelCommentAlignment() {
        return (panelCommentAlignment != null && panelCommentAlignment != GFlexAlignment.STRETCH) ? panelCommentAlignment : GFlexAlignment.CENTER;
    }

    public GFlexAlignment getAlignment() {
        return alignment;
    }
    
    public Style.TextAlign getHorzTextAlignment(RendererType rendererType) {
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
        return getRenderType(rendererType).getHorzTextAlignment();
    }

    public String getVertTextAlignment(boolean isInput, RendererType rendererType) {
        return getRenderType(rendererType).getVertTextAlignment(isInput);
    }

    public InputElement createTextInputElement(RendererType rendererType) {
        InputElement inputElement = getRenderType(rendererType).createTextInputElement();
        inputElement.addClassName("prop-input");
        inputElement.addClassName("form-control");
        return inputElement;
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
    public void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        controller.getPropertyController(this).updateProperty(this, getColumnKeys(this, controller.getCurrentGridObjects()), updateKeys, values);
    }

    // not null
    // padding has to be included for grid column for example, and not for panel property (since flex, width, min-width, etc. doesn't include padding)
    public GSize getValueWidthWithPadding(GFont parentFont, RendererType rendererType) {
        return getValueWidth(parentFont, true, true, rendererType).add(getCellRenderer(rendererType).getWidthPadding() * 2);
    }

    public boolean hasAutoSize() {
        return valueWidth == -1 || valueHeight == -1;
    }

    // not null
    public GSize getValueWidth(GFont parentFont, boolean needNotNull, boolean globalCaptionIsDrawn, RendererType rendererType) {
        if (valueWidth >= 0)
            return GSize.getValueSize(valueWidth);

        if(!needNotNull && valueWidth == -1 && charWidth == 0)
            return null;

        return getRenderType(rendererType).getValueWidth(getFont(parentFont), this, needNotNull, globalCaptionIsDrawn);
    }

    // not null
    public GSize getValueHeight(GFont parentFont, boolean needNotNull, boolean globalCaptionIsDrawn, RendererType rendererType) {
        if (valueHeight >= 0)
            return GSize.getValueSize(valueHeight);

        if(!needNotNull && valueHeight == -1 && charHeight == 0)
            return null;

        return getRenderType(rendererType).getValueHeight(getFont(parentFont), this, needNotNull, globalCaptionIsDrawn);
    }

    private GFont getFont(GFont parentFont) {
        if(font != null)
            return font;

        if(parentFont != null)
            return parentFont;

        return null;
    }

    public GSize getCaptionWidth() {
        if(captionWidth >= 0)
            return GSize.getValueSize(captionWidth);

        return null;
    }

    public GSize getCaptionHeight() {
        if(captionHeight >= 0)
            return GSize.getValueSize(captionHeight);

        return null;
    }

    public GSize getHeaderCaptionHeight(GGridPropertyTable table) {
        GSize headerHeight = table.getHeaderHeight();
        if(headerHeight != null)
            return headerHeight;

        return getCaptionHeight();
    }

    public GFormatType getFormatType(RendererType rendererType) {
        GType renderType = getRenderType(rendererType);
        return (renderType instanceof GObjectType ? GLongType.instance : ((GFormatType) renderType));
    }

    public LinkedHashMap<String, String> getContextMenuItems() {
        return editBindingMap == null ? null : editBindingMap.getContextMenuItems();
    }

    public boolean isFocusable() {
        if(focusable != null)
            return focusable;
        return !hasKeyBinding();
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
    public boolean isDefautAlignCaption() {
        return caption != null && !hasColumnGroupObjects() && (!isAction() && (!panelCaptionVertical && !isPanelBoolean()) || isTab());
    }
}
