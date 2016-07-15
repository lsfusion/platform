package lsfusion.client.logics.classes;

import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.renderer.IntegerPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.EventObject;

import static lsfusion.client.form.EditBindingMap.EditEventFilter;
import static lsfusion.interop.KeyStrokes.isSuitableNumberEditEvent;

abstract public class ClientIntegralClass extends ClientDataClass {

    private final static char UNBREAKABLE_SPACE = '\u00a0';

    public static final EditEventFilter numberEditEventFilter = new EditEventFilter() {
        public boolean accept(EventObject e) {
            // e = null, когда editCellAt() вызывается программно. фиксил начало редактирования по editKey
            return e == null || isSuitableNumberEditEvent(e);
        }
    };

    protected ClientIntegralClass() {
    }

    @Override
    public String getMinimumMask() {
        return "99 999 999";
    }

    public String getPreferredMask() {
        return "99 999 999";
    }

    public NumberFormat getDefaultFormat() {
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(true);
        return format;
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        return getDefaultFormat().format(obj);
    }

    protected Number parseWithDefaultFormat(String s) throws ParseException {
        ParsePosition pp = new ParsePosition(0);
        NumberFormat format = getDefaultFormat();
        if (format instanceof DecimalFormat) {
            DecimalFormat decimalFormat = (DecimalFormat) format;
            // если в системных настройках для разделения разрядов используется пробел, то java использует 'неразрывный пробел'
            // http://bugs.sun.com/view_bug.do?bug_id=4510618
            if (decimalFormat.getDecimalFormatSymbols().getGroupingSeparator() == UNBREAKABLE_SPACE) {
                s = s.replace(' ', UNBREAKABLE_SPACE);
            }
        }

        Number parsed = format.parse(s, pp);
        if (pp.getIndex() != s.length()) {
            throw new NumberFormatException("Wrong input's format: \"" + s + "\"");
        }
        return parsed;
    }

    @Override
    public EditEventFilter getEditEventFilter() {
        return numberEditEventFilter;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new IntegerPropertyRenderer(property);
    }

    protected abstract PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property);
}
