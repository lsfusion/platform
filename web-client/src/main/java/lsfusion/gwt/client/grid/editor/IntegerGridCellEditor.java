package lsfusion.gwt.client.grid.editor;

import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.classes.GIntegerType;
import lsfusion.gwt.client.grid.EditManager;

public class IntegerGridCellEditor extends IntegralGridCellEditor {
    public IntegerGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(GIntegerType.instance, editManager, property);
    }
}
