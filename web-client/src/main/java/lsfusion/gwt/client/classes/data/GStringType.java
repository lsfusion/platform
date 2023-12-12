package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.classes.GTextBasedType;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.StringCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.StringCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static lsfusion.gwt.client.form.filter.user.GCompare.*;

public class GStringType extends GTextBasedType {

    public boolean blankPadded;
    public boolean caseInsensitive;
    protected GExtInt length = new GExtInt(50);

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, CONTAINS, MATCH};
    }

    @Override
    public PValue parseString(String s, String pattern) throws ParseException {
        return PValue.getPValue(s);
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

        return getScaledCharWidth(length.getValue());
    }

    // the same is on the server
    private static int getScaledCharWidth(int lengthValue) {
        return lengthValue <= 12 ? Math.max(lengthValue, 1) : (int) round(12 + pow(lengthValue - 12, 0.7));
    }

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new StringCellRenderer(property, !blankPadded);
    }

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new StringCellEditor(editManager, editProperty, !blankPadded, length.isUnlimited() ? Integer.MAX_VALUE : length.getValue(), inputList, inputListActions, editContext);
    }

    private final static GStringType text = new GStringType(GExtInt.UNLIMITED, false, false);
    @Override
    public GType getFilterMatchType() {
        return text;
    }

    @Override
    public String toString() {
        ClientMessages messages = ClientMessages.Instance.get();
        return messages.typeStringCaption() + 
                (caseInsensitive ? " " + messages.typeStringCaptionRegister() : "") + 
                (blankPadded ? " " + messages.typeStringCaptionPadding() : "") + 
                "(" + length + ")";
    }

    @Override
    public boolean isId() {
        return true;
    }
}
