package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.classes.GIntegralType;
import lsfusion.gwt.form.shared.view.grid.EditManager;

import java.text.ParseException;

public class IntegralGridCellEditor extends TextBasedGridCellEditor {
    private final static String UNBREAKABLE_SPACE = "\u00a0";
    
    protected static final NumberFormat format = NumberFormat.getDecimalFormat();

    protected final GIntegralType type;

    public IntegralGridCellEditor(GIntegralType type, EditManager editManager, GPropertyDraw property) {
        super(editManager, property, Style.TextAlign.RIGHT);
        this.type = type;
    }

    @Override
    protected String renderToString(Object value) {
        if (value != null) {
            assert value instanceof Number;
            return format.format((Number) value);
        }
        return "";
    }

    @Override
    protected Object tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        if (inputText.isEmpty() || (onCommit && "-".equals(inputText))) {
            return null;
        } else {
            inputText = inputText.replace(" ", "").replace(UNBREAKABLE_SPACE, "");
            return (!onCommit && "-".equals(inputText)) ? true : type.parseString(inputText);
        }
    }
}