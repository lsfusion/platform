package platform.client.form.cell;

import platform.client.logics.ClientCellView;
import platform.client.form.ClientForm;
import platform.client.form.ClientFormLayout;

import javax.swing.*;

public class CellController {

    private ClientCellView key;
    protected ClientCellView getKey() {
        return key;
    }

    protected final CellView view;

    // возвращаем только как компоненту, большего пока не надо
    public JComponent getView() {
        return view;
    }

    protected final ClientForm form;

    // форма нужна, поскольку ObjectEditor'у она нужна, чтобы создать диалог
    protected CellController(ClientCellView ikey, final ClientForm iform) {

        key = ikey;
        form = iform;

        view = new CellView(key) {

            protected ClientCellView getKey() {
                return key;
            }

            protected boolean cellValueChanged(Object value) {
                 return CellController.this.cellValueChanged(value);
            }

            protected boolean isDataChanging() {
                return CellController.this.isDataChanging();
            }

            protected ClientForm getForm() {
                return form;
            }
        };
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(key, view);
    }

    public void removeView(ClientFormLayout formLayout) {
        formLayout.remove(key, view);
    }

    public void setValue(Object ivalue) {
        view.setValue(ivalue);
    }

    boolean isDataChanging() {
        return true;
    }

    protected boolean cellValueChanged(Object value) {
        return true;
    }

    public void startEditing() {
        view.startEditing();
    }
}