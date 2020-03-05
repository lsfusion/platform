package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.StringGridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.StringGridCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

import java.text.ParseException;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static lsfusion.gwt.client.form.filter.user.GCompare.*;

public class GStringType extends GDataType {

    public boolean blankPadded;
    public boolean caseInsensitive;
    protected GExtInt length = new GExtInt(50);

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS, START_WITH, CONTAINS, ENDS_WITH, LIKE};
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        return s;
    }

    @Override
    public GCompare getDefaultCompare() {
        return caseInsensitive ? GCompare.CONTAINS : GCompare.EQUALS;
    }

    public GStringType() {}

    public GStringType(GExtInt length, boolean caseInsensitive, boolean blankPadded) {

        this.blankPadded = blankPadded;
        this.caseInsensitive = caseInsensitive;
        this.length = length;
    }

    @Override
    public int getDefaultCharWidth() {
        if(length.isUnlimited())
            return 15;

        int lengthValue = length.getValue();
        return lengthValue <= 12 ? Math.max(lengthValue, 1) : (int) round(12 + pow(lengthValue - 12, 0.7));
    }

    @Override
    public AbstractGridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new StringGridCellRenderer(property, !blankPadded);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new StringGridCellEditor(editManager, editProperty, !blankPadded, length.isUnlimited() ? Integer.MAX_VALUE : length.getValue());
    }

    @Override
    public String toString() {
        ClientMessages messages = ClientMessages.Instance.get();
        return messages.typeStringCaption() + 
                (caseInsensitive ? " " + messages.typeStringCaptionRegister() : "") + 
                (blankPadded ? " " + messages.typeStringCaptionPadding() : "") + 
                "(" + length + ")";
    }
}
