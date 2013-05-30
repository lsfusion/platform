package platform.gwt.form.shared.view.classes;

import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GExtInt;
import platform.gwt.form.shared.view.GFont;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.filter.GCompare;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.StringGridCellEditor;
import platform.gwt.form.shared.view.grid.editor.TextGridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.StringGridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.TextGridCellRenderer;

import java.text.ParseException;

public class GStringType extends GDataType {

    public boolean blankPadded;
    public boolean caseInsensitive;
    protected GExtInt length = new GExtInt(50);

    @Override
    public GCompare[] getFilterCompares() {
        return GCompare.values();
    }

    @Override
    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public GCompare getDefaultCompare() {
        return GCompare.CONTAINS;
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        int minCharWidth = getMinimumCharWidth(minimumCharWidth);
        return font == null || font.size == null ? minCharWidth * 10 : minCharWidth * font.size * 5 / 8;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        int prefCharWidth = getPreferredCharWidth(preferredCharWidth);
        return font == null || font.size == null ? prefCharWidth * 10 : prefCharWidth * font.size * 5 / 8;
    }

    private String minimumMask;
    private String preferredMask;

    public GStringType() {}

    public GStringType(int length) {
        this(new GExtInt(length), false, true);
    }

    public GStringType(GExtInt length, boolean caseInsensitive, boolean blankPadded) {

        this.blankPadded = blankPadded;
        this.caseInsensitive = caseInsensitive;
        this.length = length;

        if(length.isUnlimited()) {
            minimumMask = "999 999";
            preferredMask = "9 999 999";
        } else {
            int lengthValue = length.getValue();
            minimumMask = GwtSharedUtils.replicate('0', correctMinimumCharWidth(lengthValue));
            preferredMask = GwtSharedUtils.replicate('0', correctPreferredCharWidth(lengthValue));
        }
    }

    @Override
    public int getMinimumPixelHeight(GFont font) {
        if(length.isUnlimited())
            return super.getMinimumPixelHeight(font) * 4;
        return super.getMinimumPixelHeight(font);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        if(length.isUnlimited())
            return new TextGridCellRenderer(property);
        return new StringGridCellRenderer(property, !blankPadded);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        if(length.isUnlimited())
            return new TextGridCellEditor(editManager, editProperty);
        return new StringGridCellEditor(editManager, editProperty, !blankPadded);
    }

    @Override
    public String getMinimumMask() {
        return minimumMask;
    }

    public String getPreferredMask() {
        return preferredMask;
    }

    private int correctMinimumCharWidth(int charWidth) {
        return charWidth <= 3
                ? charWidth
                : charWidth <= 40
                    ? (int) Math.round(Math.pow(charWidth, 0.87))
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
    public String toString() {
        return "Строка" + (caseInsensitive ? " без регистра" : "") + (blankPadded ? " с паддингом" : "") + "(" + length + ")";
    }
}
