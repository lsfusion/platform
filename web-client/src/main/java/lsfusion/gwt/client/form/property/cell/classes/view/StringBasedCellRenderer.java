package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

import static lsfusion.gwt.client.base.GwtSharedUtils.multiplyString;

public abstract class StringBasedCellRenderer extends SimpleTextBasedCellRenderer {

    private boolean echoSymbols;
    private boolean isVarString;

    protected StringBasedCellRenderer(GPropertyDraw property, boolean isVarString) {
        super(property);

        this.isVarString = isVarString;
        echoSymbols = property.echoSymbols;
    }

    @Override
    public String format(PValue value) {
        if (echoSymbols)
            return multiplyString(EscapeUtils.UNICODE_BULLET, 6);

        String string = PValue.getStringValue(value);
        if (!isVarString)
            string = GwtSharedUtils.rtrim(string);

        return string;
    }
}
