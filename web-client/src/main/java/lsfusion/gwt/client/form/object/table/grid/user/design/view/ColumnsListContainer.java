package lsfusion.gwt.client.form.object.table.grid.user.design.view;

import com.google.gwt.user.client.ui.FocusPanel;
import lsfusion.gwt.client.base.GwtClientUtils;

import static lsfusion.gwt.client.view.MainFrame.v5;

public class ColumnsListContainer extends FocusPanel {

    private ColumnsListBox listBox;

    public ColumnsListContainer(ColumnsListBox listBox) {
        this.listBox = listBox;
        super.setWidget(listBox);
        GwtClientUtils.addClassName(this, "list-box-container", "listBoxContainer", v5);
    }

    public ColumnsListBox getListBox() {
        return listBox;
    }
}
