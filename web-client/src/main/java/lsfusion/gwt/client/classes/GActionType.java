package lsfusion.gwt.client.classes;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.classes.data.GDataType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
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
        return new ActionPanelRenderer(form, controller, property, columnKey);
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
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean needNotNull, boolean globalCaptionIsDrawn) {
        if(needNotNull) {
            GSize result = globalCaptionIsDrawn ? GSize.ZERO : super.getDefaultWidth(font, propertyDraw, needNotNull, globalCaptionIsDrawn);

            ImageDescription image = propertyDraw.getImage();
            if (image != null)
                result = result.add(image.getWidth());

            return result;
        }

        return null;
    }

    @Override
    public GSize getDefaultHeight(GFont font, GPropertyDraw propertyDraw, boolean needNotNull, boolean globalCaptionIsDrawn) {
        if(needNotNull) {
            GSize height = globalCaptionIsDrawn ? GSize.ZERO : super.getDefaultHeight(font, propertyDraw, needNotNull, globalCaptionIsDrawn);

            final ImageDescription image = propertyDraw.getImage();
            GSize imageHeight;
            if (image != null && (imageHeight = image.getHeight()) != null)
                height = height.max(imageHeight);

            return height;
        }

        return null;
    }

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return propertyDraw.getPanelCaption(GGridPropertyTable.getPropertyCaption(propertyDraw));
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeActionCaption();
    }
}
