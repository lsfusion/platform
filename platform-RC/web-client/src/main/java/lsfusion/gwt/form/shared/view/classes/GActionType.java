package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GFontMetrics;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.grid.renderer.ActionGridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.shared.view.panel.ActionPanelRenderer;
import lsfusion.gwt.form.shared.view.panel.PanelRenderer;

import java.text.ParseException;

public class GActionType extends GDataType {
    public final static GActionType instance = new GActionType();

    @Override
    public PanelRenderer createPanelRenderer(GFormController form, GPropertyDraw property, GGroupObjectValue columnKey) {
        return new ActionPanelRenderer(form, property, columnKey);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ActionGridCellRenderer(property);
    }

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, GFont font, String pattern) {
        return getPreferredPixelWidth(maximumCharWidth, font, pattern);
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("Action class doesn't support conversion from string", 0);
    }

    @Override
    public int getPreferredPixelHeight(GFont font) {
        int result =  font == null || font.size == null ? 13 : GFontMetrics.getSymbolHeight(font);
        return result + 5;
    }

    @Override
    public String getPreferredMask(String pattern) {
        return "1234567";
    }

    @Override
    public String toString() {
        return "Класс действия";
    }
}
