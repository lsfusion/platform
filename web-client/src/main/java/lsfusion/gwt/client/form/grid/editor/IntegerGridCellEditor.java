package lsfusion.gwt.client.form.grid.editor;

import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.classes.GIntegerType;
import lsfusion.gwt.client.form.grid.EditManager;

public class IntegerGridCellEditor extends IntegralGridCellEditor {
    public IntegerGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(GIntegerType.instance, editManager, property);
    }
}
