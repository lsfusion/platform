package lsfusion.gwt.client.form.object.table.tree.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.JSNIHelper;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.ColorUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.grid.Column;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.event.GMouseInputEvent;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableFooter;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;
import lsfusion.gwt.client.form.order.user.GGridSortableHeaderManager;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GTreeTable extends GGridPropertyTable<GTreeGridRecord> {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    
    private boolean dataUpdated;

    private GTreeTableTree tree;

    private TreeTableSelectionHandler treeSelectionHandler;

    private GTreeGroupController treeGroupController;

    public NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, PValue>> values = new NativeSIDMap<>();
    public NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, PValue>> loadings = new NativeSIDMap<>();
    public NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, PValue>> readOnly = new NativeSIDMap<>();

    private GTreeGroup treeGroup;

    private GSize hierarchicalWidth;

    public GTreeTable(GFormController iformController, GForm iform, GTreeGroupController treeGroupController, GTreeGroup treeGroup, TableContainer tableContainer) {
        super(iformController, lastGroupObject(treeGroup), tableContainer, treeGroupController.getFont());

        this.treeGroupController = treeGroupController;
        this.treeGroup = treeGroup;

        tree = new GTreeTableTree(iform);

        Column<GTreeGridRecord, Object> column = new ExpandTreeColumn();
        GGridPropertyTableHeader header = noHeaders ? null : new GGridPropertyTableHeader(this, messages.formTree(), null, null, null, false, null);
        insertColumn(getColumnCount(), column, header, null);

        hierarchicalWidth = treeGroup.getExpandWidth();

        treeSelectionHandler = new TreeTableSelectionHandler(this);
        setSelectionHandler(treeSelectionHandler);

        setRowChangedHandler(() -> {
            final GTreeGridRecord kbSelectedRecord = getSelectedRowValue();
            if (kbSelectedRecord != null)
                form.changeGroupObjectLater(kbSelectedRecord.getGroup(), kbSelectedRecord.getKey());
        });

        sortableHeaderManager = new GGridSortableHeaderManager<GPropertyDraw>(this, true) {
            @Override
            protected void orderChanged(GPropertyDraw columnKey, GOrder modiType) {
                form.changePropertyOrder(columnKey, GGroupObjectValue.EMPTY, modiType);
            }

            @Override
            protected void ordersSet(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> orders) {
                List<Integer> propertyList = new ArrayList<>();
                List<GGroupObjectValue> columnKeyList = new ArrayList<>();
                List<Boolean> orderList = new ArrayList<>();
                for(Map.Entry<GPropertyDraw, Boolean> entry : orders.entrySet()) {
                    propertyList.add(entry.getKey().ID);
                    columnKeyList.add(GGroupObjectValue.EMPTY);
                    orderList.add(entry.getValue());
                }

                form.setPropertyOrders(groupObject, propertyList, columnKeyList, orderList);
            }

            @Override
            protected GPropertyDraw getColumnKey(int column) {
                return getTreeGridColumn(column).getColumnProperty();
            }
        };

        if(treeGroupController.isExpandOnClick())
            form.addBinding(new GMouseInputEvent(GMouseInputEvent.DBLCLK)::isEvent, new GBindingEnv(100, GBindingMode.ONLY, null, GBindingMode.ONLY, null, null, null, null),
                    () -> {
                        GTreeObjectTableNode node = getExpandSelectedNode();
                        return node != null && node.isExpandable();
                    },
                    event -> {
                        fireExpandSelectedNode(null);
                    }, getWidget(), groupObject);
    }

    private static GGroupObject lastGroupObject(GTreeGroup treeGroup) {
        return treeGroup.groups.size() > 0 ? treeGroup.groups.get(treeGroup.groups.size() - 1) : null;
    }

    public void removeProperty(GPropertyDraw property) {
        dataUpdated = true;

        values.remove(property);
        loadings.remove(property);
        int index = tree.removeProperty(property);
        if(index > -1) // we need only last group, just like everywhere
            removeColumn(index);

        columnsUpdated = true;
    }

    public boolean isPropertyShown(GPropertyDraw property) {
        return tree.getPropertyIndex(property) != -1;
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
       activateColumn(tree.getPropertyIndex(propertyDraw));
    }

    public void updateProperty(GPropertyDraw property, List<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values) {
        dataUpdated = true;

        if(!updateKeys) {
            int index = tree.updateProperty(property);

            if (index > -1) {
                GridColumn gridColumn = new GridColumn(property);
//                String propertyCaption = property.caption;
//                String captionElementClass = property.captionElementClass;
//                AppBaseImage propertyImage = !property.isAction() ? property.appImage : null;
//                String tooltip = property.getTooltip(propertyCaption);
                GGridPropertyTableHeader header = noHeaders ? null : new GGridPropertyTableHeader(this, property, gridColumn);
                GGridPropertyTableFooter footer = noFooters ? null : new GGridPropertyTableFooter(this, property, null, null, gridColumn.isSticky(), form);

                insertColumn(index, gridColumn, header, footer);

                columnsUpdated = true;
            }
        }
        updatePropertyValues(property, values, updateKeys);
    }

    private interface TreeGridColumn  {
        GPropertyDraw getColumnProperty();
    }

    private final static String TREE_NODE_ATTRIBUTE = "__tree_node";

    public static void renderExpandDom(Element cellElement, GTreeColumnValue treeValue) {
        for (int i = 0; i <= treeValue.level; i++) {
            DivElement img = createIndentElement(cellElement);
            updateIndentElement(img, treeValue, i);
        }
    }

    private static String IMAGE = "img";

    private static DivElement createIndentElement(Element cellElement) {
        DivElement div = Document.get().createDivElement();
        cellElement.appendChild(div);
        GwtClientUtils.setupPercentParent(div);

        div.getStyle().setFloat(Style.Float.LEFT);
        div.getStyle().setWidth(16, Style.Unit.PX); // indent width

        Element img = StaticImage.TREE_EMPTY.createImage();
        div.setPropertyObject(IMAGE, img);

        if (!MainFrame.useBootstrap) {
            DivElement vert = Document.get().createDivElement();
            div.appendChild(vert);
            GwtClientUtils.setupPercentParent(vert);

            DivElement top = vert.appendChild(Document.get().createDivElement());
            top.getStyle().setHeight(MainFrame.useBootstrap ? 0 : 50, Style.Unit.PCT);

            DivElement bottom = vert.appendChild(Document.get().createDivElement());
            bottom.getStyle().setHeight(MainFrame.useBootstrap ? 100 : 50, Style.Unit.PCT);
            bottom.appendChild(img); //need some initial value

            bottom.getStyle().setPosition(Style.Position.RELATIVE);
            img.getStyle().setTop(-8, Style.Unit.PX);
            img.getStyle().setPosition(Style.Position.ABSOLUTE);
        } else {
            div.appendChild(img);

            div.addClassName("wrap-text-not-empty");
            div.addClassName("wrap-img-horz");
            div.addClassName("wrap-img-start");

            img.addClassName("wrap-text-img");
        }
        return div;
    }

    private static void updateIndentElement(DivElement element, GTreeColumnValue treeValue, int indentLevel) {
        StaticImage indentIcon;
        Element img = (Element) element.getPropertyObject(IMAGE);
        int nodeLevel = treeValue.level;
        if (indentLevel < nodeLevel - 1) {
            indentIcon = treeValue.lastInLevelMap[indentLevel] ? StaticImage.TREE_EMPTY : StaticImage.TREE_PASSBY;
            img.removeAttribute(TREE_NODE_ATTRIBUTE);
        } else if (indentLevel == nodeLevel - 1) {
            indentIcon = StaticImage.TREE_BRANCH;
            img.removeAttribute(TREE_NODE_ATTRIBUTE);
        } else {
            assert indentLevel == nodeLevel;
            img.setAttribute(TREE_NODE_ATTRIBUTE, "true");
            indentIcon = getNodeIcon(treeValue);
        }

        if(!MainFrame.useBootstrap) {
            if (StaticImage.TREE_PASSBY.equals(indentIcon)) {
                changeDots(element, true, true);
            } else if (StaticImage.TREE_BRANCH.equals(indentIcon)) {
                if (treeValue.lastInLevelMap[indentLevel]) {
                    changeDots(element, true, false); //end
                } else {
                    changeDots(element, true, true); //branch
                }
            } else if (StaticImage.TREE_EMPTY.equals(indentIcon) || StaticImage.TREE_LEAF.equals(indentIcon)) {
                changeDots(element, false, false);
            } else if (StaticImage.TREE_CLOSED.equals(indentIcon)) {
                changeDots(element, false, treeValue.closedDotBottom);
            } else if (StaticImage.TREE_OPEN.equals(indentIcon)) {
                changeDots(element, false, treeValue.openDotBottom);
            }
        }

        if(StaticImage.TREE_CLOSED.equals(indentIcon)) {
            img.removeClassName("expanded-image");
            img.addClassName("collapsed-image");
        } else if(StaticImage.TREE_OPEN.equals(indentIcon)) {
            img.removeClassName("collapsed-image");
            img.addClassName("expanded-image");
        } else if(StaticImage.TREE_LEAF.equals(indentIcon)) {
            img.addClassName("leaf-image");
        } else if(StaticImage.TREE_BRANCH.equals(indentIcon)) {
            img.addClassName("branch-image");
        } else if (StaticImage.LOADING_ASYNC.equals(indentIcon)) {
            img.addClassName("loading-async-image");
        }

        (StaticImage.TREE_PASSBY.equals(indentIcon) ? StaticImage.TREE_EMPTY : indentIcon).updateImageSrc(img);
    }

    private static void changeDots(Element element, boolean dotTop, boolean dotBottom) {
        element = element.getFirstChildElement(); // vert
        Element top = element.getFirstChild().cast();
        Element bottom = element.getLastChild().cast();

        if (dotTop && dotBottom) {
            ensureDotsAndSetBackground(element);
            clearBackground(top);
            clearBackground(bottom);
            return;
        } else {
            clearBackground(element);
        }
        if (dotTop) {
            ensureDotsAndSetBackground(top);
        } else {
            clearBackground(top);
        }

        if (dotBottom) {
            ensureDotsAndSetBackground(bottom);
        } else {
            clearBackground(bottom);
        }
    }

    private static void ensureDotsAndSetBackground(Element element) {
        element.addClassName("passby-image");

        GwtClientUtils.setThemeImage(StaticImage.TREE_PASSBY.path, str -> element.getStyle().setBackgroundImage("url('" + str + "')"));
    }

    private static void clearBackground(Element element) {
        element.removeClassName("passby-image");
        element.getStyle().clearBackgroundImage();
    }

    private static StaticImage getNodeIcon(GTreeColumnValue treeValue) {
        switch (treeValue.type) {
            case LEAF:
                return StaticImage.TREE_LEAF;
            case OPEN:
                return StaticImage.TREE_OPEN;
            case CLOSED:
                return StaticImage.TREE_CLOSED;
            case LOADING:
                return StaticImage.LOADING_ASYNC;
        }
        throw new UnsupportedOperationException();
    }

    private static class RenderedState {
        public GTreeColumnValue value;

        public GFont font;
        public String foreground;
        public String background;
    }

    // actually singleton
    private class ExpandTreeColumn extends Column<GTreeGridRecord, Object> implements TreeGridColumn {

        @Override
        public String getNativeSID() {
            return "_EXPAND_COLUMN";
        }

        private GTreeColumnValue getTreeValue(Cell cell) {
            return getTreeGridRow(cell).getTreeValue();
        }

        @Override
        public boolean isFocusable() {
            return true;
        }

        @Override
        public boolean isSticky() {
            return false;
        }

        @Override
        public GPropertyDraw getColumnProperty() {
            return null;
        }

        @Override
        public void onEditEvent(EventHandler handler, Cell editCell, Element editRenderElement) {
            Event event = handler.event;
            boolean changeEvent = GMouseStroke.isChangeEvent(event);
            if (changeEvent || (treeGroupController.isExpandOnClick() && GMouseStroke.isDoubleChangeEvent(event))) { // we need to consume double click event to prevent treetable global dblclick binding (in this case node will be collapsed / expanded once again)
                String attrID = JSNIHelper.getAttributeOrNull(Element.as(event.getEventTarget()), TREE_NODE_ATTRIBUTE);
                if (attrID != null) {
                    boolean consumed = false;
                    if(changeEvent)
                        consumed = changeTreeState(editCell);
                    if(consumed)
                        handler.consume();
                }
            }
        }

        private boolean changeTreeState(Cell cell) {
            return fireExpandNode(tree.getExpandNodeByRecord(getTreeGridRow(cell)), null);
        }

        @Override
        public void renderDom(Cell cell, TableCellElement cellElement) {
            if(cell.getRow() != null) // can be cell in column row
                GTreeTable.renderExpandDom(cellElement, getTreeValue(cell));
        }

        private static final String RENDERED = "renderedTree";

        @Override
        public void updateDom(Cell cell, TableCellElement cellElement) {

            RenderedState renderedState = (RenderedState) cellElement.getPropertyObject(RENDERED);
            boolean isNew = false;
            if(renderedState == null) {
                renderedState = new RenderedState();
                cellElement.setPropertyObject(RENDERED, renderedState);

                isNew = true;
            }

            GTreeGridRecord rowValue = (GTreeGridRecord) cell.getRow();
            String background = ColorUtils.getThemedColor(rowValue.getRowBackground());
            String foreground = ColorUtils.getThemedColor(rowValue.getRowForeground());
            if(isNew || !equalsFontColorState(renderedState, font, background, foreground)) {
                renderedState.font = font;
                renderedState.background = background;
                renderedState.foreground = foreground;

                GFormController.updateFontColors(cellElement, font, background, foreground);
            }

            GTreeColumnValue treeValue = getTreeValue(cell);
            if(isNew || !equalsDynamicState(renderedState, treeValue)) {
                renderedState.value = treeValue;

                renderDynamicContent(cellElement, treeValue);
            }
        }

        private boolean equalsDynamicState(RenderedState state, GTreeColumnValue value) {
            return state.value.equalsValue(value);
        }
        private boolean equalsFontColorState(RenderedState state, GFont font, String background, String foreground) {
            return GwtClientUtils.nullEquals(state.font, font) && GwtClientUtils.nullEquals(state.background, background) && GwtClientUtils.nullEquals(state.foreground, foreground);
        }

        private void renderDynamicContent(TableCellElement cellElement, GTreeColumnValue treeValue) {
            while (cellElement.getChildCount() > treeValue.level + 1) {
                cellElement.getLastChild().removeFromParent();
            }

            for (int i = 0; i <= treeValue.level; i++) {
                DivElement imgContainer;
                if (i >= cellElement.getChildCount()) {
                    imgContainer = createIndentElement(cellElement);
                } else {
                    imgContainer = cellElement.getChild(i).cast();
                }

                updateIndentElement(imgContainer, treeValue, i);
            }
        }

        @Override
        public boolean isCustomRenderer(RendererType rendererType) {
            return false;
        }
    }

    private class GridColumn extends GridPropertyColumn implements TreeGridColumn {

        private final GPropertyDraw columnProperty;

        @Override
        public String getNativeSID() {
            return columnProperty.getNativeSID();
        }

        @Override
        public boolean isFocusable() {
            return GTreeTable.this.isFocusable(columnProperty);
        }

        @Override
        public boolean isSticky() {
            return columnProperty.sticky;
        }

        public GridColumn(GPropertyDraw columnProperty) {
            this.columnProperty = columnProperty;
        }

        @Override
        public void onEditEvent(EventHandler handler, Cell editCell, Element editRenderElement) {
            if (getProperty(editCell) != null) { // in tree there can be no property in groups other than last
                super.onEditEvent(handler, editCell, editRenderElement);
            }
        }

        @Override
        protected PValue getValue(GPropertyDraw property, GTreeGridRecord record) {
            return record.getValue(property);
        }

        @Override
        protected boolean isLoading(GPropertyDraw property, GTreeGridRecord record) {
            return record.isLoading(property);
        }

        @Override
        protected AppBaseImage getImage(GPropertyDraw property, GTreeGridRecord record) {
            return record.getImage(property);
        }

        @Override
        protected String getValueElementClass(GPropertyDraw property, GTreeGridRecord record) {
            return record.getValueElementClass(property);
        }

        @Override
        protected GFont getFont(GPropertyDraw property, GTreeGridRecord record) {
            return record.getFont(property);
        }

        @Override
        protected String getBackground(GPropertyDraw property, GTreeGridRecord record) {
            return record.getBackground(property);
        }

        @Override
        protected String getForeground(GPropertyDraw property, GTreeGridRecord record) {
            return record.getForeground(property);
        }

        @Override
        protected String getPlaceholder(GPropertyDraw property, GTreeGridRecord record) {
            return record.getPlaceholder(property);
        }

        @Override
        protected String getPattern(GPropertyDraw property, GTreeGridRecord record) {
            return record.getPattern(property);
        }

        @Override
        protected String getRegexp(GPropertyDraw property, GTreeGridRecord record) {
            return record.getRegexp(property);
        }

        @Override
        protected String getRegexpMessage(GPropertyDraw property, GTreeGridRecord record) {
            return record.getRegexpMessage(property);
        }

        @Override
        protected String getValueTooltip(GPropertyDraw property, GTreeGridRecord record) {
            return record.getValueTooltip(property);
        }

        // in tree property might change
        private static final String PDRAW_ATTRIBUTE = "__gwt_pdraw"; // actually it represents nod depth

        @Override
        public void renderDom(Cell cell, TableCellElement cellElement) {
            super.renderDom(cell, cellElement);

            cellElement.setPropertyObject(PDRAW_ATTRIBUTE, getProperty(cell));
        }

        @Override
        public void updateDom(Cell cell, TableCellElement cellElement) {
            GPropertyDraw newProperty = getProperty(cell);
            GPropertyDraw oldProperty = ((GPropertyDraw)cellElement.getPropertyObject(PDRAW_ATTRIBUTE));
            // if property changed - rerender
            if(!GwtClientUtils.nullEquals(oldProperty, newProperty) && !form.isEditing()) { // we don't want to clear editing (it will be rerendered anyway, however not sure if this check is needed)
                if(oldProperty != null) {
                    RendererType rendererType = RendererType.GRID;
                    if(!GPropertyTableBuilder.clearRenderSized(cellElement, oldProperty, rendererType)) {
                        assert cellElement == GPropertyTableBuilder.getRenderSizedElement(cellElement, oldProperty, rendererType);
                        oldProperty.getCellRenderer(rendererType).clearRender(cellElement, getRenderContext(cell, cellElement, oldProperty, this));
                    }
                    cellElement.setPropertyObject(PDRAW_ATTRIBUTE, null);
                }

                renderDom(cell, cellElement);
            }

            super.updateDom(cell, cellElement);
        }

        @Override
        public GPropertyDraw getColumnProperty() {
            return columnProperty;
        }

        @Override
        public boolean isCustomRenderer(RendererType rendererType) {
            return columnProperty.getCellRenderer(rendererType).isCustomRenderer();
        }
    }

    public void setKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, ArrayList<GGroupObjectValue> parents, NativeHashMap<GGroupObjectValue, Integer> expandable, int requestIndex) {
        tree.setKeys(group, keys, parents, expandable, requestIndex);

        dataUpdated = true;
    }

    public void updateLoadings(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> propLoadings) {
        // here can be leaks because of nulls (so in theory nulls better to be removed)
        GwtSharedUtils.putUpdate(loadings, property, propLoadings, loadings.containsKey(property));
        dataUpdated = true;
    }

    public void updatePropertyValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> propValues, boolean updateKeys) {
        GwtSharedUtils.putUpdate(values, property, propValues, updateKeys);

        dataUpdated = true;
    }

    public void updateReadOnlyValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> readOnlyValues) {
        GwtSharedUtils.putUpdate(readOnly, property, readOnlyValues, false);

        dataUpdated = true;
    }

    @Override
    public void updateCellFontValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateCellFontValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateCellBackgroundValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updatePlaceholderValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updatePlaceholderValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updatePatternValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updatePatternValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateRegexpValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateRegexpValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateRegexpMessageValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateRegexpMessageValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateTooltipValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateTooltipValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateValueTooltipValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateValueTooltipValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateCellForegroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateCellForegroundValues(propertyDraw, values);
        dataUpdated = true;
    }

    @Override
    public void updateImageValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateImageValues(propertyDraw, values);
        if(propertyDraw.isAction())
            dataUpdated = true;
        else
            captionsUpdated = true;
    }

    @Override
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateRowBackgroundValues(values);
        dataUpdated = true;
    }

    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        super.updateRowForegroundValues(values);
        dataUpdated = true;
    }

    private Integer hierarchicalUserWidth = null;
    private Double hierarchicalUserFlex = null;
    private final NativeSIDMap<GPropertyDraw, Integer> userWidths = new NativeSIDMap<>();
    private final NativeSIDMap<GPropertyDraw, Double> userFlexes = new NativeSIDMap<>();
    @Override
    protected void setUserWidth(GPropertyDraw property, Integer value) {
        userWidths.put(property, value);
    }

    @Override
    protected void setUserFlex(GPropertyDraw property, Double value) {
        userFlexes.put(property, value);
    }

    @Override
    protected Integer getUserWidth(GPropertyDraw property) {
        return userWidths.get(property);
    }

    @Override
    protected Double getUserFlex(GPropertyDraw property) {
        return userFlexes.get(property);
    }

    @Override
    protected void setUserWidth(int i, int width) {
        if(i==0) {
            hierarchicalUserWidth = width;
            return;
        }
        super.setUserWidth(i, width);
    }

    @Override
    protected Integer getUserWidth(int i) {
        if(i==0)
            return hierarchicalUserWidth;
        return super.getUserWidth(i);
    }

    @Override
    protected void setUserFlex(int i, double flex) {
        if(i==0) {
            hierarchicalUserFlex = flex;
            return;
        }
        super.setUserFlex(i, flex);
    }

    @Override
    protected Double getUserFlex(int i) {
        if(i==0)
            return hierarchicalUserFlex;
        return super.getUserFlex(i);
    }

    public GTreeGridRecord getTreeGridRow(Cell editCell) {
        return (GTreeGridRecord) editCell.getRow();
    }
    protected TreeGridColumn getTreeGridColumn(int i) {
        return (TreeGridColumn) getColumn(i);
    }
    public GridColumn getGridColumn(int i) {
        assert i > 0;
        return (GridColumn)super.getGridColumn(i);
    }

    @Override
    protected GPropertyDraw getColumnPropertyDraw(int i) {
        return getGridColumn(i).getColumnProperty();
    }

    @Override
    protected GGroupObjectValue getColumnKey(int i) {
        return GGroupObjectValue.EMPTY;
    }

    @Override
    protected void updatePropertyHeader(int index) {
        if(index > 0) {
            super.updatePropertyHeader(index);
        }
    }

    @Override
    public void updatePropertyFooter(int index) {
        if(index > 0) {
            super.updatePropertyFooter(index);
        }
    }

    @Override
    protected Boolean isResizeOverflow() {
        return treeGroup.resizeOverflow;
    }

    @Override
    protected double getColumnFlex(int i) {
        if(i == 0)
            return hierarchicalWidth.getValueFlexSize();
        return super.getColumnFlex(i);
    }

    @Override
    protected GSize getColumnBaseWidth(int i) {
        if(i==0)
            return hierarchicalWidth;
        return super.getColumnBaseWidth(i);
    }

    public void update() {
        updateColumns();

        updateCaptions();
        updateFooters();

        updateData();
    }

    public void updateData() {
        if (dataUpdated) {
            checkUpdateCurrentRow();

//            checkSelectedRowVisible();

            rows.clear();
            tree.updateRows(rows);
            updatePropertyReaders();
//            treeSelectionHandler.dataUpdated();

            rowsChanged();

            dataUpdated = false;
        }

        updateCurrentRow();
    }

    public void updateColumns() {
        if(columnsUpdated) {
            for (int i = 1, size = getColumnCount(); i < size; i++) {
                updatePropertyHeader(i);
                updatePropertyFooter(i);
            }

            updateLayoutWidth();

            columnsChanged();

            columnsUpdated = false;
            captionsUpdated = false;
            footersUpdated = false;
        }
    }

    @Override
    public void onResize() {
        if (getWidget().isVisible()) {
            super.onResize();
        }
    }

    protected void updatePropertyReaders() {
        for (GTreeGridRecord record : rows) {
            GGroupObjectValue key = record.getKey();

            record.setRowBackground(rowBackgroundValues.get(key));
            record.setRowForeground(rowForegroundValues.get(key));

            if(record instanceof GTreeObjectGridRecord) {
                GTreeObjectGridRecord objectRecord = (GTreeObjectGridRecord)record;

                for (int i = 1, size = getColumnCount(); i < size; i++) {
                    GPropertyDraw property = getProperty(objectRecord, i);
                    if (property != null) {
                        PValue value = values.get(property).get(key);
                        objectRecord.setValue(property, value);

                        NativeHashMap<GGroupObjectValue, PValue> loadingMap = loadings.get(property);
                        boolean loading = loadingMap != null && PValue.getBooleanValue(loadingMap.get(key));
                        objectRecord.setLoading(property, loading);

                        PValue valueElementClass = null;
                        NativeHashMap<GGroupObjectValue, PValue> propValueElementClasses = cellValueElementClasses.get(property);
                        if (propValueElementClasses != null)
                            valueElementClass = propValueElementClasses.get(key);
                        objectRecord.setValueElementClass(property, valueElementClass == null ? property.valueElementClass : PValue.getClassStringValue(valueElementClass));

                        PValue font = null;
                        NativeHashMap<GGroupObjectValue, PValue> propFonts = cellFontValues.get(property);
                        if (propFonts != null)
                            font = propFonts.get(key);
                        objectRecord.setFont(property, font == null ? property.font : PValue.getFontValue(font));

                        PValue background = null;
                        NativeHashMap<GGroupObjectValue, PValue> propBackgrounds = cellBackgroundValues.get(property);
                        if (propBackgrounds != null)
                            background = propBackgrounds.get(key);
                        objectRecord.setBackground(property, background == null ? property.getBackground() : PValue.getColorStringValue(background));

                        PValue foreground = null;
                        NativeHashMap<GGroupObjectValue, PValue> propForegrounds = cellForegroundValues.get(property);
                        if (propForegrounds != null)
                            foreground = propForegrounds.get(key);
                        objectRecord.setForeground(property, foreground == null ? property.getForeground() : PValue.getColorStringValue(foreground));

                        PValue placeholder = null;
                        NativeHashMap<GGroupObjectValue, PValue> propPlaceholders = placeholders.get(property);
                        if (propPlaceholders != null)
                            placeholder = propPlaceholders.get(key);
                        objectRecord.setPlaceholder(property, placeholder == null ? property.placeholder : PValue.getStringValue(placeholder));

                        PValue pattern = null;
                        NativeHashMap<GGroupObjectValue, PValue> propPatterns = patterns.get(property);
                        if (propPatterns != null)
                            pattern = propPatterns.get(key);
                        objectRecord.setPattern(property, pattern == null ? property.getPattern() : PValue.getStringValue(pattern));

                        PValue regexp = null;
                        NativeHashMap<GGroupObjectValue, PValue> propRegexps = regexps.get(property);
                        if (propRegexps != null)
                            regexp = propRegexps.get(key);
                        objectRecord.setRegexp(property, regexp == null ? property.regexp : PValue.getStringValue(regexp));

                        PValue regexpMessage = null;
                        NativeHashMap<GGroupObjectValue, PValue> propRegexpMessages = regexpMessages.get(property);
                        if (propRegexpMessages != null)
                            regexpMessage = propRegexps.get(key);
                        objectRecord.setRegexpMessage(property, regexp == null ? property.regexpMessage : PValue.getStringValue(regexpMessage));

                        PValue valueTooltip = null;
                        NativeHashMap<GGroupObjectValue, PValue> propValueTooltips = valueTooltips.get(property);
                        if (propValueTooltips != null)
                            valueTooltip = propValueTooltips.get(key);
                        objectRecord.setValueTooltip(property, valueTooltip == null ? property.valueTooltip : PValue.getStringValue(valueTooltip));

                        NativeHashMap<GGroupObjectValue, PValue> actionImages = property.isAction() ? cellImages.get(property) : null;
                        objectRecord.setImage(property, actionImages == null ? null : PValue.getImageValue(actionImages.get(key)));
                    }
                }
            }
        }
    }

    public void fireExpandNodeRecursive(boolean current, boolean open) {
        GTreeContainerTableNode node = getExpandSelectedNode();
        if (node != null && (!current || node.isExpandable())) {
            long requestIndex = form.expandGroupObjectRecursive(node.getGroup(), current, open);
            expandNodeRecursive(current ? node : tree.root, open, requestIndex);

            updateExpand();
        }
    }

    public void expandNodeRecursive(GTreeContainerTableNode node, boolean open, long requestIndex) {
        if(open) {
            for(GTreeChildTableNode child : node.getChildren())
                if(child instanceof GTreeObjectTableNode)
                    expandNodeRecursive((GTreeObjectTableNode)child, true, requestIndex);
            if(node.isExpandable() && !node.hasExpandableChildren())
                expandNode(node, true, requestIndex); // because it is expandable
        } else {
            if(node == tree.root) { // in that case we should collapse all root elements
                for (GTreeChildTableNode child : node.getChildren())
                    if (child instanceof GTreeObjectTableNode)
                        expandNode((GTreeObjectTableNode) child, false, requestIndex);
            } else
                expandNode(node, false, requestIndex);
        }
    }
    public void expandNode(GTreeContainerTableNode node, boolean open, long requestIndex) {
        node.setPendingExpanding(open, requestIndex);

        Pair<Integer, Integer> indexRange = null; GTreeObjectGridRecord record = null; int startFrom = 0, shift = 0;
        if(incrementalUpdate) {
            // finding row index and next sibling index
            indexRange = tree.incGetIndexRange(node);

            // changing expand column
            record = incUpdateExpandColumn(indexRange, open);
        }

        if(open) { // adding virtual "expandable" node
            assert !node.hasExpandableChildren();

            int addCount = node.getExpandableChildren();
            GTreeExpandingTableNode[] expandingNodes = new GTreeExpandingTableNode[addCount];
            for(int i=0;i<addCount;i++) {
                GTreeExpandingTableNode expandingNode = new GTreeExpandingTableNode(i);
                expandingNodes[i] = expandingNode;
                node.addNode(i, expandingNode);
            }
            if(incrementalUpdate) { // adding
                assert indexRange.first.equals(indexRange.second); // since this node should have no children
                int index = indexRange.first;

                // adding rows
                for(int i=0;i<addCount;i++) {
                    GTreeExpandingTableNode expandingNode = expandingNodes[i];
                    GTreeColumnValue treeValue = tree.createTreeColumnValue(expandingNode, node, record);
                    int addIndex = index + i;
                    GTreeExpandingGridRecord treeRecord = new GTreeExpandingGridRecord(addIndex, node, treeValue, expandingNode);
                    rows.add(addIndex, treeRecord);
                    tableBuilder.incBuildRow(tableWidget.getSection(), addIndex, treeRecord);
                }

                // shifting rows (here it's better to do to avoid dom reads)
                startFrom = index + addCount; shift = addCount;
            }
        } else { // removing all children nodes
            if(incrementalUpdate) {
                // removing all rows in that tange
                tableBuilder.incDeleteRows(tableWidget.getSection(), indexRange.first, indexRange.second);
                rows.removeRange(indexRange.first, indexRange.second);

                // shifting rows (here it's better to do to avoid dom reads)
                startFrom = indexRange.first; shift = indexRange.first - indexRange.second;
            }

            tree.removeChildrenFromGroupNodes(node);
            node.setChildren(new ArrayList<>());
        }

        if(incrementalUpdate)
            incUpdateRowIndices(startFrom, shift);
    }

    private GTreeObjectGridRecord incUpdateExpandColumn(Pair<Integer, Integer> indexRange, boolean open) {
        assert incrementalUpdate;

        int index = indexRange.first - 1;
        GTreeObjectGridRecord treeObjectRecord = (GTreeObjectGridRecord) getRowValue(index);
        treeObjectRecord.setTreeValue(treeObjectRecord.getTreeValue().override(GTreeColumnValueType.get(open)));
        tableBuilder.incUpdateRow(tableWidget.getSection(), index, new int[]{0}, treeObjectRecord);
        return treeObjectRecord;
    }

    private GTreeObjectTableNode getExpandSelectedNode() {
        return tree.getExpandNodeByRecord(getSelectedRowValue());
    }
    public boolean isCurrentPathExpanded() {
        GTreeObjectTableNode node;
        return (node = getExpandSelectedNode()) != null && node.isExpandable() && node.hasExpandableChildren();
    }

    public GGroupObject getGroupObject() {
        return null;
    }
    public GGridProperty getGrid() {
        return treeGroup;
    }

    @Override
    public GAbstractTableController getGroupController() {
        return treeGroupController;
    }

    public GPropertyDraw getSelectedFilterProperty() {
        GPropertyDraw property = getSelectedProperty();
        if (property == null && getColumnCount() > 1 && getSelectedRow() >= 0) {
            property = getProperty(getSelectedCell(1));
        }
        return property;
    }

    public PValue getSelectedValue(GPropertyDraw property) {
        GTreeGridRecord record = getSelectedRowValue();
        return record == null ? null : record.getValue(property);
    }

    public List<Pair<lsfusion.gwt.client.form.view.Column, String>> getFilterColumns(GGroupObject selectedGroupObject) {
        ArrayList<GPropertyDraw> properties = tree.getProperties(selectedGroupObject);
        if (properties != null) {
            return properties.stream().map(property ->
                    getFilterColumn(property, GGroupObjectValue.EMPTY)
            ).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public GPropertyDraw getProperty(Cell cell) {
        GTreeGridRecord rowValue = getTreeGridRow(cell);
        return rowValue == null ? null : getProperty(rowValue, cell.getColumnIndex());
    }

    public GPropertyDraw getProperty(GTreeGridRecord rowValue, int columnIndex) {
        return tree.getProperty(rowValue.getGroup(), columnIndex);
    }

    @Override
    public GGroupObjectValue getSelectedColumnKey() {
        return null;
    }
    @Override
    public GGroupObjectValue getColumnKey(Cell cell) {
        return GGroupObjectValue.EMPTY;
//        return currentRecords.get(context.getIndex()).getKey();
    }

    @Override
    public Boolean isReadOnly(Cell cell) {
        GPropertyDraw property = getProperty(cell);
        if (property != null && property.isReadOnly() == null) {
            NativeHashMap<GGroupObjectValue, PValue> propReadOnly = readOnly.get(property);
            return propReadOnly == null ? null : PValue.get3SBooleanValue(propReadOnly.get(getRowKey(cell)));
        }
        return false;
    }

    @Override
    public GGroupObjectValue getRowKey(Cell editCell) {
        return getTreeGridRow(editCell).getKey();
    }

    @Override
    public void quickFilter(Event event, GPropertyDraw filterProperty, GGroupObjectValue columnKey) {
        treeGroupController.quickEditFilter(event, filterProperty, columnKey);
    }

    @Override
    public void setValueAt(Cell cell, PValue value) {
        GPropertyDraw property = getProperty(cell);
        // assert property is not null since we want get here if property is null

        getTreeGridRow(cell).setValue(property, value);

        values.get(property).put(getRowKey(cell), value);
    }

    public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
        return setLoadingValueAt(property, treeGroup.filterRowKeys(property.groupObject, fullCurrentKey), tree.getPropertyIndex(property), GGroupObjectValue.EMPTY, value);
    }

    @Override
    public void setLoadingAt(Cell cell) {
        GPropertyDraw property = getProperty(cell);
        // assert property is not null since we want get here if property is null

        getTreeGridRow(cell).setLoading(property, true);
        NativeHashMap<GGroupObjectValue, PValue> loadingMap = loadings.get(property);
        if(loadingMap == null) {
            loadingMap = new NativeHashMap<>();
            loadings.put(property, loadingMap);
        }
        loadingMap.put(getRowKey(cell), PValue.getPValue(true));
    }

    public boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        return sortableHeaderManager.changeOrders(groupObject, orders, alreadySet);
    }

    public boolean keyboardNodeChangeState(boolean open) {
        return fireExpandSelectedNode(open);
    }

    public boolean fireExpandSelectedNode(Boolean open) {
        return fireExpandNode(getExpandSelectedNode(), open);
    }

    // open = null - toggle
    public boolean fireExpandNode(GTreeObjectTableNode node, Boolean open) {
        if(node == null || !node.isExpandable())
            return false;

        boolean nodeIsOpen = node.hasExpandableChildren();
        if (open == null || !open.equals(nodeIsOpen)) {
            open = !nodeIsOpen;
            long requestIndex = form.expandGroupObject(node.getGroup(), node.getKey(), open);
            expandNode(node, open, requestIndex);
//                treeSelectionHandler.nodeTryingToExpand = node;

            updateExpand();

            return true;
        }
        return false;
    }

    public void updateExpand() {
        if (!incrementalUpdate) {
            dataUpdated = true;
            updateData();
        }
    }

    public class TreeTableSelectionHandler extends GridPropertyTableSelectionHandler<GTreeGridRecord> {
//        public GTreeTableNode nodeTryingToExpand = null;

        public TreeTableSelectionHandler(DataGrid<GTreeGridRecord> table) {
            super(table);
        }

//        public void dataUpdated() {
//            if (nodeTryingToExpand != null) {
//                if (!nodeTryingToExpand.isOpen()) {
//                    nextColumn(true);
//                }
//                nodeTryingToExpand = null;
//            }
//        }

        @Override
        public boolean handleKeyEvent(Event event) {
            assert BrowserEvents.KEYDOWN.equals(event.getType());

            int keyCode = event.getKeyCode();
            if (!event.getCtrlKey() && getSelectedColumn() == 0) {
                if (keyCode == KeyCodes.KEY_RIGHT) {
                    if (keyboardNodeChangeState(true)) {
                        return true;
                    }
                } else if (keyCode == KeyCodes.KEY_LEFT) {
                    if (keyboardNodeChangeState(false)) {
                        return true;
                    }
                }
            }

            return super.handleKeyEvent(event);
        }
    }

    @Override
    protected void scrollToEnd(boolean toEnd) {
        selectionHandler.changeRow(toEnd ? (getRowCount() - 1) : 0, FocusUtils.Reason.KEYMOVENAVIGATE);
    }
}
