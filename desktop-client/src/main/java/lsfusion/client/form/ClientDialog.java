package lsfusion.client.form;

import com.google.common.base.Throwables;
import lsfusion.interop.KeyStrokes;
import lsfusion.interop.form.RemoteDialogInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.util.EventObject;

public class ClientDialog extends ClientModalForm {

    public KeyEvent initFilterKeyEvent = null;

    private RemoteDialogInterface remoteDialog;

    public ClientDialog(Component owner, final RemoteDialogInterface dialog, EventObject initFilterEvent) {
        super(owner, dialog, true);

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
    public void beforeShowDialog() {
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
            super.beforeShowDialog();
        }
    }
}
