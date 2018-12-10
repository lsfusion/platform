package lsfusion.gwt.form.client.grid.editor;

import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.classes.GLongType;
import lsfusion.gwt.form.client.grid.EditManager;

public class LongGridCellEditor extends IntegralGridCellEditor {
    public LongGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(GLongType.instance, editManager, property);
    }
}
