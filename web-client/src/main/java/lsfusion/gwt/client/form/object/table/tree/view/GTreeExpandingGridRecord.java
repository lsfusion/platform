package lsfusion.gwt.client.form.object.table.tree.view;

public class GTreeExpandingGridRecord extends GTreeGridRecord {

    private final int index;

    public GTreeExpandingGridRecord(int rowIndex, GTreeContainerTableNode node, GTreeColumnValue treeValue, GTreeExpandingTableNode expandNode) {
        super(rowIndex, node, treeValue);

        index = expandNode.index;
    }

    @Override
    public int getExpandingIndex() {
        return index;
    }
}
