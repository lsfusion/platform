package lsfusion.gwt.client.grid.editor;

import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.client.grid.EditManager;

public class LinkGridCellEditor extends StringGridCellEditor {
    public LinkGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, true, 1000);
    }
}