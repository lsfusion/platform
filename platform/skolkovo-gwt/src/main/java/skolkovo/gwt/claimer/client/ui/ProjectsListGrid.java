package skolkovo.gwt.claimer.client.ui;

import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;

public class ProjectsListGrid extends ListGrid {
    public ProjectsListGrid() {
        setWidth100();
        setLeaveScrollbarGap(false);
        setSelectionType(SelectionStyle.SINGLE);
        setShowHeaderContextMenu(false);
        setShowHeaderMenuButton(false);
        setAnimateRemoveRecord(false);
        setShowAllRecords(true);
        setShowRollOver(false);
        setCanEdit(false);
        setAutoFitData(Autofit.VERTICAL);
        setAutoFitMaxRecords(10);

        createFields();
    }

    private void createFields() {
        ListGridField nameField = new ListGridField("name", "Name");
        nameField.setEscapeHTML(true);

        setFields(nameField);
    }
}
