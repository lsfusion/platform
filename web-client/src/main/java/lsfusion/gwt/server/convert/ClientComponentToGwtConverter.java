package lsfusion.gwt.server.convert;

import com.google.gwt.event.dom.client.KeyCodes;
import lsfusion.client.classes.ClientActionClass;
import lsfusion.client.classes.data.ClientFileClass;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.filter.ClientRegularFilter;
import lsfusion.client.form.filter.ClientRegularFilterGroup;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.object.table.ClientToolbar;
import lsfusion.client.form.object.table.grid.ClientGrid;
import lsfusion.client.form.object.table.grid.user.toolbar.ClientCalculations;
import lsfusion.client.form.object.table.tree.ClientTreeGroup;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.async.ClientAsyncEventExec;
import lsfusion.client.form.property.cell.EditBindingMap;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.classes.GClass;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.filter.GRegularFilter;
import lsfusion.gwt.client.form.filter.GRegularFilterGroup;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.GCalculations;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.navigator.window.GModalityType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.property.PivotOptions;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.interop.form.property.PropertyGroupType;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;

import static lsfusion.gwt.server.convert.StaticConverters.convertColor;

@SuppressWarnings("UnusedDeclaration")
public class ClientComponentToGwtConverter extends CachedObjectConverter {

    private final ClientTypeToGwtConverter typeConverter = ClientTypeToGwtConverter.getInstance();
    private final ClientAsyncToGwtConverter asyncConverter = ClientAsyncToGwtConverter.getInstance();
    private GForm form;

    public ClientComponentToGwtConverter(String logicsName) {
        super(logicsName);
    }

    private <T extends GComponent> T initGwtComponent(ClientComponent clientComponent, T component) {
        cacheInstance(clientComponent, component);

        component.ID = clientComponent.ID;
        component.sID = clientComponent.getSID();
        component.container = convertOrCast(clientComponent.container);
        component.defaultComponent = clientComponent.defaultComponent;

        component.width = clientComponent.width;
        component.height = clientComponent.height;

        component.span = clientComponent.span;

        component.setFlex(clientComponent.flex);
        component.setAlignment(convertFlexAlignment(clientComponent.alignment));
        component.shrink = clientComponent.shrink;
        component.alignShrink = clientComponent.alignShrink;
        component.marginTop = clientComponent.marginTop;
        component.marginBottom = clientComponent.marginBottom;
        component.marginLeft = clientComponent.marginLeft;
        component.marginRight = clientComponent.marginRight;

        if (clientComponent.design.getBackground() != null) {
            component.background = convertColor(clientComponent.design.getBackground());
        }

        if (clientComponent.design.getForeground() != null) {
            component.foreground = convertColor(clientComponent.design.getForeground());
        }

        FontInfo clientFont = clientComponent.design.getFont();
        component.font = convertFont(clientFont);

        FontInfo captionFont = clientComponent.design.getCaptionFont();
        component.captionFont = convertFont(captionFont);

        return component;
    }

    private GFlexAlignment convertFlexAlignment(FlexAlignment alignment) {
        if (alignment == null) {
            return null;
        }
        try {
            return GFlexAlignment.valueOf(alignment.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown alignment");
        }
    }

    public GFont convertFont(FontInfo clientFont) {
        GFont font = StaticConverters.convertFont(clientFont);
        if (font == null) {
            return null;
        }
        form.addFont(font);
        return font;
    }

    @Cached
    @Converter(from = ClientContainer.class)
    public GContainer convertContainer(ClientContainer clientContainer) {
        GContainer container = initGwtComponent(clientContainer,  new GContainer());

        container.caption = clientContainer.caption;
        container.collapsible = clientContainer.collapsible;
        container.horizontal = clientContainer.horizontal;
        container.tabbed = clientContainer.tabbed;
        container.childrenAlignment = convertFlexAlignment(clientContainer.childrenAlignment);
        container.grid = clientContainer.grid;
        container.wrap = clientContainer.wrap;
        container.alignCaptions = clientContainer.alignCaptions;
        container.lines = clientContainer.lines;
        container.lineSize = clientContainer.lineSize;
        container.captionLineSize = clientContainer.captionLineSize;
        container.lineShrink = clientContainer.lineShrink;
        container.customDesign = clientContainer.customDesign;

        for (ClientComponent child : clientContainer.children) {
            GComponent childComponent = convertOrCast(child);
            container.children.add(childComponent);
        }
        
        return container;
    }

    @Cached
    @Converter(from = ClientRegularFilterGroup.class)
    public GRegularFilterGroup convertRegularFilterGroup(ClientRegularFilterGroup clientFilterGroup) {
        GRegularFilterGroup filterGroup = initGwtComponent(clientFilterGroup, new GRegularFilterGroup());

        filterGroup.defaultFilterIndex = clientFilterGroup.defaultFilterIndex;
        filterGroup.groupObject = convertOrCast(clientFilterGroup.groupObject);

        for (ClientRegularFilter filter : clientFilterGroup.filters) {
            GRegularFilter regularFilter = convertOrCast(filter);
            filterGroup.filters.add(regularFilter);
        }
        return filterGroup;
    }

    @Cached
    @Converter(from = ClientRegularFilter.class)
    public GRegularFilter convertRegularFilter(ClientRegularFilter clientFilter) {
        GRegularFilter filter = new GRegularFilter();
        filter.ID = clientFilter.ID;
        filter.caption = clientFilter.caption;
        filter.key = convertOrCast(clientFilter.key);
        filter.showKey = clientFilter.showKey;
        return filter;
    }

    @Cached
    @Converter(from = ClientToolbar.class)
    public GToolbar convertToolbar(ClientToolbar clientToolbar) {
        GToolbar toolbar = initGwtComponent(clientToolbar, new GToolbar());
        toolbar.visible = clientToolbar.visible;
        toolbar.showCountQuantity = clientToolbar.showCountRows;
        toolbar.showCalculateSum = clientToolbar.showCalculateSum;
        toolbar.showGroup = clientToolbar.showGroupReport;
        toolbar.showPrintGroupXls = clientToolbar.showXls;
        toolbar.showGridSettings = clientToolbar.showSettings;
        return toolbar;
    }

    @Cached
    @Converter(from = ClientFilter.class)
    public GFilter convertFilter(ClientFilter clientFilter) {
        GFilter filter = initGwtComponent(clientFilter, new GFilter());
        filter.property = convertOrCast(clientFilter.property);
        return filter;
    }
    
    @Cached
    @Converter(from = ClientCalculations.class)
    public GCalculations convertCalculations(ClientCalculations clientCalculations) {
        return initGwtComponent(clientCalculations, new GCalculations());
    }

    @Cached
    @Converter(from = ClientGrid.class)
    public GGrid convertGrid(ClientGrid clientGrid) {
        GGrid grid = initGwtComponent(clientGrid, new GGrid());
        grid.groupObject = convertOrCast(clientGrid.groupObject);
        grid.quickSearch = clientGrid.quickSearch;
        grid.headerHeight = clientGrid.headerHeight;

        grid.autoSize = clientGrid.autoSize;
        grid.boxed = clientGrid.boxed;

        grid.lineWidth = clientGrid.lineWidth;
        grid.lineHeight = clientGrid.lineHeight;

        grid.record = convertOrCast(clientGrid.record);

        return grid;
    }

    @Cached
    @Converter(from = ClientPropertyDraw.class)
    public GPropertyDraw convertPropertyDraw(ClientPropertyDraw clientPropertyDraw) {
        GPropertyDraw propertyDraw = initGwtComponent(clientPropertyDraw, new GPropertyDraw());

        propertyDraw.ID = clientPropertyDraw.ID;
        propertyDraw.nativeSID = "p" + clientPropertyDraw.ID;
        propertyDraw.sID = clientPropertyDraw.getSID();
        propertyDraw.namespace = clientPropertyDraw.getNamespace();
        propertyDraw.caption = clientPropertyDraw.caption;
        propertyDraw.canonicalName = clientPropertyDraw.getCanonicalName();
        propertyDraw.propertyFormName = clientPropertyDraw.getPropertyFormName();
        propertyDraw.integrationSID = clientPropertyDraw.getIntegrationSID();

        propertyDraw.autoSize = clientPropertyDraw.autoSize;
        propertyDraw.boxed = clientPropertyDraw.boxed;

        propertyDraw.customRenderFunction = clientPropertyDraw.customRenderFunction;

        propertyDraw.toolTip = clientPropertyDraw.toolTip;
        propertyDraw.clearText = clientPropertyDraw.clearText;
        propertyDraw.tableName = clientPropertyDraw.tableName;
        propertyDraw.interfacesCaptions = clientPropertyDraw.interfacesCaptions;
        propertyDraw.interfacesTypes = new GClass[clientPropertyDraw.interfacesTypes.length];
        for (int i = 0; i < clientPropertyDraw.interfacesTypes.length; i++) {
            propertyDraw.interfacesTypes[i] = typeConverter.convertOrCast(clientPropertyDraw.interfacesTypes[i]);
        }
        propertyDraw.creationScript = clientPropertyDraw.creationScript;
        propertyDraw.creationPath = clientPropertyDraw.creationPath;
        propertyDraw.path = clientPropertyDraw.path;
        propertyDraw.formPath = clientPropertyDraw.formPath;

        propertyDraw.groupObject = convertOrCast(clientPropertyDraw.groupObject);
        if (!clientPropertyDraw.columnGroupObjects.isEmpty()) {
            propertyDraw.columnsName = clientPropertyDraw.columnsName;
            propertyDraw.columnGroupObjects = new ArrayList<>();
            for (ClientGroupObject clientColumnGroup : clientPropertyDraw.columnGroupObjects) {
                GGroupObject columnGroup = convertOrCast(clientColumnGroup);
                propertyDraw.columnGroupObjects.add(columnGroup);
            }
        }

        propertyDraw.baseType = typeConverter.convertOrCast(clientPropertyDraw.baseType);
        propertyDraw.returnClass = typeConverter.convertOrCast(clientPropertyDraw.returnClass);

        propertyDraw.externalChangeType = typeConverter.convertOrCast(clientPropertyDraw.externalChangeType);
        propertyDraw.asyncExecMap = new HashMap<>();
        for(Map.Entry<String, ClientAsyncEventExec> entry : clientPropertyDraw.asyncExecMap.entrySet()) {
            propertyDraw.asyncExecMap.put(entry.getKey(), asyncConverter.convertOrCast(entry.getValue()));
        }

        propertyDraw.askConfirm = clientPropertyDraw.askConfirm;
        propertyDraw.askConfirmMessage = clientPropertyDraw.askConfirmMessage;

        propertyDraw.hasEditObjectAction = clientPropertyDraw.hasEditObjectAction;
        propertyDraw.hasChangeAction = clientPropertyDraw.hasChangeAction;

        propertyDraw.editBindingMap = convertOrCast(clientPropertyDraw.editBindingMap);

        boolean canIconBeDisabled = clientPropertyDraw.baseType instanceof ClientActionClass || clientPropertyDraw.baseType instanceof ClientFileClass;
        propertyDraw.imageHolder = createImage(clientPropertyDraw.design.getImageHolder(), "property", canIconBeDisabled);

        propertyDraw.editType = convertOrCast(clientPropertyDraw.editType);

        propertyDraw.echoSymbols = clientPropertyDraw.echoSymbols;

        propertyDraw.noSort = clientPropertyDraw.noSort;
        if(clientPropertyDraw.defaultCompare != null)
            propertyDraw.defaultCompare = GCompare.get(clientPropertyDraw.defaultCompare.ordinal());

        if(clientPropertyDraw.changeKey != null)
            propertyDraw.bindingEvents.add(convertBinding(clientPropertyDraw.changeKey, clientPropertyDraw.changeKeyPriority, clientPropertyDraw.changeKey.bindingModes));
        if(clientPropertyDraw.changeMouse != null)
            propertyDraw.bindingEvents.add(convertBinding(clientPropertyDraw.changeMouse, clientPropertyDraw.changeMousePriority, clientPropertyDraw.changeMouse.bindingModes));
        propertyDraw.showChangeKey = clientPropertyDraw.showChangeKey;

        propertyDraw.isList = clientPropertyDraw.isList;

        propertyDraw.drawAsync = clientPropertyDraw.drawAsync;

        propertyDraw.pattern = clientPropertyDraw.getFormatPattern();
        propertyDraw.defaultPattern = propertyDraw.pattern;

        propertyDraw.focusable = clientPropertyDraw.focusable;
        propertyDraw.checkEquals = clientPropertyDraw.checkEquals;

        propertyDraw.captionReader = convertCaptionReader(clientPropertyDraw.captionReader);
        propertyDraw.showIfReader = convertShowIfReader(clientPropertyDraw.showIfReader);
        propertyDraw.footerReader = convertFooterReader(clientPropertyDraw.footerReader);
        propertyDraw.readOnlyReader = convertReadOnlyReader(clientPropertyDraw.readOnlyReader);
        propertyDraw.backgroundReader = convertBackgroundReader(clientPropertyDraw.backgroundReader);
        propertyDraw.foregroundReader = convertForegroundReader(clientPropertyDraw.foregroundReader);
        propertyDraw.imageReader = convertImageReader(clientPropertyDraw.imageReader);
        propertyDraw.hasDynamicImage = clientPropertyDraw.hasDynamicImage;

        propertyDraw.formula = clientPropertyDraw.formula;
        if(clientPropertyDraw.formula != null) {
            ArrayList<GPropertyDraw> formulaOperands = new ArrayList<>();
            for (ClientPropertyDraw formulaOperand : clientPropertyDraw.formulaOperands)
                formulaOperands.add(convertOrCast(formulaOperand));
            propertyDraw.formulaOperands = formulaOperands;
        }

        propertyDraw.aggrFunc = clientPropertyDraw.aggrFunc;
        propertyDraw.lastAggrDesc = clientPropertyDraw.lastAggrDesc;
        propertyDraw.lastReaders = new ArrayList<>();
        for(ClientPropertyDraw.LastReader lastReader : clientPropertyDraw.lastReaders)
            propertyDraw.lastReaders.add(convertLastReader(lastReader));

        propertyDraw.quickFilterProperty = convertOrCast(clientPropertyDraw.quickFilterProperty);

        propertyDraw.charWidth = clientPropertyDraw.charWidth;
        propertyDraw.charHeight = clientPropertyDraw.charHeight;

        propertyDraw.valueWidth = clientPropertyDraw.valueWidth;
        propertyDraw.valueHeight = clientPropertyDraw.valueHeight;

        propertyDraw.captionWidth = clientPropertyDraw.captionWidth;
        propertyDraw.captionHeight = clientPropertyDraw.captionHeight;

        propertyDraw.panelCaptionVertical = clientPropertyDraw.panelCaptionVertical;
        propertyDraw.panelCaptionLast = clientPropertyDraw.panelCaptionLast;
        propertyDraw.panelCaptionAlignment = convertFlexAlignment(clientPropertyDraw.panelCaptionAlignment);
        
        propertyDraw.panelColumnVertical = clientPropertyDraw.panelColumnVertical;
        
        propertyDraw.valueAlignment = convertFlexAlignment(clientPropertyDraw.valueAlignment);

        propertyDraw.changeOnSingleClick = clientPropertyDraw.changeOnSingleClick;
        
        propertyDraw.hide = clientPropertyDraw.hide;
        
        propertyDraw.notNull = clientPropertyDraw.notNull;

        propertyDraw.sticky = clientPropertyDraw.sticky;

        propertyDraw.hasFooter = clientPropertyDraw.hasFooter;

//        propertyDraw.getValueWidth(null, form); // parentFont - null потому как на этом этапе интересуют только в панели свойства (а parentFont для грида, там своя ветка)

        return propertyDraw;
    }

    public GInputBindingEvent convertBinding(lsfusion.interop.form.event.InputEvent event, Integer priority, Map<String, BindingMode> bindingModes) {
        return new GInputBindingEvent((GInputEvent)convertOrCast(event),
                        new GBindingEnv(priority != null && priority.equals(0) ? null : priority,
                        convertOrCast(bindingModes != null ? bindingModes.get("preview") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("dialog") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("group") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("editing") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("showing") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("panel") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("cell") : null)
                        ));
    }

    @Converter(from = EditBindingMap.class)
    public GEditBindingMap convertBindingMap(EditBindingMap editBindingMap) {
        HashMap<GKeyStroke, String> keyBindingMap = null;
        if (editBindingMap.getKeyBindingMap() != null) {
            keyBindingMap = new HashMap<>();
            for (Map.Entry<KeyStroke, String> e : editBindingMap.getKeyBindingMap().entrySet()) {
                GKeyStroke key = convertOrCast(e.getKey());
                keyBindingMap.put(key, e.getValue());
            }
        }

        LinkedHashMap<String, String> contextMenuBindingMap = editBindingMap.getContextMenuItems() != null
                                                              ? new LinkedHashMap<>(editBindingMap.getContextMenuItems())
                                                              : null;
        String mouseBinding = editBindingMap.getMouseAction();

        return new GEditBindingMap(keyBindingMap, contextMenuBindingMap, mouseBinding);
    }

    @Converter(from = KeyStroke.class)
    public GKeyStroke convertKeyStroke(KeyStroke keyStroke) {
        int modifiers = keyStroke.getModifiers();
        boolean isAltPressed = (modifiers & InputEvent.ALT_MASK) != 0;
        boolean isCtrlPressed = (modifiers & InputEvent.CTRL_MASK) != 0;
        boolean isShiftPressed = (modifiers & InputEvent.SHIFT_MASK) != 0;
        int keyCode = convertKeyCode(keyStroke.getKeyCode());

        return new GKeyStroke(keyCode, isAltPressed, isCtrlPressed, isShiftPressed);
    }

    @Converter(from = KeyInputEvent.class)
    public GKeyInputEvent convertKeyInputEvent(KeyInputEvent keyInputEvent) {
        return new GKeyInputEvent(convertOrCast(keyInputEvent.keyStroke));
    }

    @Converter(from = MouseInputEvent.class)
    public GMouseInputEvent convertMouseInputEvent(MouseInputEvent mouseInputEvent) {
        return new GMouseInputEvent(convertOrCast(mouseInputEvent.mouseEvent));
    }

    @Converter(from = BindingMode.class)
    public GBindingMode convertBindingMode(BindingMode bindingMode) {
        return  GBindingMode.valueOf(bindingMode.name());
    }

    private int convertKeyCode(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_DELETE:
                return KeyCodes.KEY_DELETE;
            case KeyEvent.VK_ESCAPE:
                return KeyCodes.KEY_ESCAPE;
            case KeyEvent.VK_ENTER:
                return KeyCodes.KEY_ENTER;
            case KeyEvent.VK_INSERT:
                return GKeyStroke.KEY_INSERT;
            default:
                return keyCode;
        }
    }

    public GCaptionReader convertCaptionReader(ClientPropertyDraw.CaptionReader reader) {
        return reader == null ? null : new GCaptionReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GShowIfReader convertShowIfReader(ClientPropertyDraw.ShowIfReader reader) {
        return reader == null ? null : new GShowIfReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GFooterReader convertFooterReader(ClientPropertyDraw.FooterReader reader) {
        return reader == null ? null : new GFooterReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GReadOnlyReader convertReadOnlyReader(ClientPropertyDraw.ReadOnlyReader reader) {
        return reader == null ? null : new GReadOnlyReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GLastReader convertLastReader(ClientPropertyDraw.LastReader reader) {
        return reader == null ? null : new GLastReader(reader.getID(), reader.index, reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GBackgroundReader convertBackgroundReader(ClientPropertyDraw.BackgroundReader reader) {
        return reader == null ? null : new GBackgroundReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GForegroundReader convertForegroundReader(ClientPropertyDraw.ForegroundReader reader) {
        return reader == null ? null : new GForegroundReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GImageReader convertImageReader(ClientPropertyDraw.ImageReader reader) {
        return reader == null ? null : new GImageReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GRowBackgroundReader convertRowBackgroundReader(ClientGroupObject.RowBackgroundReader reader) {
        return reader == null ? null : new GRowBackgroundReader(reader.getID());
    }

    public GRowForegroundReader convertRowForegroundReader(ClientGroupObject.RowForegroundReader reader) {
        return reader == null ? null : new GRowForegroundReader(reader.getID());
    }

    @Cached
    @Converter(from = PropertyEditType.class)
    public GPropertyEditType convertEditType(PropertyEditType editType) {
        switch (editType) {
            case EDITABLE: return GPropertyEditType.EDITABLE;
            case READONLY: return GPropertyEditType.READONLY;
        }
        return null;
    }

    @Cached
    @Converter(from = ModalityType.class)
    public GModalityType convertModalityType(ModalityType modalityType) {
        switch (modalityType) {
            case DOCKED: return GModalityType.DOCKED;
            case DOCKED_MODAL: return GModalityType.DOCKED_MODAL;
            case MODAL: return GModalityType.MODAL;
            case DIALOG_MODAL: return GModalityType.DIALOG_MODAL;
        }
        return null;
    }

    @Cached
    @Converter(from = ClientTreeGroup.class)
    public GTreeGroup convertTreeGroup(ClientTreeGroup clientTreeGroup) {
        GTreeGroup treeGroup = initGwtComponent(clientTreeGroup, new GTreeGroup());
        
        treeGroup.filtersContainer = convertOrCast(clientTreeGroup.filtersContainer);
        for (ClientFilter filter : clientTreeGroup.filters) {
            treeGroup.filters.add(convertOrCast(filter));
        }

        treeGroup.autoSize = clientTreeGroup.autoSize;
        treeGroup.boxed = clientTreeGroup.boxed;

        treeGroup.toolbar = convertOrCast(clientTreeGroup.toolbar);
        
        treeGroup.expandOnClick = clientTreeGroup.expandOnClick;

        treeGroup.headerHeight = clientTreeGroup.headerHeight;

        treeGroup.lineWidth = clientTreeGroup.lineWidth;
        treeGroup.lineHeight = clientTreeGroup.lineHeight;

        for (ClientGroupObject clientGroup : clientTreeGroup.groups) {
            GGroupObject group = convertOrCast(clientGroup);
            treeGroup.groups.add(group);
        }
        return treeGroup;
    }

    @Cached
    @Converter(from = ClientGroupObject.class)
    public GGroupObject convertGroupObject(ClientGroupObject clientGroupObject) {
        GGroupObject groupObject = cacheInstance(clientGroupObject, new GGroupObject());

        int clientID = clientGroupObject.ID;
        groupObject.ID = clientID;
        groupObject.nativeSID = "g" + clientID;
        String clientSID = clientGroupObject.getSID();
        groupObject.sID = clientSID != null ? clientSID : "obj" + clientID;

        for (ClientObject clientObject : clientGroupObject.objects) {
            GObject object = convertOrCast(clientObject);
            groupObject.objects.add(object);
        }
        
        groupObject.filtersContainer = convertOrCast(clientGroupObject.filtersContainer);
        for (ClientFilter filter : clientGroupObject.filters) {
            groupObject.filters.add(convertOrCast(filter));
        }
        
        groupObject.grid = convertOrCast(clientGroupObject.grid);
        groupObject.toolbar = convertOrCast(clientGroupObject.toolbar);

        groupObject.viewType = GClassViewType.valueOf(clientGroupObject.viewType.name());
        groupObject.listViewType = GListViewType.valueOf(clientGroupObject.listViewType.name());
        groupObject.pivotOptions = convertOrCast(clientGroupObject.pivotOptions);
        groupObject.customRenderFunction = clientGroupObject.customRenderFunction;
        groupObject.mapTileProvider = clientGroupObject.mapTileProvider;

        groupObject.asyncInit = clientGroupObject.asyncInit;

        groupObject.isRecursive = clientGroupObject.isRecursive;
        groupObject.isMap = clientGroupObject.isMap;
        groupObject.isCalendarDate = clientGroupObject.isCalendarDate;
        groupObject.isCalendarDateTime = clientGroupObject.isCalendarDateTime;
        groupObject.isCalendarPeriod = clientGroupObject.isCalendarPeriod;
        groupObject.parent = convertOrCast(clientGroupObject.parent);

        groupObject.hasHeaders = clientGroupObject.hasHeaders;
        groupObject.hasFooters = clientGroupObject.hasFooters;

        for (ClientGroupObject clientUpGroup : clientGroupObject.upTreeGroups) {
            GGroupObject upGroup = convertOrCast(clientUpGroup);
            groupObject.upTreeGroups.add(upGroup);
        }

        groupObject.rowBackgroundReader = convertRowBackgroundReader(clientGroupObject.rowBackgroundReader);
        groupObject.rowForegroundReader = convertRowForegroundReader(clientGroupObject.rowForegroundReader);

        return groupObject;
    }

    @Cached
    @Converter(from = PivotOptions.class)
    public GPivotOptions convertPivotOptions(PivotOptions pivotOptions) {
        return new GPivotOptions(pivotOptions.getType(), convertGroupType(pivotOptions.getAggregation()), pivotOptions.getShowSettings(), pivotOptions.getConfigFunction());
    }

    @Cached
    @Converter(from = PropertyGroupType.class)
    public GPropertyGroupType convertGroupType(PropertyGroupType groupType) {
        return groupType != null ? GPropertyGroupType.valueOf(groupType.name()) : null;
    }

    @Cached
    @Converter(from = ClientObject.class)
    public GObject convertObject(ClientObject clientObject) {
        return new GObject(convertOrCast(clientObject.groupObject), clientObject.getCaption(), clientObject.ID, clientObject.getSID());
    }

    @Cached
    @Converter(from = ClientForm.class)
    public GForm convertForm(ClientForm clientForm) {
        GForm form = new GForm();
        this.form = form;

        form.creationPath = clientForm.creationPath;
        form.autoRefresh = clientForm.autoRefresh;
        GContainer mainContainer = convertOrCast(clientForm.mainContainer);
        mainContainer.main = true;
        form.mainContainer = mainContainer;

        for (ClientTreeGroup clientTreeGroup : clientForm.treeGroups) {
            GTreeGroup treeGroup = convertOrCast(clientTreeGroup);
            form.treeGroups.add(treeGroup);
        }

        for (ClientGroupObject clientGroup : clientForm.groupObjects) {
            GGroupObject group = convertOrCast(clientGroup);
            form.groupObjects.add(group);
        }

        for (ClientPropertyDraw clientProperty : clientForm.propertyDraws) {
            GPropertyDraw property = convertOrCast(clientProperty);
            form.propertyDraws.add(property);
        }

        for (ClientRegularFilterGroup clientFilterGroup : clientForm.regularFilterGroups) {
            GRegularFilterGroup filterGroup = convertOrCast(clientFilterGroup);
            form.regularFilterGroups.add(filterGroup);
        }

        for (ClientPropertyDraw property : clientForm.defaultOrders.keyList()) {
            form.defaultOrders.put((GPropertyDraw) convertOrCast(property), clientForm.defaultOrders.get(property));
        }

        form.pivotColumns.addAll(convertPivotPropertiesList(clientForm.pivotColumns));
        form.pivotRows.addAll(convertPivotPropertiesList(clientForm.pivotRows));
        for(ClientPropertyDraw property : clientForm.pivotMeasures) {
            form.pivotMeasures.add(convertOrCast(property));
        }

        return form;
    }

    private List<List<GPropertyDraw>> convertPivotPropertiesList(List<List<ClientPropertyDraw>> pivotPropertiesList) {
        List<List<GPropertyDraw>> gPivotPropertiesList = new ArrayList<>();
        for (List<ClientPropertyDraw> pivotPropertiesEntry : pivotPropertiesList) {
            List<GPropertyDraw> gPivotPropertiesEntry = new ArrayList<>();
            for(ClientPropertyDraw property : pivotPropertiesEntry) {
                gPivotPropertiesEntry.add(convertOrCast(property));
            }
            gPivotPropertiesList.add(gPivotPropertiesEntry);
        }
        return gPivotPropertiesList;
    }
}
