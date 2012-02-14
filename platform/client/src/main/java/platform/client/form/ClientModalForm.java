package platform.client.form;

import platform.client.ClientResourceBundle;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import static platform.client.SwingUtils.*;

public class ClientModalForm extends JDialog {

    protected ClientFormController form;
    protected final RemoteFormInterface remoteForm;
    private final boolean newSession;

    public ClientModalForm(Component owner, final RemoteFormInterface remoteForm, boolean newSession) throws IOException, ClassNotFoundException {
        super(getWindow(owner), ModalityType.DOCUMENT_MODAL); // обозначаем parent'а и модальность

        this.remoteForm = remoteForm;
        this.newSession = newSession;

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        setLayout(new BorderLayout());

        // делаем, чтобы не выглядел как диалог
        getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray, 1));

        form = createFormController();
        setTitle(form.getCaption());

        add(form.getComponent(), BorderLayout.CENTER);

        createUIHandlers();
    }

    private void createUIHandlers() {
        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                windowActivatedFirstTime();

                //чтобы только для 1й активации...
                ClientModalForm.this.removeWindowListener(this);
            }
        });

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

    protected void windowActivatedFirstTime() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(form.getComponent());
    }

    protected ClientFormController createFormController() throws IOException, ClassNotFoundException {
        return new ClientFormController(remoteForm, null, false, true, newSession) {
            @Override
            public void okPressed() {
                setCanClose(true);
                super.okPressed();
                if (isCanClose()) {
                    hideDialog();
                }
            }

            @Override
            void closePressed() {
                if (newSession && dataChanged
                        && JOptionPane.YES_OPTION != showConfirmDialog(getComponent(),
                                                                       ClientResourceBundle.getString("form.do.you.really.want.to.close.form"),
                                                                       null,
                                                                       JOptionPane.WARNING_MESSAGE)) {
                    return;
                }

                super.closePressed();

                hideDialog();
            }
        };
    }

    public final void hideDialog() {
        setVisible(false);
    }

    public void showDialog(boolean showFullScreen) {
        showDialog(showFullScreen, null);
    }

    public void showDialog(boolean showFullScreen, Point onScreen) {
        setSize(clipToScreen(showFullScreen
                             ? new Dimension(10000, 10000)
                             : calculatePreferredSize(isUndecorated())));

        if (onScreen != null) {
            requestLocation(this, onScreen);
        } else {
            setLocationRelativeTo(null);
        }

        setVisible(true);
    }

    public Dimension calculatePreferredSize(boolean undecorated) {
        Dimension preferredSize = form.calculatePreferredSize();

        // так как у нас есть только preferredSize самого contentPane, а нам нужен у JDialog
        // сколько будет занимать все "рюшечки" вокруг contentPane мы посчитать не можем, поскольку
        if (undecorated) {
            preferredSize.width += 10;
            preferredSize.height += 40;
        } else {
            preferredSize.width += 20;
            preferredSize.height += 80;
        }

        preferredSize.height += 35; // под отборы

        return preferredSize;
    }
}
