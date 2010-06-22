package platform.client.form.cell;

import platform.client.logics.ClientCellView;
import platform.client.form.ClientForm;
import platform.client.form.ClientFormLayout;

import javax.swing.*;
import java.io.IOException;

public class CellController implements CellViewListener {

    private ClientCellView key;

    protected ClientCellView getKey() {
        return key;
    }

    protected final CellView view;

    // возвращаем только как компоненту, большего пока не надо
    public JComponent getView() {
        return view.getComponent();
    }

    protected final ClientForm form;

    // форма нужна, поскольку ObjectEditor'у она нужна, чтобы создать диалог
    public CellController(ClientCellView ikey, final ClientForm iform) {

        key = ikey;
        form = iform;

        view = key.getPanelComponent(form);

        if (key.focusable != null)
            view.getComponent().setFocusable(key.focusable);
        else if (key.editKey != null)
            view.getComponent().setFocusable(false);

        view.addListener(this);
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(key, getView());
    }

    public void removeView(ClientFormLayout formLayout) {
        formLayout.remove(key, getView());
    }

    public void setValue(Object ivalue) {
        view.setValue(ivalue);
    }

    boolean isDataChanging() {
        return true;
    }

    public void startEditing() {
        view.startEditing();
    }

    public boolean cellValueChanged(Object ivalue) {

        try {
            form.changeProperty(getKey(), ivalue, false);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при изменении значения свойства", e);
        }

        return true;
    }

    public void hideViews() {
        getView().setVisible(false);
    }

    public void showViews() {
        getView().setVisible(true);
    }
}