package platformlocal;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class LoginDialog extends JDialog {

    RemoteNavigator remoteNavigator;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField loginField;
    private JPasswordField passwordField;

    public LoginDialog(RemoteNavigator iremoteNavigator) {

        remoteNavigator = iremoteNavigator;

        setContentPane(contentPane);
        setAlwaysOnTop(true);
        setUndecorated(true);
        setModal(true);
        getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    }

    private boolean result = false;

    private void onOK() {

        // пока так сильно по поводу security заморачиваться не будем
        if (remoteNavigator.changeCurrentUser(loginField.getText(), new String(passwordField.getPassword()))) {
            result = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Неправильное имя пользователя или пароль.", null, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        
        dispose();
    }

    public boolean login() {

        pack();
        setLocationRelativeTo(null);

        setVisible(true);
        
        return result;
    }
}
