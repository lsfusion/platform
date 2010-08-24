package platform.client.form.cell;

import platform.client.form.ClientFormLayout;
import platform.client.form.ClientFormController;
import platform.client.logics.ClientCell;
import platform.interop.form.screen.ExternalScreenComponent;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class CellController implements CellViewListener {

    private ClientCell key;

    protected ClientCell getKey() {
        return key;
    }

    protected final CellView view;
    private ExternalScreenComponent extView;

    // возвращаем только как компоненту, большего пока не надо
    public JComponent getView() {
        return view.getComponent();
    }

    protected final ClientFormController form;

    // форма нужна, поскольку ObjectEditor'у она нужна, чтобы создать диалог
    public CellController(ClientCell ikey, final ClientFormController iform) {

        key = ikey;
        form = iform;

        view = key.getPanelComponent(form);

        if (key.focusable != null)
            view.getComponent().setFocusable(key.focusable);
        else if (key.editKey != null)
            view.getComponent().setFocusable(false);

        view.addListener(this);

        if (key.editKey != null)
            form.getComponent().addKeyBinding(key.editKey, key.getGroupObject(), new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    startEditing(e);
                }
            });

        if (key.externalScreen != null) {
            extView = new ExternalScreenComponent();
        }
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(key, getView());
        if (key.externalScreen != null)
            key.externalScreen.add(form.getID(), extView, key.externalScreenConstraints);
    }

    public void removeView(ClientFormLayout formLayout) {
        formLayout.remove(key, getView());
        if (key.externalScreen != null)
            key.externalScreen.remove(form.getID(), extView);
    }

    public void setValue(Object ivalue) {
        view.setValue(ivalue);
        if (extView != null) {
            String oldValue = (extView.getValue() == null) ? "" : extView.getValue();
            String newValue = (ivalue == null) ? "" : ivalue.toString();
            if (oldValue.equals(newValue)){
                return;
            }
            extView.setValue((ivalue == null) ? "" : ivalue.toString());
            key.externalScreen.invalidate();
        }
    }

    public void startEditing(KeyEvent e) {
        view.startEditing(e);
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