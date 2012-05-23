package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.view.GForm;
import platform.gwt.view.GGrid;

public class GGridController {
    private GGrid key;
    private VLayout gridView;
    private GGridTable table;
    private GFormLayout formLayout;

    public GGridController(GGrid key, GFormController iformController, GForm iform, GGroupObjectController igroupObject, GFormLayout iformLayout) {
        this.key = key;
        formLayout = iformLayout;
        gridView = new VLayout();
        table = new GGridTable(iformController, iform, igroupObject);
        gridView.addMember(table);
        gridView.addMember(igroupObject.getGridToolbar());
    }
    
    public VLayout getGridView() {
        return gridView;
    }

    public GGridTable getTable() {
        return table;
    }

    public void update() {
        table.update();
    }

    public void addView() {
        formLayout.add(key, gridView);
    }

    public GGrid getGrid() {
        return key;
    }

    public void hide() {
        gridView.setVisible(false);
    }

    public void show() {
        gridView.setVisible(true);
    }
}
