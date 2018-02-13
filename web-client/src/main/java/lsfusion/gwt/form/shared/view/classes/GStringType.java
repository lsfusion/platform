package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.shared.view.GExtInt;
import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GFontMetrics;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.filter.GCompare;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.StringGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.TextGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.rich.RichTextGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.StringGridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.TextGridCellRenderer;

import java.text.ParseException;

import static java.lang.Math.pow;
import static java.lang.Math.round;

public class GStringType extends GDataType {

    public boolean blankPadded;
    public boolean caseInsensitive;
    public boolean rich;
    protected GExtInt length = new GExtInt(50);

    @Override
    public GCompare[] getFilterCompares() {
        return GCompare.values();
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        return s;
    }

    @Override
    public GCompare getDefaultCompare() {
        return caseInsensitive ? GCompare.CONTAINS : GCompare.EQUALS;
    }

    @Override
    public int getPixelWidth(int minimumCharWidth, GFont font, String pattern) {
        int minCharWidth = getCharWidth(minimumCharWidth, pattern);
        return minCharWidth * GFontMetrics.getZeroSymbolWidth(font == null || font.size == null ? null : font) + 8;
    }

    private String mask;

    public GStringType() {}

    public GStringType(int length) {
        this(new GExtInt(length), false, true, false);
    }

    public GStringType(GExtInt length, boolean caseInsensitive, boolean blankPadded, boolean rich) {

        this.blankPadded = blankPadded;
        this.caseInsensitive = caseInsensitive;
        this.rich = rich;
        this.length = length;

        if (length.isUnlimited()) {
            mask = "999 999";
        } else {
            int lengthValue = length.getValue();
            mask = GwtSharedUtils.replicate('0', lengthValue <= 12 ? lengthValue : (int) round(12 + pow(lengthValue - 12, 0.7)));
        }
    }

    @Override
    public int getPixelHeight(GFont font) {
        if (length.isUnlimited()) {
            return super.getPixelHeight(font) * 4;
        }
        return super.getPixelHeight(font);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        if (length.isUnlimited()) {
            return new TextGridCellRenderer(property, rich);
        }
        return new StringGridCellRenderer(property, !blankPadded);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        if (length.isUnlimited()) {
            return rich ? new RichTextGridCellEditor(editManager, editProperty) : new TextGridCellEditor(editManager, editProperty);
        }
        return new StringGridCellEditor(editManager, editProperty, !blankPadded, length.getValue());
    }

    @Override
    public String getMask(String pattern) {
        return mask;
    }

    @Override
    public int getCharWidth(int definedMinimumCharWidth, String pattern) {
        return definedMinimumCharWidth > 0 ? definedMinimumCharWidth : mask.length();
    }

    @Override
    public String toString() {
        return "Строка" + (caseInsensitive ? " без регистра" : "") + (blankPadded ? " с паддингом" : "") + (rich ? " rich" : "") + "(" + length + ")";
    }
}
