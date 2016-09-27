package lsfusion.client.form.editor;

import lsfusion.client.logics.ClientPropertyDraw;

import java.awt.*;
import java.util.EventObject;

public class LinkPropertyEditor extends StringPropertyEditor {

    public LinkPropertyEditor(ClientPropertyDraw property, Object value) {
        super(property, value, 1000, true, false);
    }

    @Override
    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return super.getComponent(tableLocation, cellRectangle, editEvent);
    }
}