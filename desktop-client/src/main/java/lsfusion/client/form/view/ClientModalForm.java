package lsfusion.client.form.view;

import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.FormClientData;
import lsfusion.interop.form.remote.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EventObject;

import static lsfusion.client.base.SwingUtils.*;

public class ClientModalForm extends JDialog {

    private KeyEvent initFilterKeyEvent;

    private RemoteFormInterface remoteForm;

    private ClientFormController form;

    public boolean async;

    public ClientModalForm(Component owner, String title, boolean async) {
        super(getModalFormOwner(owner), title, ModalityType.DOCUMENT_MODAL);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        setLayout(new BorderLayout());

        this.async = async;
    }

    public void init(final RemoteFormInterface remoteForm, ClientForm clientForm, FormClientData clientData, boolean isDialog, EventObject initFilterEvent) {
        this.remoteForm = remoteForm;

        this.initFilterKeyEvent = initFilterEvent instanceof KeyEvent ? (KeyEvent) initFilterEvent : null;

        form = new ClientFormController(ClientModalForm.this.remoteForm, null, clientForm, clientData, null, true, isDialog) {
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
                             : calculatePreferredSize(isUndecorated())));

        if (onScreen != null) {
            requestLocation(this, onScreen);
        } else {
            setLocationRelativeTo(getOwner());
        }

        if(!async) {
            beforeShowDialog();
        }

        setVisible(true);
    }

    protected void beforeShowDialog() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                Component defaultComponent = form.getLayout().getFocusTraversalPolicy().getDefaultComponent(form.getLayout());
                SwingUtilities.invokeLater(() -> {
                    if (defaultComponent != null) {
                        defaultComponent.requestFocusInWindow();
                    } else if (form != null && !form.activateFirstComponents()) {
                        form.focusFirstComponent();
                    }
                });

                removeWindowListener(this);
            }
        });
    }

    public static int nativeScrollbarWidth = UIManager.getInt("ScrollBar.width");
    public static int nativeScrollbarHeight = UIManager.getInt("ScrollBar.height"); //there is no property "ScrollBar.height"

    public Dimension calculatePreferredSize(boolean undecorated) {
        //сначала нужно провалидейтать все компоненты, чтобы отработала логика autohide
//        form.getLayout().preValidateMainContainer();

        //todo: calculate size
        if(async) {
            return new Dimension(800, 600);
        } else {
            int horzClientOffset = 20;
            int vertClientOffset = 100;

            // there are 2 problems : rounding (we need to round up), however it coukd be fixed differently
            // since we are changing for example grid basises (by changing fill to percent), we can get extra scrollbars in grids (which is not what we want), so we just add some extraOffset
            int extraHorzOffset = nativeScrollbarWidth * 2;
            int extraVertOffset = nativeScrollbarHeight * 2;

            int wndWidth = MainFrame.instance.getWidth();
            int wndHeight = MainFrame.instance.getHeight();

            Dimension preferredSize = form.getLayout().getMaxPreferredSize(extraHorzOffset, extraVertOffset);

            // так как у нас есть только size самого contentPane, а нам нужен у JDialog
            // сколько будет занимать все "рюшечки" вокруг contentPane мы посчитать не можем, поскольку
            if (!undecorated) {
                preferredSize.width += 22;
                preferredSize.height += 40;
            }

            return new Dimension(Math.min(preferredSize.width, wndWidth - horzClientOffset), Math.min(preferredSize.height, wndHeight - vertClientOffset));
        }
    }

    public void directProcessKeyEvent(KeyEvent e) {
        if (form != null && !form.isEditing()) {
            form.getLayout().directProcessKeyEvent(e);
        }
    }
}
