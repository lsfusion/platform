package lsfusion.gwt.client.form.object.table.tree.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.JSNIHelper;
import lsfusion.gwt.client.base.view.grid.cell.AbstractCell;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;
import static lsfusion.gwt.client.base.GwtClientUtils.setThemeImage;

public class GTreeGridControlCell extends AbstractCell<Object> {
    private final static String ICON_LEAF = "tree_leaf.png";
    private final static String ICON_OPEN = "tree_open.png";
    private final static String ICON_CLOSED = "tree_closed.png";
    private final static String ICON_PASSBY = "tree_dots_passby.png";
    private final static String ICON_EMPTY = "tree_empty.png";
    private final static String ICON_BRANCH = "tree_dots_branch.png";
    private final static String TREE_NODE_ATTRIBUTE = "__tree_node";

    private GTreeTable treeTable;

    public GTreeGridControlCell(GTreeTable table) {
        super(CLICK);
        treeTable = table;
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, Object value, NativeEvent event) {
        if (CLICK.equals(event.getType())) {
            String attrID = JSNIHelper.getAttributeOrNull(Element.as(event.getEventTarget()), TREE_NODE_ATTRIBUTE);
            if (attrID != null) {
                changeTreeState(context, value, event);
                GwtClientUtils.stopPropagation(event);
            }
        }
    }

    private void changeTreeState(Context context, Object value, NativeEvent event) {
        GwtClientUtils.stopPropagation(event);

        Boolean open = ((GTreeColumnValue) value).getOpen();
        if (open != null) {
            GTreeGridRecord record = (GTreeGridRecord) context.getRowValue();
            if (!open) {
                treeTable.expandNodeByRecord(record);
            } else {
                treeTable.collapseNodeByRecord(record);
            }
        }
    }

    @Override
    public void renderDom(Context context, DivElement cellElement, Object value) {
        renderDom(cellElement, (GTreeColumnValue) value);
    }

    public static void renderDom(Element cellElement, GTreeColumnValue treeValue) {
        for (int i = 0; i <= treeValue.getLevel(); i++) {
            DivElement img = createIndentElement(cellElement);
            updateIndentElement(img, treeValue, i);
        }
    }

    @Override
    public void updateDom(Context context, DivElement cellElement, Object value) {

        GTreeColumnValue treeValue = (GTreeColumnValue) value;

        while (cellElement.getChildCount() > treeValue.getLevel() + 1) {
            cellElement.getLastChild().removeFromParent();
        }

        for (int i = 0; i <= treeValue.getLevel(); i++) {
            DivElement img;
            if (i >= cellElement.getChildCount()) {
                img = createIndentElement(cellElement);
            } else {
                img = cellElement.getChild(i).getFirstChild().cast();
            }

            updateIndentElement(img, treeValue, i);
        }
    }

    private static DivElement createIndentElement(Element cellElement) {
        DivElement div = cellElement.appendChild(Document.get().createDivElement());
        div.getStyle().setFloat(Style.Float.LEFT);
        div.getStyle().setHeight(100, Style.Unit.PCT);
        div.getStyle().setWidth(16, Style.Unit.PX);

        DivElement vert = Document.get().createDivElement();
        vert.getStyle().setWidth(16, Style.Unit.PX);
        vert.getStyle().setHeight(100, Style.Unit.PCT);

        DivElement top = vert.appendChild(Document.get().createDivElement());
        top.getStyle().setHeight(50, Style.Unit.PCT);

        DivElement bottom = vert.appendChild(Document.get().createDivElement());
        bottom.getStyle().setHeight(50, Style.Unit.PCT);
        bottom.getStyle().setPosition(Style.Position.RELATIVE);

        ImageElement img = bottom.appendChild(Document.get().createImageElement());
        img.getStyle().setPosition(Style.Position.ABSOLUTE);
        img.getStyle().setTop(-8, Style.Unit.PX);

        return div.appendChild(vert);
    }

    private static void updateIndentElement(DivElement element, GTreeColumnValue treeValue, int indentLevel) {
        String indentIcon;
        ImageElement img = element.getElementsByTagName("img").getItem(0).cast();
        int nodeLevel = treeValue.getLevel();
        if (indentLevel < nodeLevel - 1) {
            indentIcon = treeValue.isLastInLevel(indentLevel) ? ICON_EMPTY : ICON_PASSBY;
            img.removeAttribute(TREE_NODE_ATTRIBUTE);
        } else if (indentLevel == nodeLevel - 1) {
            indentIcon = ICON_BRANCH;
            img.removeAttribute(TREE_NODE_ATTRIBUTE);
        } else {
            assert indentLevel == nodeLevel;
            img.setAttribute(TREE_NODE_ATTRIBUTE, treeValue.getSID());
            indentIcon = getNodeIcon(treeValue);
        }

        if (ICON_PASSBY.equals(indentIcon)) {
            changeDots(element, true, true);
        } else if (ICON_BRANCH.equals(indentIcon)) {
            if (treeValue.isLastInLevel(indentLevel)) {
                changeDots(element, true, false); //end   
            } else {
                changeDots(element, true, true); //branch
            }
        } else if (ICON_EMPTY.equals(indentIcon) || ICON_LEAF.equals(indentIcon) || ICON_CLOSED.equals(indentIcon)) {
            changeDots(element, false, false);
        } else if (ICON_OPEN.equals(indentIcon)) {
            changeDots(element, false, treeValue.isOpenDotBottom());
        }

        if(ICON_CLOSED.equals(indentIcon)) {
            img.removeClassName("expanded-image");
            img.addClassName("collapsed-image");
        } else if(ICON_OPEN.equals(indentIcon)) {
            img.removeClassName("collapsed-image");
            img.addClassName("expanded-image");
        } else if(ICON_LEAF.equals(indentIcon)) {
            img.addClassName("leaf-image");
        } else if(ICON_BRANCH.equals(indentIcon)) {
            img.addClassName("branch-image");
        }

        setThemeImage(ICON_PASSBY.equals(indentIcon) ? ICON_EMPTY : indentIcon, img::setSrc);
    }

    private static void changeDots(DivElement element, boolean dotTop, boolean dotBottom) {
        Element top = element.getFirstChild().cast();
        Element bottom = element.getLastChild().cast();

        if (dotTop && dotBottom) {
            ensureDotsAndSetBackground(element);
            element.getStyle().setProperty("backgroundRepeat", "no-repeat repeat");
            clearBackground(top);
            clearBackground(bottom);
            return;
        } else {
            clearBackground(element);
        }
        if (dotTop) {
            ensureDotsAndSetBackground(top);
            top.getStyle().setProperty("backgroundRepeat", "no-repeat repeat");
        } else {
            clearBackground(top);
        }

        if (dotBottom) {
            ensureDotsAndSetBackground(bottom);
            bottom.getStyle().setProperty("backgroundRepeat", "no-repeat repeat");
        } else {
            clearBackground(bottom);
        }
    }

    private static void ensureDotsAndSetBackground(Element element) {
        element.addClassName("passby-image");
        setThemeImage(ICON_PASSBY, str -> element.getStyle().setBackgroundImage("url('" + str + "')"));
    }

    private static void clearBackground(Element element) {
        element.removeClassName("passby-image");
        element.getStyle().clearBackgroundImage();
    }

    private static String getNodeIcon(GTreeColumnValue treeValue) {
        if (treeValue.getOpen() == null) {
            return ICON_LEAF;
        } else if (treeValue.getOpen()) {
            return ICON_OPEN;
        } else {
            return ICON_CLOSED;
        }
    }
}
