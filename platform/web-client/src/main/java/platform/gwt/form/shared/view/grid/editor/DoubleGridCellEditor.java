package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.NumberFormat;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;

public class DoubleGridCellEditor extends TextBasedGridCellEditor {
    private final NumberFormat format;

    public DoubleGridCellEditor(EditManager editManager, GPropertyDraw property) {
        this(editManager, property, NumberFormat.getDecimalFormat());
    }

    public DoubleGridCellEditor(EditManager editManager, GPropertyDraw property, NumberFormat format) {
        super(editManager, property, Style.TextAlign.RIGHT);
        this.format = format;
    }

    @Override
    protected String renderToString(Object value) {
        return value == null ? "" : format.format((Number) value).replaceAll("\\u00A0", "");
    }

    @Override
    protected Object tryParseInputText(String inputText) throws ParseException {
        try {
            return inputText.isEmpty() ? null : format.parse(inputText.replaceAll(" ", ""));
        } catch (NumberFormatException e) {
            throw new ParseException();
        }
    }
}