package lsfusion.gwt.client.form.object.table.tree.view;

public class GTreeExpandingTableNode implements GTreeChildTableNode {

    public final int index;

    public GTreeExpandingTableNode(int index) {
        this.index = index;
    }

    @Override
    public GTreeColumnValueType getColumnValueType() {
        return GTreeColumnValueType.LOADING;
    }
}
