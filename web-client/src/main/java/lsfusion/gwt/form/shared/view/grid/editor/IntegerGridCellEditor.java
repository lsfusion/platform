package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.Style;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.classes.GIntegerType;
import lsfusion.gwt.form.shared.view.grid.EditManager;

public class IntegerGridCellEditor extends TextBasedGridCellEditor {
    public IntegerGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, Style.TextAlign.RIGHT);
    }

    @Override
    protected Object tryParseInputText(String inputText) throws ParseException {
        try {
            return GIntegerType.instance.parseString(inputText);
        } catch (Exception e) {
            throw new ParseException();
        }
    }

    @Override
    protected boolean isStringValid(String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
