package lsfusion.gwt.server.form.convert;

import com.google.gwt.event.dom.client.KeyCodes;
import lsfusion.client.form.EditBindingMap;
import lsfusion.client.logics.*;
import lsfusion.client.logics.classes.ClientActionClass;
import lsfusion.client.logics.classes.ClientFileClass;
import lsfusion.gwt.client.base.ui.GAlignment;
import lsfusion.gwt.client.base.ui.GFlexAlignment;
import lsfusion.gwt.client.base.ui.GKeyStroke;
import lsfusion.gwt.server.form.FileUtils;
import lsfusion.gwt.shared.form.view.*;
import lsfusion.gwt.shared.form.view.classes.GClass;
import lsfusion.gwt.shared.form.view.filter.GCompare;
import lsfusion.gwt.shared.form.view.reader.*;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.FontInfo;
import lsfusion.interop.PropertyEditType;
import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.ContainerType;
import lsfusion.interop.form.layout.FlexAlignment;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static lsfusion.gwt.server.form.convert.StaticConverters.convertColor;

@SuppressWarnings("UnusedDeclaration")
public class ClientComponentToGwtConverter extends CachedObjectConverter {

    private final ClientTypeToGwtConverter typeConverter = ClientTypeToGwtConverter.getInstance();
    private GForm form;

    public ClientComponentToGwtConverter() {
    }

    private <T extends GComponent> T initGwtComponent(ClientComponent clientComponent, T component) {
        cacheInstance(clientComponent, component);

        component.ID = clientComponent.ID;
        component.sID = clientComponent.getSID();
        component.container = convertOrCast(clientComponent.container);
        component.defaultComponent = clientComponent.defaultComponent;

        if (clientComponent.size != null) {
            component.width = clientComponent.size.width;
            component.height = clientComponent.size.height;
        }
        
        component.autoSize = clientComponent.autoSize;

        component.setFlex(clientComponent.flex);
        component.setAlignment(clientComponent.alignment == null ? null : convertFlexAlignment(clientComponent.alignment));
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

    private GAlignment convertAlignment(Alignment alignment) {
        switch (alignment) {
            case START: return GAlignment.START;
            case CENTER: return GAlignment.CENTER;
            case END: return GAlignment.END;
        }
        throw new IllegalStateException("Unknown alignment");
    }

    private GFlexAlignment convertFlexAlignment(FlexAlignment alignment) {
        switch (alignment) {
            case START: return GFlexAlignment.START;
            case CENTER: return GFlexAlignment.CENTER;
            case END: return GFlexAlignment.END;
            case STRETCH: return GFlexAlignment.STRETCH;
        }
        throw new IllegalStateException("Unknown alignment");
    }

    public GFont convertFont(FontInfo clientFont) {
        GFont font = StaticConverters.convertFont(clientFont);
        if (font == null) {
            return null;
        }
        if (font.size <= 0) {
            font.size = GFont.DEFAULT_FONT_SIZE;
        }
        form.addFont(font);
        return font;
    }

    private GContainerType convertContainerType(ContainerType containerType) {
        switch (containerType) {
            case CONTAINERH: return GContainerType.CONTAINERH;
            case CONTAINERV: return GContainerType.CONTAINERV;
            case COLUMNS: return GContainerType.COLUMNS;
            case TABBED_PANE: return GContainerType.TABBED_PANE;
            case VERTICAL_SPLIT_PANE: return GContainerType.VERTICAL_SPLIT_PANE;
            case HORIZONTAL_SPLIT_PANE: return GContainerType.HORIZONTAL_SPLIT_PANE;
            case SCROLL: return GContainerType.SCROLL;
            case FLOW:
                throw new IllegalStateException("FLOW container isn't yet supported");
        }
        throw new IllegalStateException("Unknown container type");
    }

    @Cached
    @Converter(from = ClientContainer.class)
    public GContainer convertContainer(ClientContainer clientContainer) {
        GContainer container = initGwtComponent(clientContainer,  new GContainer());

        container.caption = clientContainer.getRawCaption();
        container.type = convertContainerType(clientContainer.getType());
        container.childrenAlignment = convertAlignment(clientContainer.childrenAlignment);
        container.columns = clientContainer.columns;

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
    @Converter(from = ClientShowType.class)
    public GShowType convertShowType(ClientShowType clientShowType) {
        GShowType showType = initGwtComponent(clientShowType, new GShowType());
        showType.groupObject = convertOrCast(clientShowType.groupObject);
        return showType;
    }

    @Cached
    @Converter(from = ClientToolbar.class)
    public GToolbar convertToolbar(ClientToolbar clientToolbar) {
        GToolbar toolbar = initGwtComponent(clientToolbar, new GToolbar());
        toolbar.visible = clientToolbar.visible;
        toolbar.showGroupChange = clientToolbar.showGroupChange;
        toolbar.showCountQuantity = clientToolbar.showCountRows;
        toolbar.showCalculateSum = clientToolbar.showCalculateSum;
        toolbar.showGroup = clientToolbar.showGroupReport;
        toolbar.showPrintGroup = clientToolbar.showPrint;
        toolbar.showPrintGroupXls = clientToolbar.showXls;
        toolbar.showGridSettings = clientToolbar.showSettings;
        return toolbar;
    }

    @Cached
    @Converter(from = ClientFilter.class)
    public GFilter convertFilter(ClientFilter clientFilter) {
        GFilter filter = initGwtComponent(clientFilter, new GFilter());
        filter.visible = clientFilter.visible;
        return filter;
    }
    
    @Cached
    @Converter(from = ClientCalculations.class)
    public GCalculations convertCalculations(ClientCalculations clientCalculations) {
        return initGwtComponent(clientCalculations, new GCalculations());
    }

    @Cached
    @Converter(from = ClientClassChooser.class)
    public GComponent convertClassChooser(ClientClassChooser clientClassChooser) {
        return initGwtComponent(clientClassChooser, new GComponent());
    }

    @Cached
    @Converter(from = ClientGrid.class)
    public GGrid convertGrid(ClientGrid clientGrid) {
        GGrid grid = initGwtComponent(clientGrid, new GGrid());
        grid.groupObject = convertOrCast(clientGrid.groupObject);
        grid.quickSearch = clientGrid.quickSearch;
        grid.headerHeight = clientGrid.headerHeight;
        return grid;
    }

    @Cached
    @Converter(from = ClientPropertyDraw.class)
    public GPropertyDraw convertPropertyDraw(ClientPropertyDraw clientPropertyDraw) {
        GPropertyDraw propertyDraw = initGwtComponent(clientPropertyDraw, new GPropertyDraw());

        propertyDraw.ID = clientPropertyDraw.ID;
        propertyDraw.sID = clientPropertyDraw.getSID();
        propertyDraw.namespace = clientPropertyDraw.getNamespace();
        propertyDraw.caption = clientPropertyDraw.caption;
        propertyDraw.canonicalName = clientPropertyDraw.getCanonicalName();
        propertyDraw.propertyFormName = clientPropertyDraw.getPropertyFormName();

        propertyDraw.toolTip = clientPropertyDraw.toolTip;
        propertyDraw.tableName = clientPropertyDraw.tableName;
        propertyDraw.interfacesCaptions = clientPropertyDraw.interfacesCaptions;
        propertyDraw.interfacesTypes = new GClass[clientPropertyDraw.interfacesTypes.length];
        for (int i = 0; i < clientPropertyDraw.interfacesTypes.length; i++) {
            propertyDraw.interfacesTypes[i] = typeConverter.convertOrCast(clientPropertyDraw.interfacesTypes[i]);
        }
        propertyDraw.creationScript = clientPropertyDraw.creationScript;
        propertyDraw.creationPath = clientPropertyDraw.creationPath;
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
        propertyDraw.changeType = typeConverter.convertOrCast(clientPropertyDraw.changeType);
        propertyDraw.changeWYSType = typeConverter.convertOrCast(clientPropertyDraw.changeWYSType);
        propertyDraw.returnClass = typeConverter.convertOrCast(clientPropertyDraw.returnClass);

        if (clientPropertyDraw.addRemove != null) {
            GObject addRemoveObject = convertOrCast(clientPropertyDraw.addRemove.first);
            propertyDraw.addRemove = new GPropertyDraw.AddRemove(addRemoveObject, clientPropertyDraw.addRemove.second);
        }

        propertyDraw.askConfirm = clientPropertyDraw.askConfirm;
        propertyDraw.askConfirmMessage = clientPropertyDraw.askConfirmMessage;

        propertyDraw.hasEditObjectAction = clientPropertyDraw.hasEditObjectAction;
        propertyDraw.hasChangeAction = clientPropertyDraw.hasChangeAction;

        propertyDraw.editBindingMap = convertOrCast(clientPropertyDraw.editBindingMap);

        boolean canIconBeDisabled = clientPropertyDraw.baseType instanceof ClientActionClass || clientPropertyDraw.baseType instanceof ClientFileClass;
        propertyDraw.icon = FileUtils.createImage(clientPropertyDraw.design.getImageHolder(), clientPropertyDraw.design.imagePath, "property", canIconBeDisabled);

        propertyDraw.editType = convertOrCast(clientPropertyDraw.editType);

        propertyDraw.echoSymbols = clientPropertyDraw.echoSymbols;

        propertyDraw.noSort = clientPropertyDraw.noSort;
        if(clientPropertyDraw.defaultCompare != null)
            propertyDraw.defaultCompare = GCompare.get(clientPropertyDraw.defaultCompare.ordinal());

        propertyDraw.editKey = convertOrCast(clientPropertyDraw.editKey);
        propertyDraw.showEditKey = clientPropertyDraw.showEditKey;

        propertyDraw.drawAsync = clientPropertyDraw.drawAsync;

        propertyDraw.pattern = clientPropertyDraw.getFormatPattern();
        propertyDraw.defaultPattern = propertyDraw.pattern;

        propertyDraw.focusable = clientPropertyDraw.focusable == null || clientPropertyDraw.focusable;
        propertyDraw.checkEquals = clientPropertyDraw.checkEquals;

        propertyDraw.captionReader = convertCaptionReader(clientPropertyDraw.captionReader);
        propertyDraw.showIfReader = convertShowIfReader(clientPropertyDraw.showIfReader);
        propertyDraw.footerReader = convertFooterReader(clientPropertyDraw.footerReader);
        propertyDraw.readOnlyReader = convertReadOnlyReader(clientPropertyDraw.readOnlyReader);
        propertyDraw.backgroundReader = convertBackgroundReader(clientPropertyDraw.backgroundReader);
        propertyDraw.foregroundReader = convertForegroundReader(clientPropertyDraw.foregroundReader);

        propertyDraw.quickFilterProperty = convertOrCast(clientPropertyDraw.quickFilterProperty);

        propertyDraw.charWidth = clientPropertyDraw.charWidth;

        if (clientPropertyDraw.valueSize != null) {
            propertyDraw.valueWidth = clientPropertyDraw.valueSize.width;
            propertyDraw.valueHeight = clientPropertyDraw.valueSize.height;
        }

        propertyDraw.panelCaptionAbove = clientPropertyDraw.panelCaptionAbove;
        
        propertyDraw.hide = clientPropertyDraw.hide;
        
        propertyDraw.notNull = clientPropertyDraw.notNull;

//        propertyDraw.getValueWidth(null, form); // parentFont - null потому как на этом этапе интересуют только в панели свойства (а parentFont для грида, там своя ветка)

        return propertyDraw;
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

    public GBackgroundReader convertBackgroundReader(ClientPropertyDraw.BackgroundReader reader) {
        return reader == null ? null : new GBackgroundReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GForegroundReader convertForegroundReader(ClientPropertyDraw.ForegroundReader reader) {
        return reader == null ? null : new GForegroundReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
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
    @Converter(from = ClientTreeGroup.class)
    public GTreeGroup convertTreeGroup(ClientTreeGroup clientTreeGroup) {
        GTreeGroup treeGroup = initGwtComponent(clientTreeGroup, new GTreeGroup());

        treeGroup.toolbar = convertOrCast(clientTreeGroup.toolbar);
        treeGroup.filter = convertOrCast(clientTreeGroup.filter);
        
        treeGroup.expandOnClick = clientTreeGroup.expandOnClick;

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

        groupObject.ID = clientGroupObject.ID;
        groupObject.sID = clientGroupObject.getSID();

        for (ClientObject clientObject : clientGroupObject.objects) {
            GObject object = convertOrCast(clientObject);
            groupObject.objects.add(object);
        }
        groupObject.grid = convertOrCast(clientGroupObject.grid);
        groupObject.showType = convertOrCast(clientGroupObject.showType);
        groupObject.toolbar = convertOrCast(clientGroupObject.toolbar);
        groupObject.filter = convertOrCast(clientGroupObject.filter);
        groupObject.banClassView = new ArrayList<>();
        for (ClassViewType banView : clientGroupObject.banClassView) {
            groupObject.banClassView.add(banView.name());
        }

        groupObject.isRecursive = clientGroupObject.isRecursive;
        groupObject.parent = convertOrCast(clientGroupObject.parent);

        for (ClientGroupObject clientUpGroup : clientGroupObject.upTreeGroups) {
            GGroupObject upGroup = convertOrCast(clientUpGroup);
            groupObject.upTreeGroups.add(upGroup);
        }

        groupObject.rowBackgroundReader = convertRowBackgroundReader(clientGroupObject.rowBackgroundReader);
        groupObject.rowForegroundReader = convertRowForegroundReader(clientGroupObject.rowForegroundReader);

        return groupObject;
    }

    @Cached
    @Converter(from = ClientObject.class)
    public GObject convertObject(ClientObject clientObject) {
        GObject object = new GObject();
        object.ID = clientObject.ID;
        object.sID = clientObject.getSID();
        object.groupObject = convertOrCast(clientObject.groupObject);
        object.caption = clientObject.getCaption();
        return object;
    }

    @Cached
    @Converter(from = ClientForm.class)
    public GForm convertForm(ClientForm clientForm) {
        GForm form = new GForm();
        this.form = form;

        form.caption = clientForm.caption;
        form.creationPath = clientForm.creationPath;
        form.autoRefresh = clientForm.autoRefresh;
        form.mainContainer = convertOrCast(clientForm.mainContainer);

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

        return form;
    }
}
