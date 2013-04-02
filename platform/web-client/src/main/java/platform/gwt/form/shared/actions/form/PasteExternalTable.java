package platform.gwt.form.shared.actions.form;

import platform.gwt.form.shared.view.GPropertyDraw;

import java.util.List;

public class PasteExternalTable extends FormRequestIndexCountingAction<ServerResponseResult>  {
    public List<GPropertyDraw> properties;
    public String line;

    @SuppressWarnings({"UnusedDeclaration"})
    public PasteExternalTable() {
    }

    public PasteExternalTable(List<GPropertyDraw> properties, String line) {
        this.properties = properties;
        this.line = line;
    }
}
