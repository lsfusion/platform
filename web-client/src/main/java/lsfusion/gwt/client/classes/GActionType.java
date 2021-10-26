package lsfusion.gwt.client.classes;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.classes.data.GDataType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ActionCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.gwt.client.form.property.panel.view.ActionPanelRenderer;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;

import java.text.ParseException;

public class GActionType extends GDataType {
    public final static GActionType instance = new GActionType();

    @Override
    public PanelRenderer createPanelRenderer(GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, LinearCaptionContainer captionContainer) {
//        if(captionContainer != null)
//            return new PropertyPanelRenderer(form, controller, property, columnKey, captionContainer);

        return new ActionPanelRenderer(form, controller, property, columnKey, captionContainer);
    }

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ActionCellRenderer(property);
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("Action class doesn't support conversion from string", 0);
    }

    @Override
    public int getDefaultWidth(GFont font, GPropertyDraw propertyDraw) {
        ImageDescription image = propertyDraw.getImage();
        return image != null ? image.width : 0;
        // in theory we should add propertyDraw.caption when it's a panel, but a property panel renderer doesn't do that for label, so don't see why it should be done for action
    }

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeActionCaption();
    }
}
