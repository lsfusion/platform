package lsfusion.gwt.form.shared.view.grid.editor;

import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;

public class LinkGridCellEditor extends StringGridCellEditor {
    public LinkGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, true, 1000);
    }
}