package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

import static lsfusion.gwt.client.base.GwtSharedUtils.multiplyString;

public abstract class StringBasedCellRenderer extends TextBasedCellRenderer {

    private final boolean isVarString;

    protected StringBasedCellRenderer(GPropertyDraw property, boolean isVarString) {
        super(property);

        this.isVarString = isVarString;
    }

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        if (property.echoSymbols)
            return multiplyString(EscapeUtils.UNICODE_BULLET, 6);

        String text = PValue.getStringValue(value);

        if (text != null && !isMultiLine() && !property.collapse)
            text = replaceLineBreaks(text);

        if (!isVarString)
            text = GwtSharedUtils.rtrim(text);

        return text;
    }

    private static String replaceLineBreaks(String text) {
        return text.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ');
    }
}
