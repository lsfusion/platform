package platform.gwt.form.client.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.base.client.JSNHelper;
import platform.gwt.cellview.client.cell.AbstractCell;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;
import static com.google.gwt.dom.client.BrowserEvents.DBLCLICK;

public class GTreeGridControlCell extends AbstractCell<Object> {
    private final String ICON_LEAF = "tree_leaf.png";
    private final String ICON_OPEN = "tree_open.png";
    private final String ICON_CLOSED = "tree_closed.png";
    private final String ICON_PASSBY = "tree_dots_passby.png";
    private final String ICON_EMPTY = "tree_empty.png";
    private final String ICON_BRANCH = "tree_dots_branch.png";
    private final String ICON_END = "tree_dots_end.png";
    private final String TREE_NODE_ATTRIBUTE = "__tree_node";

    private GTreeTable treeTable;

    public GTreeGridControlCell(GTreeTable table) {
        super(CLICK, DBLCLICK);
        treeTable = table;
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, Object value, NativeEvent event) {
        if (DBLCLICK.equals(event.getType())) {
            changeTreeState(context, value, event);
        } else if (CLICK.equals(event.getType())) {
            String attrID = JSNHelper.getAttributeOrNull(Element.as(event.getEventTarget()), TREE_NODE_ATTRIBUTE);
            if (attrID != null) {
                changeTreeState(context, value, event);
            }
        }
    }

    private void changeTreeState(Context context, Object value, NativeEvent event) {
        GwtClientUtils.stopPropagation(event);

        Boolean open = ((GTreeColumnValue) value).getOpen();
        if (open != null) {
            if (!open) {
                treeTable.fireExpandNode((GTreeGridRecord) context.getRowValue());
            } else {
                treeTable.fireCollapseNode((GTreeGridRecord) context.getRowValue());
            }
        }
    }

    @Override
    public void renderDom(Context context, DivElement cellElement, Object value) {
        GTreeColumnValue treeValue = (GTreeColumnValue) value;
        for (int i = 0; i <= treeValue.getLevel(); i++) {
            ImageElement img = createIndentElement(cellElement);
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
            ImageElement img;
            if (i >= cellElement.getChildCount()) {
                img = createIndentElement(cellElement);
            } else {
                img = cellElement.getChild(i).getFirstChild().cast();
            }

            updateIndentElement(img, treeValue, i);
        }
    }

    private ImageElement createIndentElement(DivElement cellElement) {
        DivElement div = cellElement.appendChild(Document.get().createDivElement());
        div.getStyle().setFloat(Style.Float.LEFT);
        div.getStyle().setHeight(16, Style.Unit.PX);

        return div.appendChild(Document.get().createImageElement());
    }

    private void updateIndentElement(ImageElement img, GTreeColumnValue treeValue, int indentLevel) {
        String indentIcon;
        int nodeLevel = treeValue.getLevel();
        if (indentLevel < nodeLevel - 1) {
            indentIcon = treeValue.isLastInLevel(indentLevel) ? ICON_EMPTY : ICON_PASSBY;
            img.removeAttribute(TREE_NODE_ATTRIBUTE);
        } else if (indentLevel == nodeLevel - 1) {
            indentIcon = treeValue.isLastInLevel(indentLevel) ? ICON_END : ICON_BRANCH;
            img.removeAttribute(TREE_NODE_ATTRIBUTE);
        } else {
            assert indentLevel == nodeLevel;
            img.setAttribute(TREE_NODE_ATTRIBUTE, treeValue.getSID());
            indentIcon = getNodeIcon(treeValue);
        }

        img.setSrc(getImageURL(indentIcon));
    }

    private String getNodeIcon(GTreeColumnValue treeValue) {
        if (treeValue.getOpen() == null) {
            return ICON_LEAF;
        } else if (treeValue.getOpen()) {
            return ICON_OPEN;
        } else {
            return ICON_CLOSED;
        }
    }

    private String getImageURL(String imageName) {
        return GWT.getModuleBaseURL() + "images/" + imageName;
    }
}
