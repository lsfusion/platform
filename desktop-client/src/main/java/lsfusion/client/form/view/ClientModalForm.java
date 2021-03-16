package lsfusion.client.form.view;

import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.interop.form.event.KeyStrokes;
import lsfusion.interop.form.remote.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.EventObject;

import static lsfusion.client.base.SwingUtils.*;

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

        form = new ClientFormController(canonicalName, formSID, ClientModalForm.this.remoteForm, firstChanges, null, true, isDialog) {
            @Override
            public void onFormHidden() {
                hideDialog();
                super.onFormHidden();
            }

            @Override
            public void setFormCaption(String caption, String tooltip) {
                setCaption(caption, tooltip);
            }
        };

        add(form.getLayout(), BorderLayout.CENTER);

        createUIHandlers();
    }

    public void setCaption(String caption, String tooltip) {
        setTitle(caption);
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
                             : calculateMaxPreferredSize(isUndecorated())));

        if (onScreen != null) {
            requestLocation(this, onScreen);
        } else {
            setLocationRelativeTo(getOwner());
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
                    SwingUtilities.invokeLater(() -> {
                        if (defaultComponent != null) {
                            defaultComponent.requestFocusInWindow();
                        } else if (!form.activateFirstComponents()) {
                            form.focusFirstComponent();
                        }
                    });
                    
                    removeWindowListener(this);
                }
            });
        }
    }

    public Dimension calculateMaxPreferredSize(boolean undecorated) {
        //сначала нужно провалидейтать все компоненты, чтобы отработала логика autohide
//        form.getLayout().preValidateMainContainer();

        Dimension preferredSize = form.getLayout().getMaxPreferredSize();

        // так как у нас есть только size самого contentPane, а нам нужен у JDialog
        // сколько будет занимать все "рюшечки" вокруг contentPane мы посчитать не можем, поскольку
        if (!undecorated) {
            preferredSize.width += 22;
            preferredSize.height += 40;
        }

        return preferredSize;
    }
}
