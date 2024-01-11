package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

import static lsfusion.gwt.client.base.GwtSharedUtils.multiplyString;

public abstract class StringBasedCellRenderer extends TextBasedCellRenderer {

    private boolean echoSymbols;
    private boolean isVarString;

    protected StringBasedCellRenderer(GPropertyDraw property, boolean isVarString) {
        super(property);

        this.isVarString = isVarString;
        echoSymbols = property.echoSymbols;
    }

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        if (echoSymbols)
            return multiplyString(EscapeUtils.UNICODE_BULLET, 6);

        String string = PValue.getStringValue(value);
        if (!isVarString)
            string = GwtSharedUtils.rtrim(string);

        return string;
    }
}
