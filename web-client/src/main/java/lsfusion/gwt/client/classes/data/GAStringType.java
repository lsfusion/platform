package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.classes.GTextBasedType;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GExtInt;
import lsfusion.gwt.client.form.property.PValue;

import java.text.ParseException;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static lsfusion.gwt.client.form.filter.user.GCompare.*;

public abstract class GAStringType extends GTextBasedType {

    public boolean blankPadded;
    public boolean caseInsensitive;
    protected GExtInt length = new GExtInt(50);

    public GAStringType() {
    }

    public GAStringType(GExtInt length, boolean caseInsensitive, boolean blankPadded) {
        this.blankPadded = blankPadded;
        this.caseInsensitive = caseInsensitive;
        this.length = length;
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, CONTAINS, MATCH};
    }

    @Override
    public PValue parseString(String s, String pattern) throws ParseException {
        return PValue.getPValue(s);
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

}
