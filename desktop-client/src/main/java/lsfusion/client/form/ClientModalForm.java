package lsfusion.client.form;

import lsfusion.interop.KeyStrokes;
import lsfusion.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.EventObject;

import static lsfusion.client.SwingUtils.*;

public class ClientModalForm extends JDialog {

    private final KeyEvent initFilterKeyEvent;

    private final RemoteFormInterface remoteForm;

    private ClientFormController form;

    public ClientModalForm(String formSID, Component owner, final RemoteFormInterface remoteForm) {
        this(formSID, owner, remoteForm, false, null);
    }

    public ClientModalForm(String formSID, Component owner, final RemoteFormInterface remoteForm, boolean isDialog, EventObject initFilterEvent) {
        super(getWindow(owner), ModalityType.DOCUMENT_MODAL);

        this.remoteForm = remoteForm;

        this.initFilterKeyEvent = initFilterEvent instanceof KeyEvent ? (KeyEvent) initFilterEvent : null;

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        setLayout(new BorderLayout());

        // делаем, чтобы не выглядел как диалог
        getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray, 1));

        form = new ClientFormController(formSID, ClientModalForm.this.remoteForm, null, true, isDialog) {
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
                form.closePressed();
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                // если скрываем, то всегда делаем dipose()
                dispose();

                if (form != null) {
                    form.closed();
                    form = null;
                }
            }
        });
    }

    public final void hideDialog() {
        setVisible(false);
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

        if (initFilterKeyEvent != null && initialFilterPropertyDrawID > 0 &&
            KeyStrokes.isSuitableStartFilteringEvent(initFilterKeyEvent)) {
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
