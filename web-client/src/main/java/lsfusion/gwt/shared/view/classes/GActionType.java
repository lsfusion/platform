package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.ui.GFormController;
import lsfusion.gwt.client.form.ui.grid.renderer.ActionGridCellRenderer;
import lsfusion.gwt.client.form.ui.grid.renderer.GridCellRenderer;
import lsfusion.gwt.client.form.ui.panel.ActionPanelRenderer;
import lsfusion.gwt.client.form.ui.panel.PanelRenderer;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.changes.GGroupObjectValue;

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
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("Action class doesn't support conversion from string", 0);
    }

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return "1234567";
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeActionCaption();
    }
}
