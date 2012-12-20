package platform.gwt.form.shared.view.classes;

import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.filter.GCompare;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.StringGridCellEditor;

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
        return new StringGridCellEditor(editManager);
    }

    @Override
    public String getMinimumMask() {
        return minimumMask;
    }

    public String getPreferredMask() {
        return preferredMask;
    }

    @Override
    public GCompare[] getFilterCompares() {
        return GCompare.values();
    }

    @Override
    public GCompare getDefaultCompare() {
        return GCompare.START_WITH;
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth) {
        return getMinimumCharWidth(minimumCharWidth) * 10;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth) {
        return getPreferredCharWidth(preferredCharWidth) * 10;
    }
}
