package lsfusion.gwt.client.classes;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.classes.data.GDataType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.view.ActionCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.panel.view.ActionOrPropertyValueController;
import lsfusion.gwt.client.form.property.panel.view.ActionPanelRenderer;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;
import lsfusion.gwt.client.form.property.panel.view.PropertyPanelRenderer;

import java.text.ParseException;

public class GActionType extends GDataType {
    public final static GActionType instance = new GActionType();

    @Override
    public PanelRenderer createPanelRenderer(GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, Result<CaptionWidget> captionContainer) {
        if(property.isAlignCaption() && captionContainer != null)
            return new PropertyPanelRenderer(form, controller, property, columnKey, captionContainer);
        return new ActionPanelRenderer(form, controller, property, columnKey);
    }

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new ActionCellRenderer(property);
    }

    @Override
    public PValue parseString(String s, String pattern) throws ParseException {
        throw new ParseException("Action class doesn't support conversion from string", 0);
    }

    @Override
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean globalCaptionIsDrawn) {
        GSize result = globalCaptionIsDrawn ? GSize.ZERO : super.getDefaultWidth(font, propertyDraw, globalCaptionIsDrawn);

        GSize imageWidth = propertyDraw.getImageWidth(font);
        if (imageWidth != null)
            result = result.add(imageWidth);

        return result.add(GSize.CONST(30)); // paddings in btn
    }

    @Override
    public GSize getDefaultHeight(GFont font, GPropertyDraw propertyDraw, boolean globalCaptionIsDrawn) {
        GSize height = globalCaptionIsDrawn ? GSize.ZERO : super.getDefaultHeight(font, propertyDraw, globalCaptionIsDrawn);

        final GSize imageHeight = propertyDraw.getImageHeight(font);
        if (imageHeight != null)
            height = height.max(imageHeight);

        return height;
    }

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        String panelCaption = propertyDraw.getPanelCaption(propertyDraw.caption);
        return panelCaption != null ? panelCaption : "";
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeActionCaption();
    }
}
