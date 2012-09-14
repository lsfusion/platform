package platform.gwt.form2.server.convert;

import platform.client.logics.*;
import platform.gwt.form2.shared.view.*;
import platform.gwt.form2.shared.view.reader.*;
import platform.interop.ClassViewType;
import platform.interop.PropertyEditType;
import platform.interop.form.layout.SimplexConstraints;
import platform.interop.form.layout.SingleSimplexConstraint;

import java.util.ArrayList;

import static platform.interop.form.layout.ContainerType.*;

@SuppressWarnings("UnusedDeclaration")
public class ClientComponentToGwtConverter extends CachedObjectConverter {

    private final ClientTypeToGwtConverter typeConverter = ClientTypeToGwtConverter.getInstance();

    public ClientComponentToGwtConverter() {
    }

    private <T extends GComponent> T initGwtComponent(ClientComponent clientComponent, T component) {
        cacheInstance(clientComponent, component);

        component.ID = clientComponent.ID;
        component.sID = clientComponent.getSID();
        component.container = convertOrCast(clientComponent.container);
        component.defaultComponent = clientComponent.defaultComponent;
        component.drawToToolbar = clientComponent.drawToToolbar();

        if (clientComponent.preferredSize != null) {
            component.prefferedWidth = clientComponent.preferredSize.width;
            component.prefferedHeight = clientComponent.preferredSize.height;
        }

        if (clientComponent.preferredSize != null && clientComponent.preferredSize.equals(clientComponent.minimumSize) && clientComponent.preferredSize.equals(clientComponent.maximumSize)) {
            component.absoluteWidth = clientComponent.preferredSize.width;
            component.absoluteHeight = clientComponent.preferredSize.height;
        }
        component.fillHorizontal = clientComponent.constraints.fillHorizontal;
        component.fillVertical = clientComponent.constraints.fillVertical;
        component.hAlign = clientComponent.constraints.directions.R > 0 ? GComponent.Alignment.RIGHT : GComponent.Alignment.LEFT;

        return component;
    }

    @Cached
    @Converter(from = ClientContainer.class)
    public GContainer convertContainer(ClientContainer clientContainer) {
        GContainer container = initGwtComponent(clientContainer,  new GContainer());

        container.title = clientContainer.getTitle();

        byte containerType = clientContainer.getType();
        switch (containerType) {
            case CONTAINER:
                container.type = GContainerType.CONTAINER;
                break;
            case SPLIT_PANE_VERTICAL:
                container.type = GContainerType.VERTICAL_SPLIT_PANEL;
                break;
            case SPLIT_PANE_HORIZONTAL:
                container.type = GContainerType.HORIZONTAL_SPLIT_PANEL;
                break;
            case TABBED_PANE:
                container.type = GContainerType.TABBED_PANEL;
                break;
        }

        SimplexConstraints<ClientComponent> constraints = clientContainer.getConstraints();
        if (clientContainer.isSplitPane()) {
            container.vertical = clientContainer.getType() == SPLIT_PANE_VERTICAL;
        } else if (constraints.childConstraints.equals(SingleSimplexConstraint.TOTHE_RIGHT)) {
            container.vertical = false;
        } else if (constraints.childConstraints.equals(SingleSimplexConstraint.TOTHE_BOTTOM)) {
            container.vertical = true;
        }
        for (ClientComponent child : clientContainer.children) {
            GComponent childComponent = convertOrCast(child);
            container.children.add(childComponent);
        }
        container.resizable = clientContainer.gwtResizable;

        return container;
    }

    @Cached
    @Converter(from = ClientRegularFilterGroup.class)
    public GRegularFilterGroup convertRegularFilterGroup(ClientRegularFilterGroup clientFilterGroup) {
        GRegularFilterGroup filterGroup = initGwtComponent(clientFilterGroup, new GRegularFilterGroup());

        filterGroup.defaultFilter = clientFilterGroup.defaultFilter;
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

        if (clientPropertyDraw.addRemove != null) {
            GObject addRemoveObject = convertOrCast(clientPropertyDraw.addRemove.first);
            propertyDraw.addRemove = new GPropertyDraw.AddRemove(addRemoveObject, clientPropertyDraw.addRemove.second);
        }

        propertyDraw.askConfirm = clientPropertyDraw.askConfirm;
        propertyDraw.askConfirmMessage = clientPropertyDraw.askConfirmMessage;

        propertyDraw.iconPath = clientPropertyDraw.design.iconPath;

        propertyDraw.editType = convertOrCast(clientPropertyDraw.editType);

        propertyDraw.focusable = clientPropertyDraw.focusable;
        propertyDraw.checkEquals = clientPropertyDraw.checkEquals;

        propertyDraw.captionReader = convertCaptionReader(clientPropertyDraw.captionReader);
        propertyDraw.footerReader = convertFooterReader(clientPropertyDraw.footerReader);
        propertyDraw.backgroundReader = convertBackgroundReader(clientPropertyDraw.backgroundReader);
        propertyDraw.foregroundReader = convertForegroundReader(clientPropertyDraw.foregroundReader);

        return propertyDraw;
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

    public GCaptionReader convertCaptionReader(ClientPropertyDraw.CaptionReader reader) {
        return reader == null ? null : new GCaptionReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
    }

    public GFooterReader convertFooterReader(ClientPropertyDraw.FooterReader reader) {
        return reader == null ? null : new GFooterReader(reader.getID(), reader.getGroupObject() != null ? reader.getGroupObject().ID : -1);
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
    @Converter(from = ClientTreeGroup.class)
    public GTreeGroup convertTreeGroup(ClientTreeGroup clientTreeGroup) {
        GTreeGroup treeGroup = initGwtComponent(clientTreeGroup, new GTreeGroup());
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
        for (ClientObject clientObject : clientGroupObject.objects) {
            GObject object = convertOrCast(clientObject);
            groupObject.objects.add(object);
        }
        groupObject.grid = convertOrCast(clientGroupObject.grid);
        groupObject.showType = convertOrCast(clientGroupObject.showType);
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

        form.caption = clientForm.caption;
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
