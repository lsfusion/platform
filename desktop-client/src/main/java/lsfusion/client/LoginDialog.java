package lsfusion.client;

import lsfusion.base.BaseUtils;
import lsfusion.interop.KeyStrokes;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.RemoteLogicsLoaderInterface;
import lsfusion.interop.ServerInfo;
import org.apache.log4j.Logger;
import org.castor.core.util.Base64Decoder;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import static lsfusion.base.BaseUtils.trimToNull;
import static lsfusion.client.StartupProperties.LSFUSION_CLIENT_HOSTPORT;

public class LoginDialog extends JDialog {
    private final static Logger logger = Logger.getLogger(LoginDialog.class);

    private JPanel contentPane;
    private JButton buttonOK;
    private String checkVersionError;
    private JButton buttonCancel;
    private JComboBox loginBox;
    private JPasswordField passwordField;
    private JComboBox serverHostComboBox;
    private JCheckBox savePasswordCheckBox;
    private JCheckBox useAnonymousUICheckBox;
    private JLabel warningLabel;
    private JPanel warningPanel;
    private JComboBox serverDBComboBox;
    private JLabel loginLabel;
    private JLabel passwordLabel;
    private LoginInfo loginInfo;
    private boolean autoLogin = false;
    private JLabel imageLabel;
    private List<UserInfo> userInfos;

    public LoginDialog(LoginInfo defaultLoginInfo, List<UserInfo> userInfos) {
        super(null, null, java.awt.Dialog.ModalityType.TOOLKIT_MODAL);
        this.userInfos = userInfos;
        setupUI();
        loadServerSettings(defaultLoginInfo);

        loginInfo = defaultLoginInfo;
        setAlwaysOnTop(true);
        setModal(true);
        setResizable(false);

        initUIHandlers();

        savePasswordCheckBox.setSelected(loginInfo.getSavePwd());

        if (loginInfo.getServerHost() != null) {
            StringBuilder server = new StringBuilder(loginInfo.getServerHost());
            if (loginInfo.getServerPort() != null) {
                server.append(":");
                server.append(loginInfo.getServerPort());
            }
            String item = server.toString();
            ((MutableComboBoxModel<String>) serverHostComboBox.getModel()).addElement(item);
            serverHostComboBox.setSelectedItem(item);
        }

        String db = loginInfo.getServerDB();
        if (db != null) {
            if (serverDBComboBox.getItemCount() == 0)
                ((MutableComboBoxModel<String>) serverDBComboBox.getModel()).addElement(db);
            serverDBComboBox.setSelectedItem(db);
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

        serverHostComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                update();
            }
        });

        serverHostComboBox.addActionListener(new ActionListener() {
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
        serverHostComboBox.getEditor().getEditorComponent().addKeyListener(updateKeyListener);

        loginBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    UserInfo info = getUserInfo((String) loginBox.getModel().getSelectedItem());
                    if (info != null) {
                        savePasswordCheckBox.setSelected(info.savePassword);
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

        useAnonymousUICheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateAnonymousUIActivity();
            }
        });
    }

    private void initServerSettingsListeners() {
        serverHostComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.DESELECTED) {
                    loadServerSettings();
                }
            }
        });

        serverDBComboBox.addItemListener(new ItemListener() {
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
        ServerInfo serverInfo = getServerInfo(String.valueOf(serverHostComboBox.getSelectedItem()));
        loadServerSettings(serverInfo.getHostName(), String.valueOf(serverInfo.getPort()), (String) serverDBComboBox.getSelectedItem());
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

                error = BaseUtils.checkClientVersion(serverPlatformVersion, serverApiVersion, BaseUtils.getPlatformVersion(),  BaseUtils.getApiVersion());

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

            useAnonymousUICheckBox.setVisible(hasAnonymousUI);
            updateAnonymousUIActivity();

            setWarningMsg(error);
            checkVersionError = error;
            pack();
        }
    }

    private void updateAnonymousUIActivity() {
        boolean enable = !useAnonymousUI();
        loginLabel.setEnabled(enable);
        loginBox.setEnabled(enable);
        passwordLabel.setEnabled(enable);
        passwordField.setEnabled(enable);
        savePasswordCheckBox.setEnabled(enable);
    }

    private UserInfo getUserInfo(String userName) {
        for (UserInfo userInfo : userInfos) {
            if (userInfo.name.equals(userName)) {
                return userInfo;
            }
        }
        return null;
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
        Object item = serverHostComboBox.getEditor().getItem();
        return checkVersionError == null && !((String) loginBox.getEditor().getItem()).isEmpty()
                && (item instanceof ServerInfo || isValid(item.toString()));
    }

    private void update() {
        buttonOK.setEnabled(isOkEnabled());
    }

    private void onOK() {
        Object item = serverHostComboBox.getSelectedItem();
        ServerInfo serverInfo;
        if (item instanceof ServerInfo) {
            serverInfo = (ServerInfo) item;
        } else {
            serverInfo = getServerInfo(item.toString());
        }
        loginInfo = new LoginInfo(
                serverInfo.getHostName(),
                String.valueOf(serverInfo.getPort()),
                (String) serverDBComboBox.getSelectedItem(),
                new UserInfo((String) loginBox.getSelectedItem(), savePasswordCheckBox.isSelected(), new String(passwordField.getPassword())),
                useAnonymousUI()
        );

        setVisible(false);
    }

    private boolean useAnonymousUI() {
        return useAnonymousUICheckBox.isVisible() && useAnonymousUICheckBox.isSelected();
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
        if(checkVersionError == null) {
            warningLabel.setText(msg);
            warningPanel.setVisible(msg != null && !msg.isEmpty());
            pack();
        }
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public LoginInfo login() {
        boolean needData = loginInfo.getServerHost() == null || loginInfo.getServerPort() == null ||
                loginInfo.getUserName() == null || loginInfo.getPassword() == null;
        if (!autoLogin || needData) {
            setLocationRelativeTo(null);
            loginBox.requestFocusInWindow();
            getRootPane().setDefaultButton(buttonOK);
            setVisible(true);
        }
        return loginInfo;
    }

    private void setupUI() {

        contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), null));
        setContentPane(contentPane);

        JPanel imagePanel = new JPanel();
        imagePanel.setBorder(new LineBorder(new Color(160, 160, 160)));
        imagePanel.setBackground(Color.WHITE);
        contentPane.add(imagePanel);

        imageLabel = new JLabel("", SwingConstants.CENTER);
        imageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        imageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        imageLabel.setRequestFocusEnabled(true);
        imagePanel.add(imageLabel);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        contentPane.add(mainPanel);

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new GridBagLayout());
        mainPanel.add(settingsPanel);

        JLabel serverLabel = new JLabel(ClientResourceBundle.getString("dialog.server"));
        settingsPanel.add(serverLabel, getGridBagConstraints(0, 0, true));

        serverHostComboBox = new JComboBox();
        serverHostComboBox.setEditable(true);
        settingsPanel.add(serverHostComboBox, getGridBagConstraints(0, 1, true));

        JLabel serverDBLabel = new JLabel(ClientResourceBundle.getString("dialog.database"));
        serverDBLabel.setLabelFor(serverDBComboBox);
        settingsPanel.add(serverDBLabel, getGridBagConstraints(1, 0, true));

        serverDBComboBox = new JComboBox();
        serverDBComboBox.setEditable(true);
        settingsPanel.add(serverDBComboBox, getGridBagConstraints(1, 1, true));

        useAnonymousUICheckBox = new JCheckBox(ClientResourceBundle.getString("dialog.use.anonymous.ui"), true);
        settingsPanel.add(useAnonymousUICheckBox, getGridBagConstraints(2, 0, false));

        loginLabel = new JLabel(ClientResourceBundle.getString("dialog.login"));
        loginLabel.setLabelFor(loginBox);
        settingsPanel.add(loginLabel, getGridBagConstraints(3, 0, true));

        loginBox = new JComboBox();
        loginBox.setEditable(true);
        settingsPanel.add(loginBox, getGridBagConstraints(3, 1, true));

        passwordLabel = new JLabel(ClientResourceBundle.getString("dialog.password"));
        passwordLabel.setLabelFor(passwordField);
        settingsPanel.add(passwordLabel, getGridBagConstraints(4, 0, true));

        passwordField = new JPasswordField();
        settingsPanel.add(passwordField, getGridBagConstraints(4, 1, true));

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        mainPanel.add(bottomPanel);

        savePasswordCheckBox = new JCheckBox(ClientResourceBundle.getString("dialog.remember.me"));
        bottomPanel.add(savePasswordCheckBox, getGridBagConstraints(0, 0, false));

        JPanel okCancelPanel = new JPanel(new BorderLayout());
        bottomPanel.add(okCancelPanel, getGridBagConstraints(1, 1, false));

        JPanel subOKCancelPanel = new JPanel(new GridLayout());
        okCancelPanel.add(subOKCancelPanel, BorderLayout.EAST);

        buttonOK = new JButton(ClientResourceBundle.getString("dialog.ok"));
        buttonOK.setMnemonic('O');
        buttonOK.setDisplayedMnemonicIndex(0);
        subOKCancelPanel.add(buttonOK);

        buttonCancel = new JButton(ClientResourceBundle.getString("dialog.cancel"));
        buttonCancel.setMnemonic('C');
        buttonCancel.setDisplayedMnemonicIndex(0);
        subOKCancelPanel.add(buttonCancel);

        warningPanel = new JPanel();
        warningPanel.setBackground(new Color(-39322));
        warningLabel = new JLabel("");
        warningPanel.add(warningLabel);
        mainPanel.add(warningPanel);
    }

    private GridBagConstraints getGridBagConstraints(int row, int column, boolean insets) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = column;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.anchor = column == 0 ? GridBagConstraints.WEST : GridBagConstraints.EAST;
        gbc.fill = column == 0 ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;

        if(insets) {
            gbc.insets = new Insets(2, 0, 2, 0);
        }
        gbc.weightx = column == 0 ? 0.1 : 1.0;
        gbc.weighty = 1.0;
        return gbc;
    }
}
