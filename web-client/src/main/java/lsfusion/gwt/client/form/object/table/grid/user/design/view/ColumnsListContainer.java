package lsfusion.gwt.client.form.object.table.grid.user.design.view;

import com.google.gwt.user.client.ui.FocusPanel;
import lsfusion.gwt.client.base.GwtClientUtils;

public class ColumnsListContainer extends FocusPanel {

    private ColumnsListBox listBox;

    public ColumnsListContainer(ColumnsListBox listBox) {
        this.listBox = listBox;
        super.setWidget(listBox);
        GwtClientUtils.addClassName(this, "list-box-container", "listBoxContainer");
    }

    public ColumnsListBox getListBox() {
        return listBox;
    }
}
