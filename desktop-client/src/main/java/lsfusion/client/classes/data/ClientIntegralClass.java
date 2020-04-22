package lsfusion.client.classes.data;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.IntegralPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.EventObject;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static lsfusion.client.form.property.cell.EditBindingMap.EditEventFilter;
import static lsfusion.interop.form.event.KeyStrokes.isSuitableNumberEditEvent;

abstract public class ClientIntegralClass extends ClientFormatClass<NumberFormat> {

    private final static char UNBREAKABLE_SPACE = '\u00a0';

    public static final EditEventFilter numberEditEventFilter = new EditEventFilter() {
        public boolean accept(EventObject e) {
            // e = null, когда editCellAt() вызывается программно. фиксил начало редактирования по changeKey
            return e == null || isSuitableNumberEditEvent(e);
        }
    };

    protected ClientIntegralClass() {
    }

    protected abstract int getPrecision();
    
    @Override
    public int getDefaultCharWidth() {
        int lengthValue = getPrecision();
        return BaseUtils.min(lengthValue <= 6 ? lengthValue : (int) round(6 + pow(lengthValue - 6, 0.7)), 10);
    }

    public NumberFormat getDefaultFormat() {
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(true);
        return format;
    }

    @Override
    public NumberFormat createUserFormat(String pattern) {
        return new DecimalFormat(pattern);
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        NumberFormat format = getDefaultFormat();
        String formattedString = format.format(obj);
        if (format instanceof DecimalFormat) {
            // аналогично parse'ингу: если в системных настройках для разделения разрядов используется пробел, то java использует 'неразрывный пробел'
            // excel, например, не воспринимает полученную строку как число
            if (((DecimalFormat) format).getDecimalFormatSymbols().getGroupingSeparator() == UNBREAKABLE_SPACE) {
                formattedString = formattedString.replace(UNBREAKABLE_SPACE, ' ');
            }
        }
        return formattedString;
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
        return new IntegralPropertyRenderer(property);
    }

    protected abstract PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property);
}
