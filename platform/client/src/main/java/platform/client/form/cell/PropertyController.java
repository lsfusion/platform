package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.form.screen.ExternalScreenComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class PropertyController {

    protected ClientGroupObjectValue columnKey;
    protected ClientPropertyDraw key;

    public ClientPropertyDraw getKey() {
        return key;
    }

    protected final PanelView view;
    protected ExternalScreenComponent extView;

    // возвращаем только как компоненту, большего пока не надо
    public JComponent getView() {
        return view.getComponent();
    }

    public PanelView getPanelView() {
        return view;
    }

    protected final ClientFormController form;

    // форма нужна, поскольку ObjectEditor'у она нужна, чтобы создать диалог
    public PropertyController(ClientPropertyDraw key, final ClientFormController form, ClientGroupObjectValue columnKey) {

        this.key = key;
        this.form = form;
        this.columnKey = columnKey;

        view = key.getPanelView(form, columnKey);

        if (key.focusable != null) {
            view.getComponent().setFocusable(key.focusable);
        } else if (key.editKey != null) {
            view.getComponent().setFocusable(false);
        }

        if(key.drawAsync)
            form.setAsyncView(view);

        if (key.editKey != null) {
            form.getComponent().addKeyBinding(key.editKey, key.groupObject, new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    forceEdit();
                }
            });
        }

        if (key.externalScreen != null) {
            extView = new ExternalScreenComponent();
        }
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(key, getView());
        if (key.externalScreen != null) {
            key.externalScreen.add(form.getID(), extView, key.externalScreenConstraints);
        }
    }

    public void removeView(ClientFormLayout formLayout) {
        formLayout.remove(key, getView());
        if (key.externalScreen != null) {
            key.externalScreen.remove(form.getID(), extView);
        }
    }

    public void setValue(Object ivalue) {
        view.setValue(ivalue);
        if (extView != null) {
            String oldValue = (extView.getValue() == null) ? "" : extView.getValue();
            String newValue = (ivalue == null) ? "" : ivalue.toString();
            if (oldValue.equals(newValue)) {
                return;
            }
            extView.setValue((ivalue == null) ? "" : ivalue.toString());
            key.externalScreen.invalidate();
        }
    }

    public void forceEdit() {
        view.forceEdit();
    }

    public void setVisible(boolean visible) {
        getView().setVisible(visible);
    }

    public void setCaption(String caption) {
        view.setCaption(caption);
        view.setToolTip(caption);
    }

    public void setBackgroundColor(Color color) {
        view.setBackgroundColor(color);
    }

    public void setForegroundColor(Color color) {
        view.setForegroundColor(color);
    }
}