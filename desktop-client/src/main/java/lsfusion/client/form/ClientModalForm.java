package lsfusion.client.form;

import lsfusion.interop.KeyStrokes;
import lsfusion.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.EventObject;

import static lsfusion.client.SwingUtils.*;

public class ClientModalForm extends JDialog {

    private final KeyEvent initFilterKeyEvent;

    private final RemoteFormInterface remoteForm;

    private ClientFormController form;

    public ClientModalForm(String canonicalName, String formSID, Component owner, final RemoteFormInterface remoteForm) {
        this(canonicalName, formSID, owner, remoteForm, null, false, null);
    }

    public ClientModalForm(String canonicalName, String formSID, Component owner, final RemoteFormInterface remoteForm, byte[] firstChanges, boolean isDialog, EventObject initFilterEvent) {
        super(getWindow(owner), ModalityType.DOCUMENT_MODAL);

        this.remoteForm = remoteForm;

        this.initFilterKeyEvent = initFilterEvent instanceof KeyEvent ? (KeyEvent) initFilterEvent : null;

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        setLayout(new BorderLayout());

        // делаем, чтобы не выглядел как диалог
        getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray, 1));

        form = new ClientFormController(canonicalName, formSID, ClientModalForm.this.remoteForm, firstChanges, null, true, isDialog) {
            @Override
            public void hideForm() {
                hideDialog();
                super.hideForm();
            }
        };

        setTitle(form.getCaption());

        add(form.getLayout(), BorderLayout.CENTER);

        createUIHandlers();
    }

    private void createUIHandlers() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (form != null) {
                    RmiQueue.runAction(new Runnable() {
                        @Override
                        public void run() {
                            form.closePressed();
                        }
                    });
                }
            }
        });
    }

    public void hideDialog() {
        dispose();
        if (form != null) {
            form.closed();
            form = null;
        }
    }

    public void showDialog(boolean showFullScreen) {
        showDialog(showFullScreen, null);
    }

    public void showDialog(boolean showFullScreen, Point onScreen) {
        //вызов pack() делает диалог displayable, что позволяет нормально отрабатывать focus-логике,
        //в частности обработка клавиш происходит в этом диалоге, а не в верхнем окне
        pack();

        setSize(clipToScreen(showFullScreen
                             ? new Dimension(10000, 10000)
                             : calculatePreferredSize(isUndecorated())));

        if (onScreen != null) {
            requestLocation(this, onScreen);
        } else {
            setLocationRelativeTo(null);
        }

        beforeShowDialog();

        setVisible(true);
    }

    protected void beforeShowDialog() {
        int initialFilterPropertyDrawID = -1;
        try {
            Integer filterPropertyDraw = remoteForm.getInitFilterPropertyDraw();
            if (filterPropertyDraw != null) {
                initialFilterPropertyDrawID = filterPropertyDraw;
            }
        } catch (RemoteException ignored) {
        }

        if (initialFilterPropertyDrawID > 0) {
            form.selectProperty(initialFilterPropertyDrawID);
        }

        if (initFilterKeyEvent != null && initialFilterPropertyDrawID > 0 && KeyStrokes.isSuitableDialogFilteringEvent(initFilterKeyEvent)) {
            form.quickEditFilter(initFilterKeyEvent, initialFilterPropertyDrawID);
        } else {
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowActivated(WindowEvent e) {
                    Component defaultComponent = form.getLayout().getFocusTraversalPolicy().getDefaultComponent(form.getLayout());
                    if (defaultComponent != null) {
                        defaultComponent.requestFocusInWindow();
                    }

                    removeWindowListener(this);
                }
            });
        }
    }

    public Dimension calculatePreferredSize(boolean undecorated) {
        //сначала нужно провалидейтать все компоненты, чтобы отработала логика autohide
        form.getLayout().preValidateMainContainer();

        Dimension preferredSize = form.getLayout().getPreferredSize();

        // так как у нас есть только preferredSize самого contentPane, а нам нужен у JDialog
        // сколько будет занимать все "рюшечки" вокруг contentPane мы посчитать не можем, поскольку
        if (!undecorated) {
            preferredSize.width += 20;
            preferredSize.height += 40;
        }

        if (form.hasVisibleGrid()) {
            preferredSize.height += 40;
            preferredSize.height += 35;  // под отборы
        }

        return preferredSize;
    }
}
