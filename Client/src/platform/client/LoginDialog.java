package platform.client;

import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.RemoteLogicsInterface;

import javax.swing.*;
import java.awt.event.*;
import java.rmi.RemoteException;

public class LoginDialog extends JDialog {

    RemoteLogicsInterface BL;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField loginField;
    private JPasswordField passwordField;

    public LoginDialog(RemoteLogicsInterface iBL) {

        BL = iBL;

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

    private RemoteNavigatorInterface result = null;

    private void onOK() {

        try {
            result = BL.createNavigator(loginField.getText(),new String(passwordField.getPassword()));
            dispose();
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {

        result = null;
        dispose();
    }

    public RemoteNavigatorInterface login() {

        pack();
        setLocationRelativeTo(null);

        setVisible(true);
        
        return result;
    }
}
