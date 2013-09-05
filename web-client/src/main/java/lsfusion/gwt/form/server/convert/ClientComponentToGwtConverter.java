package lsfusion.gwt.form.server.convert;

import com.google.gwt.event.dom.client.KeyCodes;
import lsfusion.client.form.EditBindingMap;
import lsfusion.client.logics.*;
import lsfusion.client.logics.classes.ClientActionClass;
import lsfusion.client.logics.classes.ClientFileClass;
import lsfusion.gwt.base.client.ui.GAlignment;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.server.FileUtils;
import lsfusion.gwt.form.shared.view.*;
import lsfusion.gwt.form.shared.view.changes.dto.ColorDTO;
import lsfusion.gwt.form.shared.view.classes.GClass;
import lsfusion.gwt.form.shared.view.reader.*;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.PropertyEditType;
import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.ContainerType;
import lsfusion.interop.form.layout.FlexAlignment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

        if (clientComponent.minimumSize != null) {
            component.minimumWidth = clientComponent.minimumSize.width;
            component.minimumHeight = clientComponent.minimumSize.height;
        }

        if (clientComponent.maximumSize != null) {
            component.maximumWidth = clientComponent.maximumSize.width;
            component.maximumHeight = clientComponent.maximumSize.height;
        }

        if (clientComponent.preferredSize != null) {
            component.preferredWidth = clientComponent.preferredSize.width;
            component.preferredHeight = clientComponent.preferredSize.height;
        }

        component.flex = clientComponent.flex;
        component.alignment = convertFlexAlignment(clientComponent.alignment);

        if (clientComponent.design.getBackground() != null) {
            component.background = new ColorDTO(Integer.toHexString(clientComponent.design.getBackground().getRGB()).substring(2, 8));
        }

        if (clientComponent.design.getForeground() != null) {
            component.foreground = new ColorDTO(Integer.toHexString(clientComponent.design.getForeground().getRGB()).substring(2, 8));
        }

        Font clientFont = clientComponent.design.getFont();
        if (clientFont != null) {
            component.font = convertFont(clientFont);
        }

        Font headerFont = clientComponent.design.getHeaderFont();
        if (headerFont != null) {
            component.headerFont = convertFont(headerFont);
        }

        return component;
    }

    private GAlignment convertAlignment(Alignment alignment) {
        switch (alignment) {
            case LEADING: return GAlignment.LEADING;
            case CENTER: return GAlignment.CENTER;
            case TRAILING: return GAlignment.TRAILING;
        }
        throw new IllegalStateException("Unknown alignment");
    }

    private GFlexAlignment convertFlexAlignment(FlexAlignment alignment) {
        switch (alignment) {
            case LEADING: return GFlexAlignment.LEADING;
            case CENTER: return GFlexAlignment.CENTER;
            case TRAILING: return GFlexAlignment.TRAILING;
            case STRETCH: return GFlexAlignment.STRETCH;
        }
        throw new IllegalStateException("Unknown alignment");
    }

    private GFont convertFont(Font headerFont) {
        GFont font = new GFont(
                ((headerFont.getStyle() & Font.ITALIC) != 0 ? GFont.ITALIC : null),
                ((headerFont.getStyle() & Font.BOLD) != 0 ? GFont.BOLD : null),
                headerFont.getSize(),
                headerFont.getFamily()
        );
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
            case SCROLL:
                throw new IllegalStateException("SCROLL container isn't yet supported");
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
        container.gapX = clientContainer.gapX;
        container.gapY = clientContainer.gapY;
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
        toolbar.showPrintGroupButton = clientToolbar.showPrint;
        toolbar.showPrintGroupXlsButton = clientToolbar.showXls;
        toolbar.showHideSettings = clientToolbar.showSettings;
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
    @Converter(from = ClientClassChooser.class)
    public GComponent convertClassChooser(ClientClassChooser clientClassChooser) {
        return initGwtComponent(clientClassChooser, new GComponent());
    }

    @Cached
    @Converter(from = ClientGrid.class)
    public GGrid convertGrid(ClientGrid clientGrid) {
        GGrid grid = initGwtComponent(clientGrid, new GGrid());
        grid.groupObject = convertOrCast(clientGrid.groupObject);
        return grid;
    }

    @Cached
    @Converter(from = ClientPropertyDraw.class)
    public GPropertyDraw convertPropertyDraw(ClientPropertyDraw clientPropertyDraw) {
        GPropertyDraw propertyDraw = initGwtComponent(clientPropertyDraw, new GPropertyDraw());

        propertyDraw.ID = clientPropertyDraw.ID;
        propertyDraw.sID = clientPropertyDraw.getSID();
        propertyDraw.caption = clientPropertyDraw.caption;

        propertyDraw.toolTip = clientPropertyDraw.toolTip;
        propertyDraw.tableName = clientPropertyDraw.tableName;
        propertyDraw.interfacesCaptions = clientPropertyDraw.interfacesCaptions;
        propertyDraw.interfacesTypes = new GClass[clientPropertyDraw.interfacesTypes.length];
        for (int i = 0; i < clientPropertyDraw.interfacesTypes.length; i++) {
            propertyDraw.interfacesTypes[i] = typeConverter.convertOrCast(clientPropertyDraw.interfacesTypes[i]);
        }
        propertyDraw.creationScript = clientPropertyDraw.creationScript;
        propertyDraw.creationPath = clientPropertyDraw.creationPath;

        propertyDraw.groupObject = convertOrCast(clientPropertyDraw.groupObject);
        if (!clientPropertyDraw.columnGroupObjects.isEmpty()) {
            propertyDraw.columnGroupObjects = new ArrayList<GGroupObject>();
            for (ClientGroupObject clientColumnGroup : clientPropertyDraw.columnGroupObjects) {
                GGroupObject columnGroup = convertOrCast(clientColumnGroup);
                propertyDraw.columnGroupObjects.add(columnGroup);
            }
        }

        propertyDraw.baseType = typeConverter.convertOrCast(clientPropertyDraw.baseType);
        propertyDraw.changeType = typeConverter.convertOrCast(clientPropertyDraw.changeType);
        propertyDraw.returnClass = typeConverter.convertOrCast(clientPropertyDraw.returnClass);

        if (clientPropertyDraw.addRemove != null) {
            GObject addRemoveObject = convertOrCast(clientPropertyDraw.addRemove.first);
            propertyDraw.addRemove = new GPropertyDraw.AddRemove(addRemoveObject, clientPropertyDraw.addRemove.second);
        }

        propertyDraw.askConfirm = clientPropertyDraw.askConfirm;
        propertyDraw.askConfirmMessage = clientPropertyDraw.askConfirmMessage;

        propertyDraw.editBindingMap = convertOrCast(clientPropertyDraw.editBindingMap);

        boolean canIconBeDisabled = clientPropertyDraw.baseType instanceof ClientActionClass || clientPropertyDraw.baseType instanceof ClientFileClass;
        propertyDraw.icon= FileUtils.createImage(clientPropertyDraw.design.getImageHolder(), clientPropertyDraw.design.iconPath, "property", canIconBeDisabled);

        propertyDraw.editType = convertOrCast(clientPropertyDraw.editType);

        propertyDraw.echoSymbols = clientPropertyDraw.echoSymbols;

        propertyDraw.editKey = convertOrCast(clientPropertyDraw.editKey);
        propertyDraw.showEditKey = clientPropertyDraw.showEditKey;

        propertyDraw.drawAsync = clientPropertyDraw.drawAsync;

        propertyDraw.focusable = clientPropertyDraw.focusable == null || clientPropertyDraw.focusable;
        propertyDraw.checkEquals = clientPropertyDraw.checkEquals;

        propertyDraw.captionReader = convertCaptionReader(clientPropertyDraw.captionReader);
        propertyDraw.showIfReader = convertShowIfReader(clientPropertyDraw.showIfReader);
        propertyDraw.footerReader = convertFooterReader(clientPropertyDraw.footerReader);
        propertyDraw.readOnlyReader = convertReadOnlyReader(clientPropertyDraw.readOnlyReader);
        propertyDraw.backgroundReader = convertBackgroundReader(clientPropertyDraw.backgroundReader);
        propertyDraw.foregroundReader = convertForegroundReader(clientPropertyDraw.foregroundReader);

        propertyDraw.quickFilterProperty = convertOrCast(clientPropertyDraw.quickFilterProperty);

        propertyDraw.minimumCharWidth = clientPropertyDraw.minimumCharWidth;
        propertyDraw.maximumCharWidth = clientPropertyDraw.maximumCharWidth;
        propertyDraw.preferredCharWidth = clientPropertyDraw.preferredCharWidth;

        propertyDraw.panelLabelAbove = clientPropertyDraw.panelLabelAbove;

        return propertyDraw;
    }

    @Converter(from = EditBindingMap.class)
    public GEditBindingMap convertBindingMap(EditBindingMap editBindingMap) {
        HashMap<GKeyStroke, String> keyBindingMap = null;
        if (editBindingMap.getKeyBindingMap() != null) {
            keyBindingMap = new HashMap<GKeyStroke, String>();
            for (Map.Entry<KeyStroke, String> e : editBindingMap.getKeyBindingMap().entrySet()) {
                GKeyStroke key = convertOrCast(e.getKey());
                keyBindingMap.put(key, e.getValue());
            }
        }

        LinkedHashMap<String, String> contextMenuBindingMap = editBindingMap.getContextMenuItems() != null
                                                              ? new LinkedHashMap<String, String>(editBindingMap.getContextMenuItems())
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
            case SELECTOR: return GPropertyEditType.SELECTOR;
        }
        return null;
    }

    @Cached
    @Converter(from = ClientTreeGroup.class)
    public GTreeGroup convertTreeGroup(ClientTreeGroup clientTreeGroup) {
        GTreeGroup treeGroup = initGwtComponent(clientTreeGroup, new GTreeGroup());

        treeGroup.toolbar = convertOrCast(clientTreeGroup.toolbar);
        treeGroup.filter = convertOrCast(clientTreeGroup.filter);

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
        groupObject.banClassView = new ArrayList<String>();
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

        groupObject.hasUserPreferences = clientGroupObject.hasUserPreferences;

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
