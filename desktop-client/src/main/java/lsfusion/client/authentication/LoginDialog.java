package lsfusion.client.authentication;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.SystemUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.log.ClientLoggers;
import lsfusion.client.controller.MainController;
import lsfusion.client.logics.LogicsProvider;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.form.event.KeyStrokes;
import lsfusion.interop.logics.LogicsConnection;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LoginDialog extends JDialog {

    private static final String CONFIG_FILE_NAME = "login.dialog.cfg";

    private JPanel contentPane;
    private JButton buttonOK;
    private String checkVersionError;
    private JButton buttonCancel;
    private JComboBox<String> loginBox;
    private JPasswordField passwordField;
    private JComboBox<String> serverHostComboBox;
    private JCheckBox savePasswordCheckBox;
    private JCheckBox useAnonymousUICheckBox;
    private JLabel warningLabel;
    private JPanel warningPanel;
    private JPanel warningPanelWrapper;
    private JComboBox<String> serverDBComboBox;
    private JLabel loginLabel;
    private JLabel passwordLabel;
    private JLabel imageLabel;

    private List<UserInfo> userInfos; // needed for passwords
    
    public String getCurrentItem(JComboBox<String> comboBox) {
        return (String)comboBox.getEditor().getItem(); // not getSelectedItem, because we want to see edited (not selected) text
    }

    // result
    private LogicsConnection serverInfo;
    private UserInfo userInfo;

    private LoginDialog(String warningMsg, LogicsConnection serverInfo, UserInfo currentUserInfo, List<UserInfo> userInfos) {
        super(null, null, java.awt.Dialog.ModalityType.TOOLKIT_MODAL);

        setupUI();
        setAlwaysOnTop(true);
        setModal(true);
        setResizable(false);

        initServerSettings(serverInfo);

        initUserSettings(currentUserInfo, userInfos);

        updateOK();

        initUIHandlers();

        setLocationRelativeTo(null);
        loginBox.requestFocusInWindow();
        getRootPane().setDefaultButton(buttonOK);

        setWarningMsg(checkVersionError != null ? checkVersionError : warningMsg);
    }

    private void initServerSettings(LogicsConnection serverInfo) {
        StringBuilder server = new StringBuilder(serverInfo.host);
        if (serverInfo.port != 0) {
            server.append(":");
            server.append(serverInfo.port);
        }
        String item = server.toString();
        ((MutableComboBoxModel<String>) serverHostComboBox.getModel()).addElement(item);
        serverHostComboBox.setSelectedItem(item);

        String db = serverInfo.exportName;
        if (serverDBComboBox.getItemCount() == 0)
            ((MutableComboBoxModel<String>) serverDBComboBox.getModel()).addElement(db);
        serverDBComboBox.setSelectedItem(db);

        updateServerSettings(false);
        initServerSettingsListeners();
    }

    private void initUserSettings(UserInfo currentUserInfo, List<UserInfo> userInfos) {
        boolean anonymous = currentUserInfo.isAnonymous();
        useAnonymousUICheckBox.setSelected(anonymous);
        updateAnonymousUIActivity();

        this.userInfos = userInfos; // need to store them for passwords
        for (UserInfo userInfo : userInfos) {
            ((MutableComboBoxModel<String>)loginBox.getModel()).addElement(userInfo.name);
        }
        UserInfo selectInfo;
        if(anonymous)
            selectInfo = userInfos.isEmpty() ? null : userInfos.get(0);
        else
            selectInfo = currentUserInfo; 
        if(selectInfo != null) {
            loginBox.setSelectedItem(selectInfo.name);
            updatePassword(selectInfo);
        }
    }

    private void initUIHandlers() {
        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        serverHostComboBox.addItemListener(e -> updateOK());

        serverHostComboBox.addActionListener(e -> updateOK());

        KeyListener updateKeyListener = new KeyListener() {
            public void keyTyped(KeyEvent e) {
                updateOK();
            }

            public void keyPressed(KeyEvent e) {
                updateOK();
            }

            public void keyReleased(KeyEvent e) {
                updateOK();
            }
        };
        serverHostComboBox.getEditor().getEditorComponent().addKeyListener(updateKeyListener);

        loginBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                UserInfo info = getUserInfo(getCurrentItem(loginBox));
                if (info != null) {
                    updatePassword(info);
                }
                updateOK();
            }
        });

        loginBox.getEditor().getEditorComponent().addKeyListener(updateKeyListener);

        loginBox.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                JComboBox<String> textBox = (JComboBox<String>) e.getComponent();
                textBox.getEditor().selectAll();
            }
        });

        passwordField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                JTextField textField = (JTextField) e.getComponent();
                textField.selectAll();
            }
        });

        useAnonymousUICheckBox.addItemListener(e -> updateAnonymousUIActivity());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(e -> onCancel(), KeyStrokes.getEscape(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public void updatePassword(UserInfo info) {
        savePasswordCheckBox.setSelected(info.savePassword);
        passwordField.setText(info.savePassword ? info.password : "");
    }

    private void initServerSettingsListeners() {
        serverHostComboBox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.DESELECTED) {
                updateServerSettings(true);
            }
        });

        serverDBComboBox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.DESELECTED) {
                updateServerSettings(true);
            }
        });
    }

    public void updateServerSettings(boolean setWarningMessage) {
        LogicsConnection serverInfo = getServerInfo();

        Result<String> checkVersionError = new Result<>();
        ServerSettings serverSettings = isValid(serverInfo) ? getAndCheckServerSettings(serverInfo, checkVersionError, false) : null;

        setTitle(MainController.getMainTitle(serverSettings));

        setIconImages(MainController.getMainIcons(serverSettings));

        imageLabel.setIcon(MainController.getLogo(serverSettings));

        boolean hasAnonymousUI = serverSettings != null && serverSettings.anonymousUI;

        useAnonymousUICheckBox.setVisible(hasAnonymousUI);
        updateAnonymousUIActivity();

        if(setWarningMessage) {
            setWarningMsg(checkVersionError.result);
        }
        this.checkVersionError = checkVersionError.result;
        pack();
    }

    private ServerSettings getAndCheckServerSettings(LogicsConnection serverInfo, Result<String> rCheck, boolean noCache) {
        ServerSettings serverSettings = LogicsProvider.instance.getServerSettings(serverInfo, MainController.getSessionInfo(), null, noCache);
        String checkVersionError = serverSettings != null ? BaseUtils.checkClientVersion(serverSettings.platformVersion, serverSettings.apiVersion, BaseUtils.getPlatformVersion(),  BaseUtils.getApiVersion()) : null;
        if(checkVersionError != null) {
            if(!noCache) // try without cache
                return getAndCheckServerSettings(serverInfo, rCheck, true);
            rCheck.set(checkVersionError);
        }
        return serverSettings;
    }

    private static void syncUsers(RemoteLogicsInterface remoteLogics, Result<List<UserInfo>> userInfos) {
        if (remoteLogics != null) {
            JSONArray users = new JSONArray();
            for (UserInfo userInfo : userInfos.result) {
                users.put(userInfo.name);
            }
            FileData fileData = new FileData(new RawFileData(users.toString().getBytes(StandardCharsets.UTF_8)), "json");
            try {
                ExternalResponse result = remoteLogics.exec(AuthenticationToken.ANONYMOUS, MainController.getSessionInfo(), "Authentication.syncUsers[ISTRING[100], JSONFILE]", new ExternalRequest(new Object[]{MainController.computerName, fileData}));
                JSONArray unlockedUsers = new JSONArray(new String(((FileData) result.results[0]).getRawFile().getBytes(), StandardCharsets.UTF_8));
                List<Object> currentUsers = unlockedUsers.toList();
                List<UserInfo> newUserInfos = new ArrayList<>();
                for (UserInfo userInfo : userInfos.result) {
                    if (currentUsers.remove(userInfo.name)) {
                        newUserInfos.add(userInfo);
                    }
                }
                for (Object user : currentUsers) {
                    newUserInfos.add(new UserInfo(user.toString(), false, null));
                }
                userInfos.set(newUserInfos);
            } catch (RemoteException e) {
                ClientLoggers.clientLogger.error("Error synchronizing users", e);
            }
        }
    }
    
    private static void storeServerAndUserInfos(LogicsConnection serverInfo, UserInfo currentUserInfo, List<UserInfo> userInfos) {
        try {
            FileWriter fileWr = new FileWriter(SystemUtils.getUserFile(CONFIG_FILE_NAME));
            fileWr.write(serverInfo.host + "\n");
            fileWr.write(serverInfo.port + "\n");

            // reordering users (to make current first) and saving + important that here userInfos
            StringBuilder usersString = new StringBuilder();
            boolean currentAnonymous = currentUserInfo.isAnonymous();
            usersString.append(currentAnonymous ? "ANONYMOUS" : currentUserInfo);
            for (UserInfo userInfo : userInfos) {
                if (currentAnonymous || !userInfo.name.equals(currentUserInfo.name))
                    usersString.append("\t").append(userInfo);
            }
            fileWr.write(usersString + "\n");

            fileWr.write(serverInfo.exportName + "\n");
            fileWr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Pair<LogicsConnection, Pair<UserInfo, List<UserInfo>>> restoreServerAndUserInfos() {
        try {
            File file = SystemUtils.getUserFile(CONFIG_FILE_NAME, false);
            if (file.exists() && file.length() < 10000) { // don't restore if cfg file has grown large: most likely it's damaged
                FileReader fileRd = new FileReader(file);
                Scanner scanner = new Scanner(fileRd);
                String serverHost = scanner.hasNextLine() ? scanner.nextLine() : "";
                int serverPort = scanner.hasNextLine() ? Integer.parseInt(scanner.nextLine()) : 0;
                List<UserInfo> userInfos = new ArrayList<>();
                UserInfo currentUserInfo = null;
                if (scanner.hasNextLine()) {
                    String users = scanner.nextLine();
                    if (!users.isEmpty()) {
                        Scanner usersScanner = new Scanner(users);
                        usersScanner.useDelimiter("\\t");
                        while (usersScanner.hasNext()) {
                            String name = usersScanner.next().trim();
                            UserInfo userInfo;
                            if(name.equals("ANONYMOUS"))
                                userInfo = UserInfo.ANONYMOUS;
                            else {
                                boolean save = false;
                                String pass = null;
                                if (usersScanner.hasNext()) {
                                    save = Boolean.parseBoolean(usersScanner.next());
                                }
                                if (save && usersScanner.hasNext()) {
                                    pass = new String(Base64.decodeBase64(usersScanner.next()));
                                }
                                userInfo = new UserInfo(name, save, pass);
                                userInfos.add(userInfo);
                            }
                            if(currentUserInfo == null)
                                currentUserInfo = userInfo;
                        }
                    }
                }
                String serverDB = scanner.hasNextLine() ? scanner.nextLine() : "default";
                return new Pair<>(new LogicsConnection(serverHost, serverPort, serverDB), new Pair<>(currentUserInfo, userInfos));
            }
        } catch (IOException e) { // some problems with file
            return null;
        }
        return null;
    }

    private void updateAnonymousUIActivity() {
        boolean enable = !useAnonymousUI();
        loginLabel.setEnabled(enable);
        loginBox.setEnabled(enable);
        passwordLabel.setEnabled(enable);
        passwordField.setEnabled(enable);
        savePasswordCheckBox.setEnabled(enable);
        updateOK();
    }

    private UserInfo getUserInfo(String userName) {
        for (UserInfo userInfo : userInfos) {
            if (userInfo.name.equals(userName)) {
                return userInfo;
            }
        }
        return null;
    }
    
    private boolean isValid(LogicsConnection connection) {
        return connection.port != 0 && !connection.host.isEmpty() && !connection.exportName.isEmpty();
    }

    private boolean isValid(UserInfo userInfo) {
        return userInfo.isAnonymous() || !userInfo.name.isEmpty();
    }

    private boolean isOkEnabled() {
        return checkVersionError == null && (useAnonymousUI() || (isValid(getUserInfo()) && isValid(getServerInfo())));
    }

    private void updateOK() {
        buttonOK.setEnabled(isOkEnabled());
    }

    private void onOK() {
        this.serverInfo = getServerInfo();
        this.userInfo = getUserInfo(); 
        setVisible(false);
    }

    private UserInfo getUserInfo() {
        return useAnonymousUI() ? UserInfo.ANONYMOUS : new UserInfo(getCurrentItem(loginBox), savePasswordCheckBox.isSelected(), new String(passwordField.getPassword()));
    }

    private boolean useAnonymousUI() {
        return useAnonymousUICheckBox.isVisible() && useAnonymousUICheckBox.isSelected();
    }

    private LogicsConnection getServerInfo() {
        Pair<String, Integer> host = parseHost(getCurrentItem(serverHostComboBox));
        return new LogicsConnection(host.first, host.second, getCurrentItem(serverDBComboBox));
    }

    private Pair<String, Integer> parseHost(String server) {
        int pos = server.indexOf(':');
        if (pos == -1)
            return new Pair<>(server, 0);

        int port = 0;
        try {
            port = Integer.parseInt(server.substring(pos + 1));
        } catch (NumberFormatException ignored) {
        }

        return new Pair<>(server.substring(0, pos), port);
    }

    private void onCancel() {
        dispose();
        MainController.shutdown();
    }

    public void setWarningMsg(String msg) {
        ClientLoggers.clientLogger.info("setWarningMsg: " + msg);
        warningLabel.setText(msg != null ? "<html><body style='width: " + warningPanel.getSize().width + "'>" + msg + "</body></html>" : "");
        warningPanelWrapper.setVisible(msg != null && !msg.isEmpty());
        pack();
    }

    public static Pair<LogicsConnection, UserInfo> login(LogicsConnection serverInfo, UserInfo userInfo, String warningMsg) {

        // restoring saved server and user info (if no initializing empty)
        Pair<LogicsConnection, Pair<UserInfo, List<UserInfo>>> restoredData = restoreServerAndUserInfos();
        List<UserInfo> userInfos = null;
        if(restoredData != null) {
            if(serverInfo == null)
                serverInfo = restoredData.first;
            if(userInfo == null)
                userInfo = restoredData.second.first;
            userInfos = restoredData.second.second;
        }

        if(serverInfo == null)
            serverInfo = new LogicsConnection("localhost", 7652, "default");
        if(userInfo == null)
            userInfo = UserInfo.ANONYMOUS;
        if(userInfos == null)
            userInfos = new ArrayList<>();

        LoginDialog loginDialog = new LoginDialog(warningMsg, serverInfo, userInfo, userInfos);
        loginDialog.setVisible(true);
        
        if(loginDialog.serverInfo == null)
            return null;            
        
        Pair<LogicsConnection, UserInfo> result = new Pair<>(loginDialog.serverInfo, loginDialog.userInfo);

        // synchronizing userInfos
        final Result<List<UserInfo>> rUserInfos = new Result<>(userInfos);
        try {
            LogicsProvider.instance.runRequest(serverInfo, sessionObject -> {
                syncUsers(sessionObject.remoteLogics, rUserInfos);
                return null;
            });
        } catch (Throwable e) {
            ClientLoggers.clientLogger.error("Failed to synchronize users", e);
        }

        // saving server and user info
        storeServerAndUserInfos(result.first, result.second, rUserInfos.result);

        return result;
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

        JLabel serverLabel = new JLabel(ClientResourceBundle.getString("dialog.server.address"));
        settingsPanel.add(serverLabel, getGridBagConstraints(0, 0, true));

        serverHostComboBox = new JComboBox<>();
        serverHostComboBox.setEditable(true);
        settingsPanel.add(serverHostComboBox, getGridBagConstraints(0, 1, true));

        JLabel serverDBLabel = new JLabel(ClientResourceBundle.getString("dialog.server.name"));
        serverDBLabel.setLabelFor(serverDBComboBox);
        settingsPanel.add(serverDBLabel, getGridBagConstraints(1, 0, true));

        serverDBComboBox = new JComboBox<>();
        serverDBComboBox.setEditable(true);
        settingsPanel.add(serverDBComboBox, getGridBagConstraints(1, 1, true));

        useAnonymousUICheckBox = new JCheckBox(ClientResourceBundle.getString("dialog.use.anonymous.ui"), true);
        settingsPanel.add(useAnonymousUICheckBox, getGridBagConstraints(2, 0, false));

        loginLabel = new JLabel(ClientResourceBundle.getString("dialog.login"));
        loginLabel.setLabelFor(loginBox);
        settingsPanel.add(loginLabel, getGridBagConstraints(3, 0, true));

        loginBox = new JComboBox<>();
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

        JPanel subOKCancelPanel = new JPanel();
        subOKCancelPanel.setLayout(new BoxLayout(subOKCancelPanel, BoxLayout.X_AXIS));
        okCancelPanel.add(subOKCancelPanel, BorderLayout.EAST);

        buttonOK = new JButton(ClientResourceBundle.getString("dialog.ok"));
        buttonOK.setMnemonic('O');
        buttonOK.setDisplayedMnemonicIndex(0);
        subOKCancelPanel.add(buttonOK);
        subOKCancelPanel.add(Box.createHorizontalStrut(2));

        buttonCancel = new JButton(ClientResourceBundle.getString("dialog.cancel"));
        buttonCancel.setMnemonic('C');
        buttonCancel.setDisplayedMnemonicIndex(0);
        subOKCancelPanel.add(buttonCancel);

        warningPanel = new JPanel();
        warningPanel.setBackground(new Color(-39322));
        warningLabel = new JLabel("");
        warningPanel.add(warningLabel);
        warningPanelWrapper = new JPanel();
        warningPanelWrapper.setLayout(new BoxLayout(warningPanelWrapper, BoxLayout.Y_AXIS));
        warningPanelWrapper.add(warningPanel);
        warningPanelWrapper.setBorder(BorderFactory.createEmptyBorder(5, 1, 1, 1));
        mainPanel.add(warningPanelWrapper);
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
