package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.form.screen.ExternalScreenComponent;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class PropertyController implements CellViewListener {

    protected ClientGroupObjectValue columnKey;
    protected ClientPropertyDraw key;

    protected ClientPropertyDraw getKey() {
        return key;
    }

    protected final CellView view;
    protected ExternalScreenComponent extView;

    // возвращаем только как компоненту, большего пока не надо
    public JComponent getView() {
        return view.getComponent();
    }

    protected final ClientFormController form;

    // форма нужна, поскольку ObjectEditor'у она нужна, чтобы создать диалог
    public PropertyController(ClientPropertyDraw key, final ClientFormController form, ClientGroupObject groupObject, ClientGroupObjectValue columnKey) {

        this.key = key;
        this.form = form;
        this.columnKey = columnKey;

        view = key.getPanelComponent(form, columnKey);
        view.setHighlightColor(groupObject != null ? groupObject.grid.highlightColor : null);
        view.setCaption(key.getFullCaption());

        if (key.focusable != null)
            view.getComponent().setFocusable(key.focusable);
        else if (key.editKey != null)
            view.getComponent().setFocusable(false);

        view.addListener(this);

        if (key.editKey != null)
            form.getComponent().addKeyBinding(key.editKey, key.getKeyBindingGroup(), new KeyAdapter() {
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
            form.changePropertyDraw(getKey(), ivalue, false, columnKey);
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

    public void setCaption(String caption) {
        view.setCaption(caption);
    }

    public void setHighlight(Object highlight) {
        view.setHighlight(highlight);
    }

}