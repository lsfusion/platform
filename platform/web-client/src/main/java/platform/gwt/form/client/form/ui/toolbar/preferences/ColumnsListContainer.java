package platform.gwt.form.client.form.ui.toolbar.preferences;

import com.google.gwt.user.client.ui.FocusPanel;

public class ColumnsListContainer extends FocusPanel {
    private static final String CSS_LIST_BOX_CONTAINER = "listBoxContainer";

    private ColumnsListBox listBox;

    public ColumnsListContainer(ColumnsListBox listBox) {
        this.listBox = listBox;
        super.setWidget(listBox);
        addStyleName(CSS_LIST_BOX_CONTAINER);
    }

    public ColumnsListBox getListBox() {
        return listBox;
    }
}
