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

        minimumMask = GwtSharedUtils.replicate('0', correctMinimumCharWidth(length));
        preferredMask = GwtSharedUtils.replicate('0', correctPreferredCharWidth(length));
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new StringGridCellEditor(editManager, editProperty);
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

    private int correctMinimumCharWidth(int charWidth) {
        return charWidth <= 3
                ? charWidth
                : charWidth <= 80
                    ? (int) Math.round(Math.pow(charWidth, 0.7))
                    : (int) Math.round(Math.pow(charWidth, 0.65));
    }

    private int correctPreferredCharWidth(int charWidth) {
        return charWidth <= 20
                ? charWidth
                : charWidth <= 80
                    ? (int) Math.round(Math.pow(charWidth, 0.8))
                    : (int) Math.round(Math.pow(charWidth, 0.7));
    }

    @Override
    public int getMinimumCharWidth(int definedMinimumCharWidth) {
        return definedMinimumCharWidth > 0 ? correctMinimumCharWidth(definedMinimumCharWidth) : minimumMask.length();
    }

    @Override
    public int getPreferredCharWidth(int definedPreferredCharWidth) {
        return definedPreferredCharWidth > 0 ? correctPreferredCharWidth(definedPreferredCharWidth) : preferredMask.length();
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, Integer fontSize) {
        int minCharWidth = getMinimumCharWidth(minimumCharWidth);
        return fontSize == null ? minCharWidth * 10 : minCharWidth * fontSize * 5 / 8;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, Integer fontSize) {
        int prefCharWidth = getPreferredCharWidth(preferredCharWidth);
        return fontSize == null ? prefCharWidth * 10 : prefCharWidth * fontSize * 5 / 8;
    }
}
