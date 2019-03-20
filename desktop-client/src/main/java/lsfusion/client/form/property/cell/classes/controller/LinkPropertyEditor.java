package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.form.property.ClientPropertyDraw;

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