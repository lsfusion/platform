package platform.client.form;

import com.google.common.base.Throwables;
import platform.interop.KeyStrokes;
import platform.interop.form.RemoteDialogInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.util.EventObject;

public class ClientDialog extends ClientModalForm {

    // todo: удалить все эти поля вмест с ObjectPropertyEditor
    public final static int NOT_CHOSEN = 0;
    public final static int VALUE_CHOSEN = 1;

    public int result = NOT_CHOSEN;
    public Object dialogValue;
    public Object displayValue;

    public KeyEvent initFilterKeyEvent = null;

    private RemoteDialogInterface remoteDialog;

    public ClientDialog(Component owner, final RemoteDialogInterface dialog, EventObject initFilterEvent, boolean isDialog) {
        super(owner, dialog, isDialog);

        remoteDialog = dialog;

        this.initFilterKeyEvent = initFilterEvent instanceof KeyEvent ? (KeyEvent) initFilterEvent : null;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setupUndecorated();
    }

    protected void setupUndecorated() {
        try {
            Boolean undecorated = remoteDialog.isUndecorated();
            if (undecorated == null || undecorated) {
                setResizable(false);
                // делаем, чтобы не выглядел как диалог
                setUndecorated(true);
                ((JPanel)getContentPane()).setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            }
        } catch (RemoteException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public void windowActivatedFirstTime() {
        int initialFilterPropertyDrawID = -1;
        try {
            Integer filterPropertyDraw = remoteDialog.getInitFilterPropertyDraw();
            if (filterPropertyDraw != null) {
                initialFilterPropertyDrawID = filterPropertyDraw;
            }
        } catch (RemoteException ignored) {
        }

        if (initialFilterPropertyDrawID > 0) {
            form.selectProperty(initialFilterPropertyDrawID);
        }

        if (initFilterKeyEvent != null && initialFilterPropertyDrawID > 0 &&
                KeyStrokes.isSuitableStartFilteringEvent(initFilterKeyEvent)) {
            form.quickEditFilter(initFilterKeyEvent, initialFilterPropertyDrawID);
        } else {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(form.getComponent());
        }
    }
}
