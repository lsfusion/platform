package platform.gwt.form.client.navigator;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tree.TreeGrid;
import platform.gwt.form.client.MainFrame;
import platform.gwt.form.client.events.OpenFormEvent;

public class NavigatorPanel extends VLayout {
    private final MainFrame mainFrame;

    public NavigatorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        addMember(createTreeGrid());
    }

    private TreeGrid treeGrid;

    private TreeGrid createTreeGrid() {
        treeGrid = new TreeGrid();
        treeGrid.setWidth("300");
        treeGrid.setHeight100();
        treeGrid.setSelectionType(SelectionStyle.SINGLE);
        treeGrid.setShowRollOver(false);
        treeGrid.setCanResizeFields(true);
        treeGrid.setCanSort(false);
        treeGrid.setCanEdit(false);
        treeGrid.setAutoFetchData(true);
        treeGrid.setShowHeaderContextMenu(false);
        treeGrid.setShowConnectors(true);
        treeGrid.setShowHeaderMenuButton(false);
        treeGrid.setShowOpenIcons(false);
        treeGrid.setShowDropIcons(false);

        treeGrid.setDataSource(NavigatorTreeDS.getInstance());

        treeGrid.addRecordDoubleClickHandler(new RecordDoubleClickHandler() {
            @Override
            public void onRecordDoubleClick(RecordDoubleClickEvent event) {
                Record formRecord = event.getRecord();
                boolean isForm = formRecord.getAttributeAsBoolean("isForm");
                if (isForm) {
                    OpenFormEvent.fireEvent(formRecord.getAttribute("elementSid"), formRecord.getAttribute("caption"));
                }
            }
        });
        return treeGrid;
    }
}
