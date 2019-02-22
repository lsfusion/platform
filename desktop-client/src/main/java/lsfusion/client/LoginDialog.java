package lsfusion.client;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lsfusion.base.BaseUtils;
import lsfusion.interop.*;
import lsfusion.interop.remote.RMIUtils;
import org.apache.log4j.Logger;
import org.castor.core.util.Base64Decoder;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import static lsfusion.base.BaseUtils.trimToNull;
import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.StartupProperties.LSFUSION_CLIENT_HOSTPORT;

public class LoginDialog extends JDialog {
    private final static Logger logger = Logger.getLogger(LoginDialog.class);

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox<String> loginBox;
    private JPasswordField passwordField;
    private JComboBox<String> serverHost;
    private JCheckBox savePassword;
    private JCheckBox useAnonymousUI;
    private boolean hasAnonymousUI = false;
    private JLabel warning;
    private JPanel warningPanel;
    private JComboBox<String> serverDB;
    private JLabel loginLabel;
    private JLabel passwordLabel;
    private String waitMessage = ClientResourceBundle.getString("dialog.please.wait");
    private LoginInfo loginInfo;
    private boolean autoLogin = false;
    private JLabel imageLabel;
    private List<UserInfo> userInfos;

    public LoginDialog(LoginInfo defaultLoginInfo, List<UserInfo> userInfos) {
        super(null, null, java.awt.Dialog.ModalityType.TOOLKIT_MODAL);
        this.userInfos = userInfos;
        loadServerSettings(defaultLoginInfo);

        loginInfo = defaultLoginInfo;
        setContentPane(contentPane);
        setAlwaysOnTop(true);
        setModal(true);
        initServerHostList((MutableComboBoxModel) serverHost.getModel());
        setResizable(false);

        initUIHandlers();

        savePassword.setSelected(loginInfo.getSavePwd());

        if (loginInfo.getServerHost() != null) {
            StringBuilder server = new StringBuilder(loginInfo.getServerHost());
            if (loginInfo.getServerPort() != null) {
                server.append(":");
                server.append(loginInfo.getServerPort());
            }
            String item = server.toString();
            ((MutableComboBoxModel<String>) serverHost.getModel()).addElement(item);
            serverHost.setSelectedItem(item);
        }

        String db = loginInfo.getServerDB();
        if (db != null) {
            if (serverDB.getItemCount() == 0)
                ((MutableComboBoxModel<String>) serverDB.getModel()).addElement(db);
            serverDB.setSelectedItem(db);
        }

        for (UserInfo userInfo : userInfos) {
            ((MutableComboBoxModel<String>)loginBox.getModel()).addElement(userInfo.name);
        }
        if (loginInfo.getUserName() != null) {
            loginBox.setSelectedItem(loginInfo.getUserName());
        }

        if (loginInfo.getSavePwd() && loginInfo.getPassword() != null) { // чтобы при повторном показе диалога ("Выход") сбрасывался несохранённый пароль 
            passwordField.setText(loginInfo.getPassword());
        }

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

        initServerSettingsListeners();
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

        KeyListener updateKeyListener = new KeyListener() {
            public void keyTyped(KeyEvent e) {
                update();
            }

            public void keyPressed(KeyEvent e) {
                update();
            }

            public void keyReleased(KeyEvent e) {
                update();
            }
        };
        serverHost.getEditor().getEditorComponent().addKeyListener(updateKeyListener);

        loginBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    UserInfo info = getUserInfo((String) loginBox.getModel().getSelectedItem());
                    if (info != null) {
                        savePassword.setSelected(info.savePassword);
                        passwordField.setText(info.savePassword ? info.password : "");
                    }
                    update();
                }
            }
        });

        loginBox.getEditor().getEditorComponent().addKeyListener(updateKeyListener);

        loginBox.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                JComboBox textBox = (JComboBox) e.getComponent();
                textBox.getEditor().selectAll();
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
//                serverDB.removeAllItems();
//                propagateServerAgents();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) { }
        });

        useAnonymousUI.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateAnonymousUIVisibility();
            }
        });
    }

    private void initServerSettingsListeners() {
        serverHost.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.DESELECTED) {
                    loadServerSettings();
                }
            }
        });

        serverDB.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.DESELECTED) {
                    loadServerSettings();
                }
            }
        });
    }

    public void loadServerSettings(LoginInfo defaultLoginInfo) {
        loadServerSettings(defaultLoginInfo.getServerHost(), defaultLoginInfo.getServerPort(), defaultLoginInfo.getServerDB());
    }

    public void loadServerSettings() {
        ServerInfo serverInfo = getServerInfo(String.valueOf(serverHost.getSelectedItem()));
        loadServerSettings(serverInfo.getHostName(), String.valueOf(serverInfo.getPort()), (String) serverDB.getSelectedItem());
    }

    public void loadServerSettings(String host, String port, String dataBase) {
        if (host != null && port != null && dataBase != null) {
            String logicsName = null;
            String logicsDisplayName = null;
            String iconBase64 = null;
            String logoBase64 = null;
            boolean hasAnonymousUI = false;
            String error = null;
            try {
                RemoteLogicsLoaderInterface remoteLoader = new ReconnectWorker(host, port, dataBase).connect(false);
                RemoteLogicsInterface remoteLogics = remoteLoader.getLogics();

                JSONObject serverSettings = Main.getServerSettings(remoteLogics);
                logicsName = trimToNull(serverSettings.optString("logicsName"));
                logicsDisplayName = trimToNull(serverSettings.optString("displayName"));
                iconBase64 = trimToNull(serverSettings.optString("logicsIcon"));
                logoBase64 = trimToNull(serverSettings.optString("logicsLogo"));
                hasAnonymousUI = serverSettings.optBoolean("anonymousUI");
                String serverPlatformVersion = trimToNull(serverSettings.optString("platformVersion"));
                Integer serverApiVersion = serverSettings.optInt("apiVersion");
                error = checkApiVersion(serverPlatformVersion, serverApiVersion);

            } catch (Throwable e) {
                logger.error("Failed to load server settings", e);
            }

            Main.logicsName = logicsName;

            Main.logicsDisplayName = logicsDisplayName;
            setTitle(Main.getMainTitle());

            Main.logicsMainIcon = iconBase64 != null ? Base64Decoder.decode(iconBase64) : null;
            setIconImages(Main.getMainIcons());

            Main.logicsLogo = logoBase64 != null ? Base64Decoder.decode(logoBase64) : null;
            imageLabel.setIcon(Main.getLogo());

            this.hasAnonymousUI = hasAnonymousUI;
            updateAnonymousUIVisibility();

            setWarningMsg(error);
            pack();
        }
    }

    private String checkApiVersion(String serverPlatformVersion, Integer serverApiVersion) {
        String serverVersion = null;
        String clientVersion = null;
        String clientPlatformVersion = BaseUtils.getPlatformVersion();
        if(clientPlatformVersion != null && !clientPlatformVersion.equals(serverPlatformVersion)) {
            serverVersion = serverPlatformVersion;
            clientVersion = clientPlatformVersion;
        } else {
            Integer clientApiVersion = BaseUtils.getApiVersion();
            if(!clientApiVersion.equals(serverApiVersion)) {
                serverVersion = serverPlatformVersion + " [" + serverApiVersion + "]";
                clientVersion = clientPlatformVersion + " [" + clientApiVersion + "]";
            }
        }
        return serverVersion != null ? getString("client.error.need.restart", serverVersion, clientVersion) : null;
    }

    private void updateAnonymousUIVisibility() {
        useAnonymousUI.setVisible(hasAnonymousUI);
        boolean show = !useAnonymousUI();
        loginLabel.setVisible(show);
        loginBox.setVisible(show);
        passwordLabel.setVisible(show);
        passwordField.setVisible(show);
        savePassword.setVisible(show);
        pack();
    }
    
    private UserInfo getUserInfo(String userName) {
        for (UserInfo userInfo : userInfos) {
            if (userInfo.name.equals(userName)) {
                return userInfo;
            }
        }
        return null;
    }

    private void propagateServerAgents() {
        try {
            RemoteServerAgentInterface remoteLoader = RMIUtils.rmiLookup("localhost", 6666, "ServerAgent", Main.rmiSocketFactory);
            for (String exportName : remoteLoader.getExportNames()) {
                ((MutableComboBoxModel<String>) serverDB.getModel()).addElement(exportName);
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
        return !waitMessage.equals(item) 
                && !((String) loginBox.getEditor().getItem()).isEmpty() 
                && (item instanceof ServerInfo || isValid(item.toString()));
    }

    private void update() {
        buttonOK.setEnabled(isOkEnabled());
    }

    private void initServerHostList(MutableComboBoxModel serverHostModel) {
        new ServerAgentsEnumerator(serverHostModel, waitMessage).execute();
//        propagateServerAgents();
    }

    private void onOK() {
        Object item = serverHost.getSelectedItem();
        ServerInfo serverInfo;
        if (item instanceof ServerInfo) {
            serverInfo = (ServerInfo) item;
        } else {
            serverInfo = getServerInfo(item.toString());
        }
        loginInfo = new LoginInfo(
                serverInfo.getHostName(), 
                String.valueOf(serverInfo.getPort()),
                (String) serverDB.getSelectedItem(), 
                new UserInfo((String) loginBox.getSelectedItem(), savePassword.isSelected(), new String(passwordField.getPassword())),
                useAnonymousUI()
        );

        setVisible(false);
    }

    private boolean useAnonymousUI() {
        return useAnonymousUI.isVisible() && useAnonymousUI.isSelected();
    }

    private ServerInfo getServerInfo(String server) {
        int pos = server.indexOf(':');
        if (pos == -1) {
            return new ServerInfo(server, server, Integer.parseInt(System.getProperty(LSFUSION_CLIENT_HOSTPORT, "7652")));
        }

        return new ServerInfo(server, server.substring(0, pos), Integer.parseInt(server.substring(pos + 1)));
    }

    private void onCancel() {
        loginInfo = null;
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

            loginBox.requestFocusInWindow();

            getRootPane().setDefaultButton(buttonOK);

            setVisible(true);

            return loginInfo;
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
        contentPane = new JPanel(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), null));
        imageLabel = new JLabel("", 0);
        imageLabel.setHorizontalTextPosition(0);
        imageLabel.setRequestFocusEnabled(true);
        JPanel imagePanel = new JPanel();
        imagePanel.add(imageLabel);
        imagePanel.setBorder(new LineBorder(new Color(160, 160, 160)));
        imagePanel.setBackground(Color.WHITE);
        imageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(imagePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel(new GridLayoutManager(5, 1, new Insets(4, 4, 4, 4), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(24, 48), null, 0, false));
        final JPanel panel2 = new JPanel(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        loginLabel = new JLabel(ClientResourceBundle.getString("dialog.login"));
        panel2.add(loginLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        passwordLabel = new JLabel(ClientResourceBundle.getString("dialog.password"));
        panel2.add(passwordLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loginBox = new JComboBox();
        loginBox.setEditable(true);
        panel2.add(loginBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        passwordField = new JPasswordField();
        panel2.add(passwordField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel serverLabel = new JLabel(ClientResourceBundle.getString("dialog.server"));
        panel2.add(serverLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverHost = new JComboBox();
        serverHost.setEditable(true);
        panel2.add(serverHost, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel dataBaseLabel = new JLabel(ClientResourceBundle.getString("dialog.database"));
        panel2.add(dataBaseLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverDB = new JComboBox();
        serverDB.setEditable(true);
        panel2.add(serverDB, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel spacerOkCancelPanel = new JPanel();
        spacerOkCancelPanel.setPreferredSize(new Dimension(300, 100));
        spacerOkCancelPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(spacerOkCancelPanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        spacerOkCancelPanel.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel okCancelPanel = new JPanel();
        okCancelPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        spacerOkCancelPanel.add(okCancelPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton(ClientResourceBundle.getString("dialog.ok"));
        buttonOK.setMnemonic('O');
        buttonOK.setDisplayedMnemonicIndex(0);
        okCancelPanel.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton(ClientResourceBundle.getString("dialog.cancel"));
        buttonCancel.setMnemonic('C');
        buttonCancel.setDisplayedMnemonicIndex(0);
        okCancelPanel.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        savePassword = new JCheckBox(ClientResourceBundle.getString("dialog.remember.me"));
        panel1.add(savePassword, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useAnonymousUI = new JCheckBox(ClientResourceBundle.getString("dialog.use.anonymous.ui"), true);
        panel1.add(useAnonymousUI, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        warningPanel = new JPanel();
        warningPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        warningPanel.setBackground(new Color(-39322));
        panel1.add(warningPanel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 30), new Dimension(-1, 30), null, 0, false));
        warning = new JLabel("");
        warning.setBackground(new Color(-986896));
        warningPanel.add(warning, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        loginLabel.setLabelFor(loginBox);
        passwordLabel.setLabelFor(passwordField);
        dataBaseLabel.setLabelFor(serverDB);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
