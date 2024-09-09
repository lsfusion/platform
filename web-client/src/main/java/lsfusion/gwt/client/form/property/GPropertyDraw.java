package lsfusion.gwt.client.form.property;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.*;
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
import lsfusion.gwt.client.form.property.async.*;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
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
import java.util.*;

import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;
import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.client.form.event.GKeyStroke.*;

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

    public int wrap;
    public boolean wrapWordBreak;
    public boolean collapse;
    public boolean ellipsis;

    public int captionWrap;
    public boolean captionWrapWordBreak;
    public boolean captionCollapse;
    public boolean captionEllipsis;

    public boolean clearText;
    public boolean notSelectAll;
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

    public Boolean inline;

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

    public GClass returnClass;

    public String tag;
    public GInputType inputType;
    public String valueElementClass;
    public boolean toolbar;
    public boolean toolbarActions;

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

    public GInputListAction[] getInputListActions() {
        GAsyncEventExec asyncExec = getAsyncEventExec(ServerResponse.CHANGE);
        return asyncExec instanceof GAsyncInput ? ((GAsyncInput) asyncExec).inputListActions : null;
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
        GInputListAction[] inputListActions = getInputListActions();

        List<QuickAccessAction> actions = new ArrayList<>();
        if(inputListActions != null) {
            for (int i = 0; i < inputListActions.length; i++) {
                List<GQuickAccess> quickAccessList = inputListActions[i].quickAccessList;

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

                GInputListAction action = inputListActions[i];
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

    public boolean ignoreHasHeaders;

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

    public boolean hasExternalChangeActionForRendering(RendererType rendererType) {
        return canUseChangeValueForRendering(externalChangeType, rendererType);
    }

    public boolean disableInputList;

    public GEditBindingMap editBindingMap;

    public boolean hasDynamicImage;
    public boolean hasDynamicCaption;
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
    public boolean hasDynamicImage() {
        return hasDynamicImage;
    }
    public boolean hasDynamicCaption() {
        return hasDynamicCaption;
    }

    public ArrayList<GInputBindingEvent> bindingEvents = new ArrayList<>();
    public boolean showChangeKey;
    public boolean showChangeMouse;

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

    public boolean hasMouseBinding() {
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GMouseInputEvent)
                return true;
        return false;
    }
    public String getMouseBindingText() {
        assert hasMouseBinding();
        String result = "";
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GMouseInputEvent) {
                result = (result.isEmpty() ? "" : result + ",") + ((GMouseInputEvent) bindingEvent.inputEvent).mouseEvent;
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
    public GExtraPropReader fontReader;
    public GBackgroundReader backgroundReader;
    public GForegroundReader foregroundReader;
    public GImageReader imageReader;

    public GExtraPropReader commentReader;
    public GExtraPropReader commentElementClassReader;
    public GExtraPropReader placeholderReader;
    public GExtraPropReader patternReader;
    public GExtraPropReader regexpReader;
    public GExtraPropReader regexpMessageReader;
    public GExtraPropReader tooltipReader;
    public GExtraPropReader valueTooltipReader;

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

    public boolean panelColumnVertical;
    
    public GFlexAlignment valueAlignmentHorz;
    public GFlexAlignment valueAlignmentVert;

    public String valueOverflowHorz;
    public String valueOverflowVert;

    public boolean valueShrinkHorz;
    public boolean valueShrinkVert;

    public String comment;
    public String commentElementClass;
    public boolean panelCommentVertical;
    public boolean panelCommentFirst;
    public GFlexAlignment panelCommentAlignment;

    public String placeholder;

    public String pattern;
    public String userPattern;

    public String regexp;
    public String regexpMessage;

    public String tooltip;
    public String valueTooltip;

    public Boolean changeOnSingleClick;

    public boolean hide;

    private transient CellRenderer cellRenderer;
    private transient CellRenderer valueRenderer;

    public boolean notNull;

    public boolean sticky;

    public boolean hasFooter;

    // eventually gets to PropertyDrawEntity.getEventAction (which is symmetrical to this)
    public String getEventSID(Event editEvent, boolean isBinding, ExecuteEditContext editContext, Result<Integer> contextAction) {
        if(isBinding)
            return GEditBindingMap.changeOrGroupChange(GKeyStroke.isKeyEvent(editEvent) && FormsController.isForceGroupChangeMode()); // if binding has alt in it, it doesn't mean that we want group change

        if (editBindingMap != null) { // property bindings
            String actionSID = editBindingMap.getEventSID(editEvent);
            if(actionSID != null)
                return actionSID;
        }

        Integer inputActionIndex = getKeyInputActionIndex(getInputListActions(), editEvent, false);
        if(inputActionIndex != null) {
            contextAction.set(inputActionIndex);
            return GEditBindingMap.changeOrGroupChange();
        }

        if (isEditObjectEvent(editEvent, hasEditObjectAction, hasUserChangeAction(), customRenderFunction != null)) // has to be before isChangeEvent, since also handles MOUSE CHANGE event
            return GEditBindingMap.EDIT_OBJECT;

        // starting change on focus, or any key pressed when focus is on input
        Element editElement = editContext.getEditElement();
        InputElement inputElement = InputBasedCellRenderer.getInputEventTarget(editElement, editEvent);
        if (inputElement != null) {
            UpdateContext updateContext = editContext.getUpdateContext();

            GInputType inputType = InputBasedCellRenderer.getInputElementType(inputElement);
            if(inputType.isStretchText()) {
                if (DataGrid.FOCUSIN.equals(editEvent.getType()) && !FocusUtils.isSuppressOnFocusChange(inputElement))
                    return GEditBindingMap.changeOrGroupChange();

                if (InputBasedCellRenderer.isInputKeyEvent(editEvent, updateContext, inputType.isMultilineText()))
                    return GEditBindingMap.changeOrGroupChange();
            }

            if (!updateContext.isNavigateInput() && GKeyStroke.isKeyDownEvent(editEvent))
                if (editEvent.getShiftKey() && (isCharNavigateHorzKeyEvent(editEvent) || isCharNavigateVertKeyEvent(editEvent))) {
                    return GEditBindingMap.changeOrGroupChange();
                }
        }

        if (GMouseStroke.isChangeEvent(editEvent)) {
            Integer actionIndex = (Integer) GEditBindingMap.getToolbarAction(editEvent);
            if(actionIndex == null) {
                actionIndex = getDialogInputActionIndex();
            }
            contextAction.set(actionIndex);
            return GEditBindingMap.changeOrGroupChange();
        }

        if (GKeyStroke.isGroupChangeKeyEvent(editEvent))
            return GEditBindingMap.GROUP_CHANGE;

        GType changeType = getChangeType();
        if (isCharModifyKeyEvent(editEvent, changeType == null ? null : changeType.getEditEventFilter()) ||
                isDropEvent(editEvent) || isChangeAppendKeyEvent(editEvent))
            return GEditBindingMap.changeOrGroupChange();

        return null;
    }

    public Integer getKeyInputActionIndex(GInputListAction[] actions, Event editEvent, boolean isEditing) {
        if (actions != null && KEYDOWN.equals(editEvent.getType())) {
            GKeyStroke keyStroke = null;
            for (GInputListAction action : actions) {
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
        GInputListAction[] inputListActions = getInputListActions();
        if (inputListActions != null) {
            return getDialogInputActionIndex(inputListActions);
        }
        return null;
    }

    public Integer getDialogInputActionIndex(GInputListAction[] actions) {
        if (actions != null && (disableInputList || FormsController.isDialogMode())) {
            for (GInputListAction action : actions) {
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

    public void setUserPattern(String userPattern) {
        this.userPattern = userPattern;
    }

    public PValue parsePaste(String s, GType parseType) {
        if (s == null) {
            return null;
        }
        if(parseType == null)
            return null;
        try {
            return parseType.parseString(s, getPattern());
        } catch (ParseException pe) {
            return null;
        }
    }

    public String getPattern() {
        return nvl(userPattern, pattern);
    }

    public boolean canUseChangeValueForRendering(GType type, RendererType rendererType) {
        GType renderType = getRenderType(rendererType);
        return type != null && renderType.getClass() == type.getClass() && !(renderType instanceof GJSONType) && !(renderType instanceof GFileType);
    }

    public String getPanelCaption(String caption) {
        if(caption == null)
            return null;

        String eventCaption = getEventCaption(showChangeKey && hasKeyBinding() ? getKeyBindingText() : null,
                showChangeMouse && hasMouseBinding() ? getMouseBindingText() : null);
        return caption + (eventCaption != null ? " (" + eventCaption + ")" : "");
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
        String propCaption = GwtSharedUtils.nullTrim(!GwtSharedUtils.isRedundantString(tooltip) ? tooltip : caption);

        String eventCaption = getEventCaption(hasKeyBinding() ? getKeyBindingText() : null, hasMouseBinding() ? getMouseBindingText() : null);
        String bindingText = eventCaption != null ? GwtSharedUtils.stringFormat(getChangeKeyToolTipFormat(), eventCaption) : "";

        if (!MainFrame.showDetailedInfo) {
            return propCaption.isEmpty() ? null : GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT, propCaption, bindingText);
        } else {
            String ifaceObjects = GwtSharedUtils.toString(", ", interfacesCaptions);
            String scriptPath = creationPath != null ? creationPath : "";
            String scriptFormPath = formPath != null ? formPath : "";

            if (isAction()) {
                return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + getDetailedActionToolTipFormat(),
                        propCaption, bindingText, canonicalName, ifaceObjects, scriptPath, propertyFormName, scriptFormPath);
            } else {
                String tableName = this.tableName != null ? this.tableName : "&lt;none&gt;";
                String returnClass = this.returnClass != null ? this.returnClass.toString() : "";
                String ifaceClasses = GwtSharedUtils.toString(", ", interfacesTypes);
                String script = creationScript != null ? creationScript : "";

                return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + getDetailedToolTipFormat(),
                        propCaption, bindingText, canonicalName, tableName, ifaceObjects, returnClass, ifaceClasses,
                        script, scriptPath, propertyFormName, scriptFormPath);
            }
        }
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

    public Boolean isReadOnly() {
        return editType == GPropertyEditType.EDITABLE ? null : editType == GPropertyEditType.DISABLE;
    }

    public boolean isEditableNotNull() {
        return notNull && isReadOnly() == null;
    }

    public boolean isTagInput() {
        return tag != null && tag.equals("input");
    }

    public boolean isFlex() {
        return flex == -2 || super.isFlex();
    }
    public double getFlex(RendererType rendererType) {
        if (flex == -2) {
            return getValueWidth(null, true, false).getValueFlexSize();
        }
        return super.getFlex(rendererType);
    }

    public boolean isPanelCommentFirst() {
        return panelCommentFirst;
    }

    public GFlexAlignment getPanelValueAlignment() {
        return getRenderType(RendererType.PANEL) instanceof GLogicalType && isTagInput() ? GFlexAlignment.CENTER : GFlexAlignment.STRETCH; // we don't want to stretch input, since it's usually has fixed size
    }

    public GFlexAlignment getPanelCommentAlignment() {
        return panelCommentAlignment;
    }

    public GFlexAlignment getHorzTextAlignment() {
        return valueAlignmentHorz;
    }

    public GFlexAlignment getVertTextAlignment() {
        return valueAlignmentVert;
    }

    public String getValueOverflowHorz() {
        return valueOverflowHorz;
    }

    public String getValueOverflowVert() {
        return valueOverflowVert;
    }

    public boolean getValueShrinkHorz() {
        return valueShrinkHorz;
    }

    public boolean getValueShrinkVert() {
        return valueShrinkVert;
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

    public boolean hasAutoSize() {
        return hasAutoWidth() || hasAutoHeight();
    }

    public boolean hasAutoWidth() {
        return valueWidth == -1 && charWidth == 0;
    }

    public boolean hasAutoHeight() {
        return valueHeight == -1 && charHeight == 0;
    }

    // not null
    public GSize getValueWidth(GFont parentFont, boolean needNotNull, boolean globalCaptionIsDrawn) {
        if (valueWidth >= 0)
            return GSize.getValueSize(valueWidth);

        if(!needNotNull && hasAutoWidth())
            return null;

        return getValueType().getValueWidth(getFont(parentFont), this, needNotNull, globalCaptionIsDrawn);
    }

    // not null
    public GSize getValueHeight(GFont parentFont, boolean needNotNull, boolean globalCaptionIsDrawn) {
        if (valueHeight >= 0)
            return GSize.getValueSize(valueHeight);

        if(!needNotNull && hasAutoHeight())
            return null;

        return getValueType().getValueHeight(getFont(parentFont), this, needNotNull, globalCaptionIsDrawn);
    }

    private GFont getFont(GFont parentFont) {
        if(font != null)
            return font;

        if(parentFont != null)
            return parentFont;

        return null;
    }

    public ImageHtmlOrTextType getCaptionHtmlOrTextType() {
        // property / action grid caption
        return new ImageHtmlOrTextType() {
            @Override
            protected boolean isEllipsis() {
                return captionEllipsis;
            }

            @Override
            protected boolean isCollapse() {
                return captionCollapse;
            }

            @Override
            public int getWrap() {
                return captionWrap;
            }

            @Override
            protected boolean isWrapWordBreak() {
                return captionWrapWordBreak;
            }
        };
    }

    // not clear if it is caption, or data (however rendered as caption)
    public ImageHtmlOrTextType getActionHtmlOrTextType() {
        return new ImageHtmlOrTextType() {
            @Override
            public boolean isImageVertical() {
                return captionVertical;
            }

            @Override
            public int getWrap() {
                return wrap;
            }

            @Override
            protected boolean isWrapWordBreak() {
                return wrapWordBreak;
            }

            @Override
            protected boolean isCollapse() {
                return collapse;
            }

            @Override
            protected boolean isEllipsis() {
                return ellipsis;
            }
        };
    }

    public DataHtmlOrTextType getDataHtmlOrTextType() {
        return new DataHtmlOrTextType() {
            @Override
            public int getWrap() {
                return wrap;
            }

            @Override
            protected boolean isWrapWordBreak() {
                return wrapWordBreak;
            }

            @Override
            protected boolean isCollapse() {
                return collapse;
            }

            @Override
            protected boolean isEllipsis() {
                return ellipsis;
            }
        };
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
        return caption != null && !hasColumnGroupObjects() && ((!isAction() && !captionVertical && !isPanelBoolean()) || isTab());
    }

    public boolean isInline() {
        // not supported yet
        // also there is an alignCaptions check, but it's done above
        if(hasColumnGroupObjects() || isTab())
            return false;

        if(inline != null)
            return inline;

        return isInCustom();
    }

    // should match PropertyDrawEntity.isPredefinedImage
    public boolean isPredefinedImage() {
        String sid = integrationSID;
        return sid != null && sid.equals("image");
    }
}
