package platform.client.form.panel;

import platform.client.form.cell.CellController;
import platform.client.form.ClientForm;
import platform.client.logics.ClientCellView;

import java.io.IOException;

public class PanelCellController extends CellController {

    public PanelCellController(ClientCellView ikey, ClientForm form) {
        super(ikey, form);
    }

    protected boolean cellValueChanged(Object ivalue) {

        try {
            form.changeProperty(getKey(), ivalue);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при изменении значения свойства", e);
        }

        return true;
    }

    public void hideViews() {
        view.setVisible(false);
    }

    public void showViews() {
        view.setVisible(true);
    }
}
