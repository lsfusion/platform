package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.form.screen.ExternalScreenComponent;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class PropertyController implements CellViewListener {

    protected ClientGroupObjectValue columnKey;
    protected ClientPropertyDraw key;

    public ClientPropertyDraw getKey() {
        return key;
    }

    protected final CellView view;
    protected ExternalScreenComponent extView;

    // возвращаем только как компоненту, большего пока не надо
    public JComponent getView() {
        return view.getComponent();
    }

    public CellView getCellView() {
        return view;
    }

    protected final ClientFormController form;

    // форма нужна, поскольку ObjectEditor'у она нужна, чтобы создать диалог
    public PropertyController(ClientPropertyDraw key, final ClientFormController form, ClientGroupObjectValue columnKey) {

        this.key = key;
        this.form = form;
        this.columnKey = columnKey;

        view = key.getPanelComponent(form, columnKey);
        view.setCaption(key.getFullCaption());

        if (key.focusable != null) {
            view.getComponent().setFocusable(key.focusable);
        } else if (key.editKey != null) {
            view.getComponent().setFocusable(false);
        }

        view.setListener(this);

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

    public void changeValue(Object ivalue, boolean aggValue) {
//        try {
//            form.executeEditAction(getKey(), columnKey, EditActionResult.CHANGE);
//            form.changePropertyDraw(getKey(), columnKey, ivalue, false, aggValue);
//        } catch (IOException e) {
//            throw new RuntimeException(ClientResourceBundle.getString("errors.error.changing.property.value"), e);
//        }

        //todo: remove... along with interface
    }

    public void hideViews() {
        getView().setVisible(false);
    }

    public void showViews() {
        getView().setVisible(true);
    }

    public void setCaption(String caption) {
        view.setCaption(caption);
        view.setToolTip(caption);
    }

    public void setBackground(Object color) {
        view.setBackground(color);
    }

    public void setForeground(Object color) {
        view.setForeground(color);
    }
}