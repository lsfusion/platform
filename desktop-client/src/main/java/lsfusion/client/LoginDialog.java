package lsfusion.client;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lsfusion.base.SystemUtils;
import lsfusion.interop.KeyStrokes;
import lsfusion.interop.RemoteServerAgentInterface;
import lsfusion.interop.ServerInfo;
import lsfusion.interop.remote.RMIUtils;
import org.apache.commons.codec.binary.Base64;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import static lsfusion.client.StartupProperties.LSFUSION_CLIENT_HOSTPORT;

public class LoginDialog extends JDialog {

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField loginField;
    private JPasswordField passwordField;
    private JComboBox serverHost;
    private JCheckBox savePassword;
    private JLabel warning;
    private JPanel warningPanel;
    private JComboBox serverDB;
    private String waitMessage = ClientResourceBundle.getString("dialog.please.wait");
    private LoginInfo loginInfo;
    private boolean autoLogin = false;
    private JLabel imageLabel;

    public LoginDialog(LoginInfo defaultLoginInfo) {
        super(null, "lsFusion", java.awt.Dialog.ModalityType.TOOLKIT_MODAL);
        imageLabel.setIcon(Main.getLogo());

        loginInfo = restoreLoginData(defaultLoginInfo);
        setContentPane(contentPane);
        setAlwaysOnTop(true);
        setModal(true);
        setIconImage(Main.getLogo().getImage());
        initServerHostList((MutableComboBoxModel) serverHost.getModel());

        initUIHandlers();

        if (loginInfo.getServerHost() != null) {
            StringBuilder server = new StringBuilder(loginInfo.getServerHost());
            if (loginInfo.getServerPort() != null) {
                server.append(":");
                server.append(loginInfo.getServerPort());
            }
            String item = server.toString();
            ((MutableComboBoxModel) serverHost.getModel()).addElement(item);
            serverHost.setSelectedItem(item);
        }

        String db = loginInfo.getServerDB();
        if (db != null) {
            if (serverDB.getItemCount() == 0)
                ((MutableComboBoxModel) serverDB.getModel()).addElement(db);
            serverDB.setSelectedItem(db);
        }

        if (loginInfo.getUserName() != null) {
            loginField.setText(loginInfo.getUserName());
        }

        if (loginInfo.getPassword() != null) {
            passwordField.setText(loginInfo.getPassword());
        }

        warningPanel.setVisible(false);

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
        }, KeyStrokes.getEscape(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void initUIHandlers() {
        getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);

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

        serverHost.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                update();
            }
        });

        serverHost.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                update();
            }
        });

        serverHost.getEditor().getEditorComponent().addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
                update();
            }

            public void keyPressed(KeyEvent e) {
                update();
            }

            public void keyReleased(KeyEvent e) {
                update();
            }
        });

        loginField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });

        loginField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                JTextField textField = (JTextField) e.getComponent();
                textField.selectAll();
            }
        });

        passwordField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                JTextField textField = (JTextField) e.getComponent();
                textField.selectAll();
            }
        });

        serverDB.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                serverDB.removeAllItems();
                propagateServerAgents();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) { }
        });
    }

    private void propagateServerAgents() {
        try {
            RemoteServerAgentInterface remoteLoader = RMIUtils.rmiLookup("localhost", 6666, "ServerAgent", Main.rmiSocketFactory);
            for (String exportName : remoteLoader.getExportNames()) {
                ((MutableComboBoxModel) serverDB.getModel()).addElement(exportName);
            }
        } catch (Exception ignore) {
        }
    }

    private boolean isValid(String server) {
        int pos = server.indexOf(':');
        if (pos != -1) {
            if (pos == 0) {
                return false;
            }
            try {
                Integer.parseInt(server.substring(pos + 1));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return !server.isEmpty();
    }

    private boolean isOkEnabled() {
        Object item = serverHost.getEditor().getItem();
        if (waitMessage.equals(item)) {
            return false;
        } else {
            if (loginField.getDocument().getLength() <= 0) {
                return false;
            } else if (!(item instanceof ServerInfo) && !isValid(item.toString())) {
                return false;
            }
        }

        return true;
    }

    private void update() {
        buttonOK.setEnabled(isOkEnabled());
    }

    private void initServerHostList(MutableComboBoxModel serverHostModel) {
        new ServerAgentsEnumerator(serverHostModel, waitMessage).execute();
        propagateServerAgents();
    }

    private LoginInfo result = null;

    private void onOK() {
        Object item = serverHost.getSelectedItem();
        ServerInfo serverInfo;
        if (item instanceof ServerInfo) {
            serverInfo = (ServerInfo) item;
        } else {
            serverInfo = getServerInfo(item.toString());
        }
        result = loginInfo = new LoginInfo(serverInfo.getHostName(), String.valueOf(serverInfo.getPort()), String.valueOf(serverDB.getSelectedItem()), loginField.getText(), new String(passwordField.getPassword()));

        storeServerData();
        setVisible(false);
    }

    private static final String configName = "login.dialog.cfg";

    private void storeServerData() {
        try {
            FileWriter fileWr = new FileWriter(SystemUtils.getUserFile(configName));
            fileWr.write(result.getServerHost() + '\n');
            fileWr.write(result.getServerPort() + '\n');
            fileWr.write(result.getUserName() + '\n');
            fileWr.write(result.getServerDB() + '\n');
            fileWr.write(String.valueOf(savePassword.isSelected()) + '\n');
            if (savePassword.isSelected()) {
                fileWr.write(Base64.encodeBase64String(result.getPassword().getBytes()) + '\n');
            }
            fileWr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LoginInfo restoreLoginData(LoginInfo loginInfo) {
        try {
            File file = SystemUtils.getUserFile(configName, false);
            if (file.exists()) {
                FileReader fileRd = new FileReader(file);
                Scanner scanner = new Scanner(fileRd);
                String serverHost = scanner.hasNextLine() ? scanner.nextLine() : "";
                if (loginInfo.getServerHost() != null) {
                    serverHost = loginInfo.getServerHost();
                }
                String serverPort = scanner.hasNextLine() ? scanner.nextLine() : "";
                if (loginInfo.getServerPort() != null) {
                    serverPort = loginInfo.getServerPort();
                }
                String userName = scanner.hasNextLine() ? scanner.nextLine() : "";
                if (loginInfo.getUserName() != null) {
                    userName = loginInfo.getUserName();
                }
                String serverDB = scanner.hasNextLine() ? scanner.nextLine() : "";
                if (loginInfo.getServerDB() != null) {
                    serverDB = loginInfo.getServerDB();
                }
                if (serverDB.isEmpty()) {
                    serverDB = "default";
                }
                String savePwd = scanner.hasNextLine() ? scanner.nextLine() : "";
                savePassword.setSelected(Boolean.valueOf(savePwd));
                String password = "";
                if (scanner.hasNextLine()) {
                    password = scanner.nextLine();
                    byte[] pass = Base64.decodeBase64(password);
                    password = new String(pass);
                }
                if (loginInfo.getPassword() != null) {
                    password = loginInfo.getPassword();
                }

                return new LoginInfo(serverHost, serverPort, serverDB, userName, password);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return loginInfo;
        }

        return loginInfo;
    }

    private ServerInfo getServerInfo(String server) {
        int pos = server.indexOf(':');
        if (pos == -1) {
            return new ServerInfo(server, server, Integer.parseInt(System.getProperty(LSFUSION_CLIENT_HOSTPORT, "7652")));
        }

        return new ServerInfo(server, server.substring(0, pos), Integer.parseInt(server.substring(pos + 1)));
    }

    private void onCancel() {
        result = null;
        dispose();
        Main.shutdown();
    }

    public void setWarningMsg(String msg) {
        warning.setText(msg);
        warningPanel.setVisible(msg != null && !msg.isEmpty());
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public LoginInfo login() {
        boolean needData = loginInfo.getServerHost() == null || loginInfo.getServerPort() == null ||
                loginInfo.getUserName() == null || loginInfo.getPassword() == null;
        if (!autoLogin || needData) {
            pack();
            setLocationRelativeTo(null);

            loginField.requestFocusInWindow();

            getRootPane().setDefaultButton(buttonOK);

            setVisible(true);

            return result;
        }

        return loginInfo;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), null));
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(0);
        imageLabel.setHorizontalTextPosition(0);
        imageLabel.setRequestFocusEnabled(true);
        imageLabel.setText("");
        contentPane.add(imageLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 1, new Insets(4, 4, 4, 4), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(24, 48), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText(ClientResourceBundle.getString("dialog.login"));
        panel2.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText(ClientResourceBundle.getString("dialog.password"));
        panel2.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loginField = new JTextField();
        panel2.add(loginField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        passwordField = new JPasswordField();
        panel2.add(passwordField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText(ClientResourceBundle.getString("dialog.server"));
        panel2.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverHost = new JComboBox();
        serverHost.setEditable(true);
        panel2.add(serverHost, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText(ClientResourceBundle.getString("dialog.database"));
        panel2.add(label5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverDB = new JComboBox();
        serverDB.setEditable(true);
        panel2.add(serverDB, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setPreferredSize(new Dimension(300, 100));
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel3.add(panel4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        buttonOK.setMnemonic('O');
        buttonOK.setDisplayedMnemonicIndex(0);
        panel4.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        buttonCancel.setMnemonic('C');
        buttonCancel.setDisplayedMnemonicIndex(0);
        panel4.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        savePassword = new JCheckBox();
        savePassword.setText(ClientResourceBundle.getString("dialog.remember.me"));
        panel1.add(savePassword, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        warningPanel = new JPanel();
        warningPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        warningPanel.setBackground(new Color(-39322));
        panel1.add(warningPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 30), new Dimension(-1, 30), null, 0, false));
        warning = new JLabel();
        warning.setBackground(new Color(-986896));
        warning.setText("");
        warningPanel.add(warning, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        label2.setLabelFor(loginField);
        label3.setLabelFor(passwordField);
        label5.setLabelFor(serverDB);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
