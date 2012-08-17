package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.shared.view.grid.renderer.DateGridRenderer;
import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;

public class GDateType extends GDataType {
    public static GDateType instance = new GDateType();

//    @Override
//    public PanelRenderer createPanelRenderer(GFormController form, GPropertyDraw property) {
//        return new DatePanelRenderer(property, GwtSharedUtils.getDefaultDateFormat());
//    }

    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new DateGridRenderer();
    }
}
