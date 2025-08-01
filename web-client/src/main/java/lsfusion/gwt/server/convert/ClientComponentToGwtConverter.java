package lsfusion.gwt.server.convert;

import lsfusion.client.classes.ClientActionClass;
import lsfusion.client.classes.data.ClientFileClass;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.filter.ClientRegularFilter;
import lsfusion.client.form.filter.ClientRegularFilterGroup;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.filter.user.ClientFilterControls;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.object.table.ClientToolbar;
import lsfusion.client.form.object.table.grid.ClientGrid;
import lsfusion.client.form.object.table.grid.user.toolbar.ClientCalculations;
import lsfusion.client.form.object.table.tree.ClientTreeGroup;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.async.ClientAsyncEventExec;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormEventClose;
import lsfusion.gwt.client.GFormScheduler;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.classes.GClass;
import lsfusion.gwt.client.classes.GInputType;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.filter.GRegularFilter;
import lsfusion.gwt.client.form.filter.GRegularFilterGroup;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.GCalculations;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.interop.form.event.*;
import lsfusion.interop.form.property.PivotOptions;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.interop.form.property.PropertyGroupType;

import java.io.IOException;
import java.util.*;

import static lsfusion.gwt.server.convert.StaticConverters.convertColor;

@SuppressWarnings("UnusedDeclaration")
public class ClientComponentToGwtConverter extends CachedFormObjectConverter {

    private final ClientTypeToGwtConverter typeConverter = ClientTypeToGwtConverter.getInstance();
    private final ClientBindingToGwtConverter bindingConverter = ClientBindingToGwtConverter.getInstance();
    private final ClientAsyncToGwtConverter asyncConverter;
    private GForm form;

    public ClientComponentToGwtConverter(MainDispatchServlet servlet, FormSessionObject formSessionObject) {
        super(servlet, formSessionObject);

        asyncConverter = new ClientAsyncToGwtConverter(servlet, formSessionObject);
    }

    private <T extends GComponent> T initGwtComponent(ClientComponent clientComponent, T component) {
        cacheInstance(clientComponent, component);

        component.ID = clientComponent.ID;
        component.sID = clientComponent.getSID();
        component.container = convertOrCast(clientComponent.container);
        component.defaultComponent = clientComponent.defaultComponent;

        component.elementClass = clientComponent.elementClass;

        component.width = clientComponent.width;
        component.height = clientComponent.height;

        component.span = clientComponent.span;

        component.setFlex(clientComponent.flex);
        component.setAlignment(convertFlexAlignment(clientComponent.alignment));
        component.shrink = clientComponent.shrink;
        component.alignShrink = clientComponent.alignShrink;
        component.alignCaption = clientComponent.alignCaption;
        component.overflowHorz = clientComponent.overflowHorz;
        component.overflowVert = clientComponent.overflowVert;

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

        component.captionVertical = clientComponent.captionVertical;
        component.captionLast = clientComponent.captionLast;
        component.captionAlignmentHorz = convertFlexAlignment(clientComponent.captionAlignmentHorz);
        component.captionAlignmentVert = convertFlexAlignment(clientComponent.captionAlignmentVert);

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
        return font;
    }

    @Cached
    @Converter(from = ClientContainer.class)
    public GContainer convertContainer(ClientContainer clientContainer) throws IOException {
        GContainer container = initGwtComponent(clientContainer,  new GContainer());

        container.nativeSID = "c" + clientContainer.ID;
        container.caption = clientContainer.caption;
        container.name = clientContainer.name;
        container.image = createImage(clientContainer.image, false);
        container.captionClass = clientContainer.captionClass;
        container.valueClass = clientContainer.valueClass;
        container.collapsible = clientContainer.collapsible;
        container.popup = clientContainer.popup;
        container.border = clientContainer.border;
        container.horizontal = clientContainer.horizontal;
        container.tabbed = clientContainer.tabbed;
        container.path = clientContainer.path;
        container.creationPath = clientContainer.creationPath;
        container.childrenAlignment = convertFlexAlignment(clientContainer.childrenAlignment);
        container.grid = clientContainer.grid;
        container.wrap = clientContainer.wrap;
        container.resizeOverflow = clientContainer.resizeOverflow;
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
        filterGroup.noNull = clientFilterGroup.noNull;

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
        if (clientFilter.keyInputEvent != null)
            filter.bindingEvents.add(bindingConverter.convertBinding(clientFilter.keyInputEvent));
        filter.showKey = clientFilter.showKey;
        if (clientFilter.mouseInputEvent != null)
            filter.bindingEvents.add(bindingConverter.convertBinding(clientFilter.mouseInputEvent));
        filter.showMouse = clientFilter.showMouse;
        return filter;
    }

    @Cached
    @Converter(from = ClientToolbar.class)
    public GToolbar convertToolbar(ClientToolbar clientToolbar) {
        GToolbar toolbar = initGwtComponent(clientToolbar, new GToolbar());
        toolbar.visible = clientToolbar.visible;

        toolbar.showViews = clientToolbar.showViews;
        toolbar.showFilters = clientToolbar.showFilters;
        toolbar.showSettings = clientToolbar.showSettings;
        toolbar.showCountQuantity = clientToolbar.showCountQuantity;
        toolbar.showCalculateSum = clientToolbar.showCalculateSum;
        toolbar.showPrintGroupXls = clientToolbar.showPrintGroupXls;
        toolbar.showManualUpdate = clientToolbar.showManualUpdate;
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
    @Converter(from = ClientFilterControls.class)
    public GFilterControls convertFilterControls(ClientFilterControls clientControls) {
        return initGwtComponent(clientControls, new GFilterControls());
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

        grid.valueClass = clientGrid.valueClass;

        grid.resizeOverflow = clientGrid.resizeOverflow;

        grid.captionHeight = clientGrid.captionHeight;
        grid.captionCharHeight = clientGrid.captionCharHeight;

        grid.boxed = clientGrid.boxed;

        grid.lineWidth = clientGrid.lineWidth;
        grid.lineHeight = clientGrid.lineHeight;

        grid.record = convertOrCast(clientGrid.record);

        return grid;
    }

    @Cached
    @Converter(from = ClientPropertyDraw.class)
    public GPropertyDraw convertPropertyDraw(ClientPropertyDraw clientPropertyDraw) throws IOException {
        GPropertyDraw propertyDraw = initGwtComponent(clientPropertyDraw, new GPropertyDraw());

        propertyDraw.ID = clientPropertyDraw.ID;
        propertyDraw.nativeSID = "p" + clientPropertyDraw.ID;
        propertyDraw.sID = clientPropertyDraw.getSID();
        propertyDraw.namespace = clientPropertyDraw.getNamespace();
        propertyDraw.caption = clientPropertyDraw.caption;
        propertyDraw.canonicalName = clientPropertyDraw.getCanonicalName();
        propertyDraw.propertyFormName = clientPropertyDraw.getPropertyFormName();
        propertyDraw.integrationSID = clientPropertyDraw.getIntegrationSID();

        propertyDraw.customRenderFunction = clientPropertyDraw.customRenderFunction;
        propertyDraw.customCanBeRenderedInTD = clientPropertyDraw.customCanBeRenderedInTD;
        propertyDraw.customNeedPlaceholder = clientPropertyDraw.customNeedPlaceholder;
        propertyDraw.customNeedReadonly = clientPropertyDraw.customNeedReadonly;
        
        propertyDraw.wrap = clientPropertyDraw.wrap;
        propertyDraw.wrapWordBreak = clientPropertyDraw.wrapWordBreak;
        propertyDraw.collapse = clientPropertyDraw.collapse;
        propertyDraw.ellipsis = clientPropertyDraw.ellipsis;

        propertyDraw.captionWrap = clientPropertyDraw.captionWrap;
        propertyDraw.captionWrapWordBreak = clientPropertyDraw.captionWrapWordBreak;
        propertyDraw.captionCollapse = clientPropertyDraw.captionCollapse;
        propertyDraw.captionEllipsis = clientPropertyDraw.captionEllipsis;

        propertyDraw.clearText = clientPropertyDraw.clearText;
        propertyDraw.notSelectAll = clientPropertyDraw.notSelectAll;
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

        propertyDraw.cellType = typeConverter.convertOrCast(clientPropertyDraw.baseType);
        propertyDraw.differentValue = clientPropertyDraw.valueType != null;
        propertyDraw.valueType = propertyDraw.differentValue ? typeConverter.convertOrCast(clientPropertyDraw.valueType) : propertyDraw.cellType;
        propertyDraw.returnClass = typeConverter.convertOrCast(clientPropertyDraw.returnClass);

        propertyDraw.tag = clientPropertyDraw.tag;
        propertyDraw.inputType = clientPropertyDraw.inputType != null ? new GInputType(clientPropertyDraw.inputType) : null;
        propertyDraw.valueElementClass = clientPropertyDraw.valueElementClass;
        propertyDraw.captionElementClass = clientPropertyDraw.captionElementClass;
        propertyDraw.toolbar = clientPropertyDraw.toolbar;
        propertyDraw.toolbarActions = clientPropertyDraw.toolbarActions;

        propertyDraw.externalChangeType = typeConverter.convertOrCast(clientPropertyDraw.externalChangeType);
        propertyDraw.asyncExecMap = new HashMap<>();
        for(Map.Entry<String, ClientAsyncEventExec> entry : clientPropertyDraw.asyncExecMap.entrySet()) {
            propertyDraw.asyncExecMap.put(entry.getKey(), asyncConverter.convertOrCast(entry.getValue()));
        }

        propertyDraw.ignoreHasHeaders = clientPropertyDraw.ignoreHasHeaders;

        propertyDraw.askConfirm = clientPropertyDraw.askConfirm;
        propertyDraw.askConfirmMessage = clientPropertyDraw.askConfirmMessage;

        propertyDraw.hasEditObjectAction = clientPropertyDraw.hasEditObjectAction;
        propertyDraw.hasChangeAction = clientPropertyDraw.hasChangeAction;
        propertyDraw.hasUserChangeAction = clientPropertyDraw.hasUserChangeAction;

        propertyDraw.disableInputList = clientPropertyDraw.disableInputList;

        propertyDraw.editBindingMap = clientPropertyDraw.editBindingMap != null ? bindingConverter.convertBindingMap(clientPropertyDraw.editBindingMap) : null;

        boolean canIconBeDisabled = clientPropertyDraw.baseType instanceof ClientActionClass || clientPropertyDraw.baseType instanceof ClientFileClass;
        propertyDraw.appImage = createImage(clientPropertyDraw.image, canIconBeDisabled);

        propertyDraw.editType = convertOrCast(clientPropertyDraw.editType);

        propertyDraw.echoSymbols = clientPropertyDraw.echoSymbols;

        propertyDraw.noSort = clientPropertyDraw.noSort;
        if(clientPropertyDraw.defaultCompare != null)
            propertyDraw.defaultCompare = GCompare.get(clientPropertyDraw.defaultCompare.ordinal());

        if(clientPropertyDraw.changeKey != null)
            propertyDraw.bindingEvents.add(bindingConverter.convertBinding(clientPropertyDraw.changeKey));
        propertyDraw.showChangeKey = clientPropertyDraw.showChangeKey;
        if(clientPropertyDraw.changeMouse != null)
            propertyDraw.bindingEvents.add(bindingConverter.convertBinding(clientPropertyDraw.changeMouse));
        propertyDraw.showChangeMouse = clientPropertyDraw.showChangeMouse;

        propertyDraw.inline = clientPropertyDraw.inline;
        propertyDraw.isList = clientPropertyDraw.isList;

        propertyDraw.drawAsync = clientPropertyDraw.drawAsync;

        propertyDraw.focusable = clientPropertyDraw.focusable;
        propertyDraw.checkEquals = clientPropertyDraw.checkEquals;

        propertyDraw.captionReader = convertCaptionReader(clientPropertyDraw.captionReader);
        propertyDraw.loadingReader = new GLoadingReader(clientPropertyDraw.getID(), clientPropertyDraw.getGroupObject() != null ? clientPropertyDraw.getGroupObject().ID : -1);
        propertyDraw.showIfReader = convertShowIfReader(clientPropertyDraw.showIfReader);
        propertyDraw.footerReader = convertFooterReader(clientPropertyDraw.footerReader);
        propertyDraw.readOnlyReader = convertReadOnlyReader(clientPropertyDraw.readOnlyReader);
        propertyDraw.gridElementClassReader = convertGridElementClassReader(clientPropertyDraw.gridElementClassReader);
        propertyDraw.valueElementClassReader = convertValueElementClassReader(clientPropertyDraw.valueElementClassReader);
        propertyDraw.captionElementClassReader = convertCaptionElementClassReader(clientPropertyDraw.captionElementClassReader);
        propertyDraw.fontReader = convertExtraPropReader(clientPropertyDraw.fontReader);
        propertyDraw.backgroundReader = convertBackgroundReader(clientPropertyDraw.backgroundReader);
        propertyDraw.foregroundReader = convertForegroundReader(clientPropertyDraw.foregroundReader);
        propertyDraw.imageReader = convertImageReader(clientPropertyDraw.imageReader);
        propertyDraw.hasDynamicImage = clientPropertyDraw.hasDynamicImage;
        propertyDraw.hasDynamicCaption = clientPropertyDraw.hasDynamicCaption;
        propertyDraw.commentReader = convertExtraPropReader(clientPropertyDraw.commentReader);
        propertyDraw.commentElementClassReader = convertExtraPropReader(clientPropertyDraw.commentElementClassReader);
        propertyDraw.placeholderReader = convertExtraPropReader(clientPropertyDraw.placeholderReader);
        propertyDraw.patternReader = convertExtraPropReader(clientPropertyDraw.patternReader);
        propertyDraw.regexpReader = convertExtraPropReader(clientPropertyDraw.regexpReader);
        propertyDraw.regexpMessageReader = convertExtraPropReader(clientPropertyDraw.regexpMessageReader);
        propertyDraw.tooltipReader = convertExtraPropReader(clientPropertyDraw.tooltipReader);
        propertyDraw.valueTooltipReader = convertExtraPropReader(clientPropertyDraw.valueTooltipReader);
        propertyDraw.propertyCustomOptionsReader = convertExtraPropReader(clientPropertyDraw.propertyCustomOptionsReader);
        propertyDraw.changeKeyReader = convertExtraPropReader(clientPropertyDraw.changeKeyReader);
        propertyDraw.changeMouseReader = convertExtraPropReader(clientPropertyDraw.changeMouseReader);

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
        propertyDraw.captionCharHeight = clientPropertyDraw.captionCharHeight;

        propertyDraw.panelColumnVertical = clientPropertyDraw.panelColumnVertical;
        propertyDraw.panelCustom = clientPropertyDraw.panelCustom;

        propertyDraw.valueAlignmentHorz = convertFlexAlignment(clientPropertyDraw.valueAlignmentHorz);
        propertyDraw.valueAlignmentVert = convertFlexAlignment(clientPropertyDraw.valueAlignmentVert);

        propertyDraw.highlightDuplicateValue = clientPropertyDraw.highlightDuplicateValue;

        propertyDraw.valueOverflowHorz = clientPropertyDraw.valueOverflowHorz;
        propertyDraw.valueOverflowVert = clientPropertyDraw.valueOverflowVert;

        propertyDraw.valueShrinkHorz = clientPropertyDraw.valueShrinkHorz;
        propertyDraw.valueShrinkVert = clientPropertyDraw.valueShrinkVert;

        propertyDraw.comment = clientPropertyDraw.comment;
        propertyDraw.commentElementClass = clientPropertyDraw.commentElementClass;
        propertyDraw.panelCommentVertical = clientPropertyDraw.panelCommentVertical;
        propertyDraw.panelCommentFirst = clientPropertyDraw.panelCommentFirst;
        propertyDraw.panelCommentAlignment = convertFlexAlignment(clientPropertyDraw.panelCommentAlignment);

        propertyDraw.placeholder = clientPropertyDraw.placeholder;
        propertyDraw.pattern = clientPropertyDraw.pattern;
        propertyDraw.regexp = clientPropertyDraw.regexp;
        propertyDraw.regexpMessage = clientPropertyDraw.regexpMessage;

        propertyDraw.tooltip = clientPropertyDraw.tooltip;
        propertyDraw.valueTooltip = clientPropertyDraw.valueTooltip;

        propertyDraw.changeOnSingleClick = clientPropertyDraw.changeOnSingleClick;
        
        propertyDraw.hide = clientPropertyDraw.hide;
        propertyDraw.remove = clientPropertyDraw.remove;

        propertyDraw.notNull = clientPropertyDraw.notNull;

        propertyDraw.sticky = clientPropertyDraw.sticky;

        propertyDraw.hasFooter = clientPropertyDraw.hasFooter;

//        propertyDraw.getValueWidth(null, form); // parentFont - null потому как на этом этапе интересуют только в панели свойства (а parentFont для грида, там своя ветка)

        return propertyDraw;
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

    public GGridElementClassReader convertGridElementClassReader(ClientPropertyDraw.GridElementClassReader reader) {
        return reader == null ? null : new GGridElementClassReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GValueElementClassReader convertValueElementClassReader(ClientPropertyDraw.ValueElementClassReader reader) {
        return reader == null ? null : new GValueElementClassReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GCaptionElementClassReader convertCaptionElementClassReader(ClientPropertyDraw.CaptionElementClassReader reader) {
        return reader == null ? null : new GCaptionElementClassReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
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

    public GCustomOptionsReader convertCustomOptionsReader(ClientGroupObject.CustomOptionsReader reader) {
        return reader == null ? null : new GCustomOptionsReader(reader.getID());
    }

    public GExtraPropReader convertExtraPropReader(ClientPropertyDraw.ExtraPropReader reader) {
        return reader == null ? null : new GExtraPropReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1, reader.getType());
    }

    @Cached
    @Converter(from = PropertyEditType.class)
    public GPropertyEditType convertEditType(PropertyEditType editType) {
        switch (editType) {
            case EDITABLE: return GPropertyEditType.EDITABLE;
            case READONLY: return GPropertyEditType.READONLY;
            case DISABLE: return GPropertyEditType.DISABLE;
        }
        return null;
    }

    @Cached
    @Converter(from = ClientTreeGroup.class)
    public GTreeGroup convertTreeGroup(ClientTreeGroup clientTreeGroup) {
        GTreeGroup treeGroup = initGwtComponent(clientTreeGroup, new GTreeGroup());
        
        treeGroup.filtersContainer = convertOrCast(clientTreeGroup.filtersContainer);
        treeGroup.filterControls = convertOrCast(clientTreeGroup.filterControls);
        for (ClientFilter filter : clientTreeGroup.filters) {
            treeGroup.filters.add(convertOrCast(filter));
        }

        treeGroup.boxed = clientTreeGroup.boxed;

        treeGroup.valueClass = clientTreeGroup.valueClass;

        treeGroup.toolbar = convertOrCast(clientTreeGroup.toolbar);
        
        treeGroup.expandOnClick = clientTreeGroup.expandOnClick;
        treeGroup.hierarchicalWidth = clientTreeGroup.hierarchicalWidth;
        treeGroup.hierarchicalCaption = clientTreeGroup.hierarchicalCaption;

        treeGroup.resizeOverflow = clientTreeGroup.resizeOverflow;

        treeGroup.captionHeight = clientTreeGroup.captionHeight;
        treeGroup.captionCharHeight = clientTreeGroup.captionCharHeight;

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
        groupObject.filtersControls = convertOrCast(clientGroupObject.filterControls);
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

        groupObject.enableManualUpdate = clientGroupObject.enableManualUpdate;

        for (ClientGroupObject clientUpGroup : clientGroupObject.upTreeGroups) {
            GGroupObject upGroup = convertOrCast(clientUpGroup);
            groupObject.upTreeGroups.add(upGroup);
        }

        groupObject.rowBackgroundReader = convertRowBackgroundReader(clientGroupObject.rowBackgroundReader);
        groupObject.rowForegroundReader = convertRowForegroundReader(clientGroupObject.rowForegroundReader);
        groupObject.customOptionsReader = convertCustomOptionsReader(clientGroupObject.customOptionsReader);

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
        form.path = clientForm.path;
        for(FormScheduler formScheduler : clientForm.formSchedulers) {
            form.formSchedulers.add(convertOrCast(formScheduler));
        }
        for(Map.Entry<FormEvent, ClientAsyncEventExec> asyncExec : clientForm.asyncExecMap.entrySet()) {
            form.asyncExecMap.put(convertOrCast(asyncExec.getKey()), asyncConverter.convertOrCast(asyncExec.getValue()));
        }

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

        form.inputGroupObjects = new HashSet<>();
        for (ClientGroupObject groupObject : clientForm.inputGroupObjects) {
            form.inputGroupObjects.add(convertOrCast(groupObject));
        }

        return form;
    }

    private ArrayList<ArrayList<GPropertyDraw>> convertPivotPropertiesList(List<List<ClientPropertyDraw>> pivotPropertiesList) {
        ArrayList<ArrayList<GPropertyDraw>> gPivotPropertiesList = new ArrayList<>();
        for (List<ClientPropertyDraw> pivotPropertiesEntry : pivotPropertiesList) {
            ArrayList<GPropertyDraw> gPivotPropertiesEntry = new ArrayList<>();
            for(ClientPropertyDraw property : pivotPropertiesEntry) {
                gPivotPropertiesEntry.add(convertOrCast(property));
            }
            gPivotPropertiesList.add(gPivotPropertiesEntry);
        }
        return gPivotPropertiesList;
    }

    @Converter(from = FormScheduler.class)
    public GFormScheduler convertAction(FormScheduler scheduler) {
        return new GFormScheduler(scheduler.period, scheduler.fixed);
    }

    @Converter(from = FormEventClose.class)
    public GFormEventClose convertAction(FormEventClose formEventClose) {
        return new GFormEventClose(formEventClose.ok);
    }
}
