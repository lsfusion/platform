package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.StringGridEditor;
import platform.gwt.base.shared.GwtSharedUtils;

public class GStringType extends GDataType {
    protected int length = 50;

    private String minimumMask;
    private String preferredMask;

    public GStringType() {}

    public GStringType(int length) {
        this.length = length;

        minimumMask = GwtSharedUtils.replicate('0', length <= 3 ? length : (int) Math.round(Math.pow(length, 0.7)));
        preferredMask = GwtSharedUtils.replicate('0', length <= 20 ? length : (int) Math.round(Math.pow(length, 0.8)));
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new StringGridEditor(editManager);
    }

    @Override
    public String getMinimumMask() {
        return minimumMask;
    }

    public String getPreferredMask() {
        return preferredMask;
    }
}
