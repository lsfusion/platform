package platform.fullclient.layout;

import bibliothek.gui.dock.common.*;
import bibliothek.gui.dock.common.intern.CSetting;
import bibliothek.gui.dock.common.menu.*;
import bibliothek.gui.dock.common.theme.ThemeMap;
import bibliothek.gui.dock.facile.menu.RootMenuPiece;
import bibliothek.gui.dock.facile.menu.SubmenuPiece;
import bibliothek.gui.dock.support.menu.SeparatingMenuPiece;
import com.google.common.base.Throwables;
import net.sf.jasperreports.engine.JRException;
import platform.base.ExceptionUtils;
import platform.client.Log;
import platform.client.Main;
import platform.client.MainFrame;
import platform.client.descriptor.view.LogicsDescriptorView;
import platform.client.form.dispatch.ClientNavigatorActionDispatcher;
import platform.client.logics.DeSerializer;
import platform.client.navigator.ClientAbstractWindow;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorAction;
import platform.client.navigator.ClientNavigatorForm;
import platform.fullclient.navigator.NavigatorController;
import platform.interop.AbstractWindowType;
import platform.interop.exceptions.LoginException;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.ReportGenerationData;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static platform.base.BaseUtils.mergeLinked;
import static platform.client.ClientResourceBundle.getString;

public class DockableMainFrame extends MainFrame {
    private final ClientNavigatorActionDispatcher actionDispatcher;

    private final LinkedHashMap<SingleCDockable, ClientAbstractWindow> windowDockables = new LinkedHashMap<SingleCDockable, ClientAbstractWindow>();
    private final CControl mainControl;
    private final DockableManager dockableManager;

    private final NavigatorController navigatorController;
    private final ClientNavigator mainNavigator;

    public DockableMainFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
        super(remoteNavigator);

        DeSerializer.deserializeListClientNavigatorElementWithChildren(remoteNavigator.getNavigatorTree());

        mainNavigator = new ClientNavigator(remoteNavigator) {
            public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
                try {
                    dockableManager.openForm(this, element.getSID());
                } catch (JRException e) {
                    throw new RuntimeException(e);
                }
            }

            public void openModalForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
                dockableManager.openModalForm(element.getSID(), this, element.modalityType.isFullScreen());
            }

            @Override
            public void openAction(ClientNavigatorAction action) {
                executeNavigatorAction(action);
            }
        };

        actionDispatcher = new ClientNavigatorActionDispatcher(mainNavigator);

        navigatorController = new NavigatorController(mainNavigator);

        mainControl = new CControl(this);

        dockableManager = new DockableManager(mainControl, mainNavigator);

        initDockStations();

        setupMenu();

        navigatorController.update();

        focusPageIfNeeded();

        bindUIHandlers();
    }

    private void executeNavigatorAction(ClientNavigatorAction action) {
        try {
            actionDispatcher.dispatchResponse(remoteNavigator.executeNavigatorAction(action.getSID()));
        } catch (IOException e) {
            throw new RuntimeException(getString("errors.error.executing.action"), e);
        }
    }

    private void bindUIHandlers() {
        // временно отключаем из-за непредсказуемого поведения при измении окон
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                //windowClosing не срабатывает, если вызван dispose,
                //поэтому сохраняем лэйаут в windowClosed
                try {
                    mainControl.save("default");
                    DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(baseDir, "layout.data")));
                    dockableManager.getForms().write(out);
                    mainControl.getResources().writeStream(out);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void focusPageIfNeeded() {
        try {
            ClientFormDockable pageToFocus = null;
            ArrayList<String> savedForms = remoteNavigator.showDefaultForms()
                    ? remoteNavigator.getDefaultForms()
                    : new ArrayList<String>(dockableManager.getForms().getFormsList());

            dockableManager.getForms().getFormsList().clear();
            ClientFormDockable page;
            for (String formSID : savedForms) {
                page = dockableManager.openForm(mainNavigator, formSID);
                if (pageToFocus == null) {
                    pageToFocus = page;
                }
            }
            if (pageToFocus != null) {
                pageToFocus.intern().getController().getFocusController().setFocusedDockable(pageToFocus.intern(), null, true, true, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // важно, что в случае каких-либо Exception'ов при восстановлении форм нужно все игнорировать и открывать расположение "по умолчанию"
    private void initDockStations() {
        mainControl.setTheme(ThemeMap.KEY_ECLIPSE_THEME);

        loadLayout();

        // создаем все окна и их виды
        initWindows();

        CGrid mainGrid = createGrid();
        CContentArea mainContentArea = mainControl.getContentArea();
        mainContentArea.deploy(mainGrid);
        add(mainContentArea, BorderLayout.CENTER);

        setDefaultVisible();

        for (String s : mainControl.layouts()) {
            if (s.equals("default")) {
                try {
                    //проверяем, бы ли созданы новые Dockable
                    boolean hasNewDockables = false;
                    CSetting setting = (CSetting) mainControl.intern().getSetting(s);
                    if (setting != null) {
                        for (SingleCDockable dockable : windowDockables.keySet()) {
                            boolean isNewDockable = true;
                            for (int i = 0; i < setting.getModes().size(); i++) {
                                if (setting.getModes().getId(i).equals("single " + dockable.getUniqueId())) {
                                    isNewDockable = false;
                                    break;
                                }
                            }
                            if (isNewDockable) {
                                hasNewDockables = true;
                                break;
                            }
                        }
                    }
                    //если новые Dockable созданы не были, грузим сохранённое расположение
                    if (!hasNewDockables) {
                        mainControl.load("default");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mainContentArea.deploy(mainGrid); // иначе покажется пустая форма
                }
                break;
            }
        }
    }

    private void loadLayout() {
        File layoutFile = new File(baseDir, "layout.data");
        if (layoutFile.exists()) {
            DataInputStream in = null;
            try {
                in = new DataInputStream(new FileInputStream(layoutFile));
                dockableManager.getForms().read(in);
                mainControl.getResources().readStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void runReport(String reportSID, boolean isModal, ReportGenerationData generationData) throws IOException, ClassNotFoundException {
        if (isModal) {
            ReportDialog.showReportDialog(generationData);
        } else {
            dockableManager.openReport(reportSID, generationData);
        }
    }

    @Override
    public void runForm(RemoteFormInterface remoteForm, FormCloseListener closeListener) {
        try {
            ClientFormDockable dockable = dockableManager.openForm(mainNavigator, remoteForm, closeListener);
            if (closeListener != null) {
                dockable.setFocusMostRecentOnClose(false);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    private void initWindows() {
        ClientAbstractWindow formsWindow;
        LinkedHashMap<ClientAbstractWindow, JComponent> windows = new LinkedHashMap<ClientAbstractWindow, JComponent>();

        try {
            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(remoteNavigator.getCommonWindows()));

            windows.put(new ClientAbstractWindow(inStream), mainNavigator.relevantFormNavigator);
            windows.put(new ClientAbstractWindow(inStream), mainNavigator.relevantClassNavigator);
            windows.put(new ClientAbstractWindow(inStream), Log.recreateLogPanel());
            windows.put(new ClientAbstractWindow(inStream), status);

            formsWindow = new ClientAbstractWindow(inStream);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при считывании информации об окнах", e);
        }

        navigatorController.initWindowViews();

        windows = mergeLinked(windows, navigatorController.getWindowsViews());

        // инициализируем dockables
        for (Map.Entry<ClientAbstractWindow, JComponent> entry : windows.entrySet()) {
            ClientAbstractWindow window = entry.getKey();
            JComponent component = entry.getValue();
            if (window.position == AbstractWindowType.DOCKING_POSITION) {
                ClientWindowDockable dockable = new ClientWindowDockable(window, entry.getValue());
                navigatorController.recordDockable(component, dockable);
                windowDockables.put(dockable, window);
            } else {
                add(component, window.borderConstraint);
            }
        }

        windowDockables.put(dockableManager.getFormArea(), formsWindow);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createViewMenu());
        menuBar.add(createOptionsMenu());
        menuBar.add(createWindowMenu());
        menuBar.add(createHelpMenu());
        setJMenuBar(menuBar);
    }

    private CGrid createGrid() {
        CGrid grid = new CGrid(mainControl);
        for (Map.Entry<SingleCDockable, ClientAbstractWindow> entry : windowDockables.entrySet()) {
            ClientAbstractWindow window = entry.getValue();
            grid.add(window.x, window.y, window.width, window.height, entry.getKey());
        }
        return grid;
    }

    private void setDefaultVisible() {
        for (Map.Entry<SingleCDockable, ClientAbstractWindow> entry : windowDockables.entrySet()) {
            entry.getKey().setVisible(entry.getValue().visible);
        }
    }

    private JMenu createWindowMenu() {
        RootMenuPiece dockableMenu = new RootMenuPiece(getString("layout.menu.window"), false, new SingleCDockableListMenuPiece(mainControl));
        dockableMenu.add(new SeparatingMenuPiece(new CLayoutChoiceMenuPiece(mainControl, false), true, false, false));

        final JMenuItem reload = new JMenuItem((getString("layout.menu.window.default.location")));
        reload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mainControl.getContentArea().deploy(createGrid());
                setDefaultVisible();
                navigatorController.update();
            }
        });
        dockableMenu.getMenu().addSeparator();
        dockableMenu.getMenu().add(reload);

        return dockableMenu.getMenu();
    }

    private JMenu createViewMenu() {
        RootMenuPiece layout = new RootMenuPiece(getString("layout.menu.view"), false);
        layout.add(new SubmenuPiece(getString("layout.menu.view.look.and.feel"), true, new CLookAndFeelMenuPiece(mainControl)));
        layout.add(new SubmenuPiece(getString("layout.menu.view.theme"), true, new CThemeMenuPiece(mainControl)));
        layout.add(CPreferenceMenuPiece.setup(mainControl));

        return layout.getMenu();
    }

    private JMenu createFileMenu() {

        JMenu menu = new JMenu(getString("layout.menu.file"));
        final JMenuItem changeUser = new JMenuItem(getString("layout.menu.file.change.user"));
        changeUser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {

                    while (true) {
                        final JTextField login = new JTextField();
                        final JPasswordField jpf = new JPasswordField();
                        JOptionPane jop = new JOptionPane(
                                new Object[]{new JLabel(getString("layout.menu.file.login")), login, new JLabel(getString("layout.menu.file.password")), jpf},
                                JOptionPane.QUESTION_MESSAGE,
                                JOptionPane.OK_CANCEL_OPTION
                        );
                        JDialog dialog = jop.createDialog(DockableMainFrame.this, getString("layout.menu.file.enter.login.and.password"));
                        dialog.addComponentListener(new ComponentAdapter() {
                            @Override
                            public void componentShown(ComponentEvent e) {
                                login.requestFocusInWindow();
                            }
                        });
                        dialog.setVisible(true);
                        int result = (jop.getValue() != null) ? (Integer) jop.getValue() : JOptionPane.CANCEL_OPTION;
                        dialog.dispose();
                        String password = null;
                        if (result == JOptionPane.OK_OPTION) {
                            password = new String(jpf.getPassword());

                            try {
                                if (Main.remoteLogics.checkUser(login.getText(), password)) {
                                    Main.frame.remoteNavigator.relogin(login.getText());
                                    Main.frame.updateUser();
                                    break;
                                }
                            } catch (RemoteException e) {
                                if (ExceptionUtils.getInitialCause(e) instanceof LoginException) {
                                    JOptionPane.showMessageDialog(DockableMainFrame.this, ExceptionUtils.getInitialCause(e).getMessage(), getString("layout.menu.file.error.user.changing"), JOptionPane.ERROR_MESSAGE);
                                } else {
                                    throw new RuntimeException(e);
                                }
                            }
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        menu.add(changeUser);

        final JMenuItem changePassword = new JMenuItem(getString("layout.menu.file.change.password"));
        changePassword.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String login = null;
                try {
                    login = Main.frame.remoteNavigator.getCurrentUserLogin();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                while (true) {
                    final JPasswordField oldPassword = new JPasswordField();
                    final JPasswordField newPassword = new JPasswordField();
                    final JPasswordField confirmPassword = new JPasswordField();
                    JOptionPane jop = new JOptionPane(new Object[]{new JLabel(getString("layout.menu.file.login") + ": " + login),
                                                                   new JLabel(getString("layout.menu.file.old.password")), oldPassword,
                                                                   new JLabel(getString("layout.menu.file.new.password")), newPassword,
                                                                   new JLabel(getString("layout.menu.file.confirm.password")), confirmPassword},
                                                      JOptionPane.QUESTION_MESSAGE,
                                                      JOptionPane.OK_CANCEL_OPTION);
                    JDialog dialog = jop.createDialog(DockableMainFrame.this, getString("layout.menu.file.change.password"));
                    dialog.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentShown(ComponentEvent e) {
                            oldPassword.requestFocusInWindow();
                        }
                    });
                    dialog.setVisible(true);
                    int result = (jop.getValue() != null) ? (Integer) jop.getValue() : JOptionPane.CANCEL_OPTION;
                    dialog.dispose();

                    if (result == JOptionPane.OK_OPTION) {
                        String oldPass = new String(oldPassword.getPassword());
                        String newPass = new String(newPassword.getPassword());
                        String confirmPass = new String(confirmPassword.getPassword());
                        try {
                            if (Main.remoteLogics.checkUser(login, oldPass)) {
                                if (!"".equals(newPass)) {
                                    if (newPass.equals(confirmPass)) {
                                        Main.frame.remoteNavigator.changePassword(login, newPass);
                                    } else {
                                        JOptionPane.showMessageDialog(DockableMainFrame.this, getString("layout.menu.file.passwords.not.equals"));
                                        break;
                                    }
                                }
                                login = Main.frame.remoteNavigator.getCurrentUserLogin();
                                Main.frame.updateUser();
                                break;
                            }
                        } catch (IOException e1) {
                            if (ExceptionUtils.getInitialCause(e1) instanceof LoginException) {
                                JOptionPane.showMessageDialog(DockableMainFrame.this, ExceptionUtils.getInitialCause(e1).getMessage(), getString("layout.menu.file.error.password.changing"), JOptionPane.ERROR_MESSAGE);
                                break;
                            } else {
                                throw new RuntimeException(e1);
                            }
                        } catch (ClassNotFoundException e1) {
                            e1.printStackTrace();
                        }

                    } else {
                        break;
                    }
                }
            }
        }

        );

        menu.add(changePassword);

        menu.addSeparator();

        JMenuItem openReport = new JMenuItem(getString("layout.menu.file.open.report"));
        openReport.setToolTipText(getString("layout.menu.file.opens.previously.saved.report"));

        openReport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser chooser = new JFileChooser();
                chooser.addChoosableFileFilter(new FileNameExtensionFilter(getString("layout.menu.file.jasperReports.reports"), "jrprint"));
                if (chooser.showOpenDialog(DockableMainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        dockableManager.openReport(chooser.getSelectedFile());
                    } catch (JRException e) {
                        throw new RuntimeException(getString("layout.menu.file.error.opening.saved.report"), e);
                    }
                }
            }
        });
        menu.add(openReport);

        menu.addSeparator();

        final JMenuItem exit = new JMenuItem(getString("layout.menu.file.exit"));
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WindowEvent wev = new WindowEvent(DockableMainFrame.this, WindowEvent.WINDOW_CLOSING);
                Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
            }
        });

        menu.add(exit);

        return menu;
    }

    private JMenu createOptionsMenu() {

        JMenu menu = new JMenu(getString("layout.menu.options"));

        final JMenuItem logicsConfigurator = new JMenuItem(getString("layout.menu.options.configurator"));
        logicsConfigurator.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    Boolean configurator = Main.frame.remoteNavigator.getConfiguratorSecurityPolicy();
                    if ((configurator != null) && (configurator == true)) {
                        openLogicSetupForm();
                    } else {
                        JOptionPane.showMessageDialog(null, getString("descriptor.view.access.denied"), getString("descriptor.view.error"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            }
        });

        menu.add(logicsConfigurator);

        return menu;
    }

    private void openLogicSetupForm() {
        LogicsDescriptorView view = new LogicsDescriptorView(this, mainNavigator);

        Rectangle bounds = this.getBounds();
        bounds.x += 20;
        bounds.y += 20;
        bounds.width -= 40;
        bounds.height -= 40;
        view.setBounds(bounds);

        view.setVisible(true);
    }

    private JMenu createHelpMenu() {
        JMenu menu = new JMenu(getString("layout.menu.help"));
        final JMenuItem about = new JMenuItem(getString("layout.menu.help.about"));
        about.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = new JDialog(DockableMainFrame.this);
                Container contentPane = dialog.getContentPane();
                contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

                contentPane.add(new JLabel(Main.getLogo()));
                contentPane.add(new JSeparator(JSeparator.HORIZONTAL));

                String text = Main.getDisplayName();
                if (text == null) {
                    text = Main.PLATFORM_TITLE;
                } else {
                    text = "<html><b>" + text + "</b> powered by " + Main.PLATFORM_TITLE + "</html>";
                }
                JLabel labelName = new JLabel(text);
                labelName.setFont(labelName.getFont().deriveFont(10));
                contentPane.add(labelName);

                dialog.setTitle(about.getText());
                dialog.pack();
                dialog.setResizable(false);
                dialog.setLocationRelativeTo(DockableMainFrame.this);
                dialog.setVisible(true);
            }
        });
        menu.add(about);
        return menu;
    }
}
