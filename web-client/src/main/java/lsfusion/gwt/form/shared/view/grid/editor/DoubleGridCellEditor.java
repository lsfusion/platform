package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.classes.GDoubleType;
import lsfusion.gwt.form.shared.view.grid.EditManager;

public class DoubleGridCellEditor extends IntegralGridCellEditor {
    protected static final NumberFormat format = NumberFormat.getDecimalFormat();

    public DoubleGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(GDoubleType.instance, editManager, property);
    }
}