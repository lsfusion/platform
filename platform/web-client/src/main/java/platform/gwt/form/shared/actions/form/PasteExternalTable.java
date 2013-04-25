package platform.gwt.form.shared.actions.form;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.List;

public class PasteExternalTable extends FormRequestIndexCountingAction<ServerResponseResult>  {
    public List<GPropertyDraw> properties;
    public List<GGroupObjectValue> columnKeys;
    public String line;

    @SuppressWarnings({"UnusedDeclaration"})
    public PasteExternalTable() {
    }

    public PasteExternalTable(List<GPropertyDraw> properties, List<GGroupObjectValue> columnKeys, String line) {
        this.properties = properties;
        this.columnKeys = columnKeys;
        this.line = line;
    }
}
