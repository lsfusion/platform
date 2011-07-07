package platform.fullclient.layout;

import bibliothek.gui.dock.common.*;
import bibliothek.gui.dock.common.intern.CSetting;
import bibliothek.gui.dock.common.menu.*;
import bibliothek.gui.dock.common.mode.ExtendedMode;
import bibliothek.gui.dock.common.theme.ThemeMap;
import bibliothek.gui.dock.facile.menu.RootMenuPiece;
import bibliothek.gui.dock.facile.menu.SubmenuPiece;
import bibliothek.gui.dock.support.menu.SeparatingMenuPiece;
import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.DebugUtils;
import platform.client.ClientResourceBundle;
import platform.client.Log;
import platform.client.Main;
import platform.client.MainFrame;
import platform.client.descriptor.view.LogicsDescriptorView;
import platform.client.navigator.*;
import platform.fullclient.navigator.NavigatorController;
import platform.interop.AbstractWindowType;
import platform.interop.exceptions.LoginException;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

public class DockableMainFrame extends MainFrame {
    private CControl control;
    ViewManager view;


    private ClientNavigator mainNavigator;
    public static NavigatorController navigatorController;

    public DockableMainFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
        super(remoteNavigator);

        navigatorController = new NavigatorController(remoteNavigator);

        mainNavigator = new ClientNavigator(remoteNavigator) {

            public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
                try {
                    view.openClient(element.getSID(), this, false);
                } catch (JRException e) {
                    throw new RuntimeException(e);
                }
            }

            public void openRelevantForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
                if (element.isPrintForm) {
                    view.openReport(element.getSID(), this, true);
                } else {
                    try {
                        view.openClient(element.getSID(), this, true);
                    } catch (JRException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        };

        navigatorController.mainNavigator = mainNavigator;

        initDockStations(mainNavigator, navigatorController);

        setupMenu();

        navigatorController.update();

        try {
            ClientFormDockable pageToFocus = null;
            if (remoteNavigator.showDefaultForms()) {
                ArrayList<String> ids = remoteNavigator.getDefaultForms();
                view.getForms().getFormsList().clear();
                ClientFormDockable page = null;
                for (String id : ids) {
                    page = view.openClient(id.trim(), mainNavigator, false);
                    if (pageToFocus == null) {
                        pageToFocus = page;
                    }
                }
                if (page != null) {
                    page.setExtendedMode(ExtendedMode.MAXIMIZED);
                }
            } else {
                ArrayList<String> savedForms = new ArrayList<String>(view.getForms().getFormsList());
                view.getForms().getFormsList().clear();
                ClientFormDockable page;
                for (String id : savedForms) {
                    page = view.openClient(id, mainNavigator, false);
                    if (pageToFocus == null) {
                        pageToFocus = page;
                    }
                }
            }
            if (pageToFocus != null) {
                pageToFocus.intern().getController().getFocusController().setFocusedDockable(pageToFocus.intern(), null, true, true, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // временно отключаем из-за непредсказуемого поведения при измении окон
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                super.windowClosing(event);
                try {
                    control.save("default");
                    DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(baseDir, "layout.data")));
                    view.getForms().write(out);
                    control.getResources().writeStream(out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void runReport(RemoteFormInterface remoteForm, boolean isModal) throws ClassNotFoundException, IOException {
        if (isModal) {
            try {
                ReportDialog dlg = new ReportDialog(Main.frame, remoteForm);
                dlg.setVisible(true);
            } catch (JRException e) {
                throw new RuntimeException(e);
            }
        } else {
            view.openReport(mainNavigator, remoteForm);
        }
    }

    @Override
    public void runSingleGroupReport(RemoteFormInterface remoteForm, int groupId) throws IOException, ClassNotFoundException {
        view.openSingleGroupReport(mainNavigator, remoteForm, groupId);
    }

    @Override
    public void runSingleGroupXlsExport(RemoteFormInterface remoteForm, int groupId) throws IOException, ClassNotFoundException {
        ReportGenerator.exportToExcel(remoteForm, groupId);
    }

    @Override
    public void runForm(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        try {
            view.openClient(mainNavigator, remoteForm);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    private LinkedHashMap<ClientAbstractWindow, JComponent> windows = new LinkedHashMap<ClientAbstractWindow, JComponent>();
    private ClientFormsWindow formsWindow;

    private void initWindows(RemoteNavigatorInterface remoteNavigator) {

        try {
            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(remoteNavigator.getWindows()));

            windows.put(new ClientRelevantFormsWindow(inStream), mainNavigator.relevantFormNavigator);
            windows.put(new ClientRelevantClassFormsWindow(inStream), mainNavigator.relevantClassNavigator);
            windows.put(new ClientLogWindow(inStream), Log.getPanel());
            windows.put(new ClientStatusWindow(inStream), status);

            formsWindow = new ClientFormsWindow(inStream);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при считывании информации об окнах", e);
        }
    }

    private LinkedHashMap<? extends ClientAbstractWindow,? extends JComponent> getAbstractWindows(NavigatorController navigatorController) {
        LinkedHashMap<ClientNavigatorWindow, JComponent> views = new LinkedHashMap<ClientNavigatorWindow, JComponent>();
        for (ClientNavigatorWindow win : navigatorController.views.keySet()) {
            views.put(win, navigatorController.views.get(win).getView());
        }
        return BaseUtils.mergeLinked(windows, views);
    }

    LinkedHashMap<SingleCDockable, ClientAbstractWindow> dockables = new LinkedHashMap<SingleCDockable, ClientAbstractWindow>();

    // важно, что в случае каких-либо Exception'ов при восстановлении форм нужно все игнорировать и открывать расположение "по умолчанию"
    private void initDockStations(ClientNavigator mainNavigator, NavigatorController navigatorController) {

        control = new CControl(this);
        view = new ViewManager(control, mainNavigator);
        control.setTheme(ThemeMap.KEY_ECLIPSE_THEME);

        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(new File(baseDir, "layout.data")));
            view.getForms().read(in);
            control.getResources().readStream(in);
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

        // создаем все окна и их виды
        initWindows(remoteNavigator);
        navigatorController.initViews();

        LinkedHashMap<? extends ClientAbstractWindow, ? extends JComponent> windows = getAbstractWindows(navigatorController);

        // инициализируем dockables
        for (Map.Entry<? extends ClientAbstractWindow, ? extends JComponent> entry : windows.entrySet()) {
            ClientAbstractWindow window = entry.getKey();
            JComponent component = entry.getValue();
            if (window.position == AbstractWindowType.DOCKING_POSITION) {
                SingleCDockable dockable = createDockable(window, entry.getValue());
                navigatorController.recordDockable(/*(NavigatorView)*/component, dockable);
                dockables.put(dockable, window);
            } else {
                add(component, window.borderConstraint);
            }
        }

        dockables.put(view.getGridArea(), formsWindow);

        CGrid grid = createGrid();
        control.getContentArea().deploy(grid);

        add(control.getContentArea(), BorderLayout.CENTER);

        setDefaultVisible();

        for (String s : control.layouts()) {
            if (s.equals("default")) {
                try {
                    //проверяем, бы ли созданы новые Dockable
                    boolean hasNewDockables = false;
                    CSetting setting = (CSetting) control.intern().getSetting(s);
                    if (setting != null) {
                        for (SingleCDockable dockable : dockables.keySet()) {
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
                        control.load("default");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    control.getContentArea().deploy(grid); // иначе покажется пустая форма
                }
                break;
            }

        }
    }


    void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createViewMenu());
        menuBar.add(createOptionsMenu());
        menuBar.add(createWindowMenu());
        menuBar.add(createHelpMenu());
        setJMenuBar(menuBar);
    }

    private CGrid createGrid() {
        CGrid grid = new CGrid(control);
        for (Map.Entry<SingleCDockable, ClientAbstractWindow> entry : dockables.entrySet()) {
            ClientAbstractWindow window = entry.getValue();
            grid.add(window.x, window.y, window.width, window.height, entry.getKey());
        }
        return grid;
    }

    private void setDefaultVisible() {
        for (Map.Entry<SingleCDockable, ClientAbstractWindow> entry : dockables.entrySet()) {
            entry.getKey().setVisible(entry.getValue().visible);
        }
    }

    private JMenu createWindowMenu() {
        RootMenuPiece dockableMenu = new RootMenuPiece(ClientResourceBundle.getString("layout.menu.window"), false, new SingleCDockableListMenuPiece(control));
        dockableMenu.add(new SeparatingMenuPiece(new CLayoutChoiceMenuPiece(control, false), true, false, false));

        final JMenuItem reload = new JMenuItem((ClientResourceBundle.getString("layout.menu.window.default.location")));
        reload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                control.getContentArea().deploy(createGrid());
                setDefaultVisible();
                navigatorController.update();
            }
        });
        dockableMenu.getMenu().addSeparator();
        dockableMenu.getMenu().add(reload);

        return dockableMenu.getMenu();
    }

    private JMenu createViewMenu() {
        RootMenuPiece layout = new RootMenuPiece(ClientResourceBundle.getString("layout.menu.view"), false);
        layout.add(new SubmenuPiece(ClientResourceBundle.getString("layout.menu.view.look.and.feel"), true, new CLookAndFeelMenuPiece(control)));
        layout.add(new SubmenuPiece(ClientResourceBundle.getString("layout.menu.view.theme"), true, new CThemeMenuPiece(control)));
        layout.add(CPreferenceMenuPiece.setup(control));

        return layout.getMenu();
    }

    JMenu createFileMenu() {

        JMenu menu = new JMenu(ClientResourceBundle.getString("layout.menu.file"));
        final JMenuItem changeUser = new JMenuItem(ClientResourceBundle.getString("layout.menu.file.change.user"));
        changeUser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {

                    while (true) {
                        final JTextField login = new JTextField();
                        final JPasswordField jpf = new JPasswordField();
                        JOptionPane jop = new JOptionPane(new Object[]{new JLabel(ClientResourceBundle.getString("layout.menu.file.login")), login, new JLabel(ClientResourceBundle.getString("layout.menu.file.password")), jpf},
                                JOptionPane.QUESTION_MESSAGE,
                                JOptionPane.OK_CANCEL_OPTION);
                        JDialog dialog = jop.createDialog(DockableMainFrame.this, ClientResourceBundle.getString("layout.menu.file.enter.login.and.password"));
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
                                if (DebugUtils.getInitialCause(e) instanceof LoginException)
                                    JOptionPane.showMessageDialog(DockableMainFrame.this, DebugUtils.getInitialCause(e).getMessage(), ClientResourceBundle.getString("layout.menu.user.changing"), JOptionPane.ERROR_MESSAGE);
                                else
                                    throw new RuntimeException(e);
                            }
                        } else
                            break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        menu.add(changeUser);

        menu.addSeparator();

        JMenuItem openReport = new JMenuItem(ClientResourceBundle.getString("layout.menu.file.open.report"));
        openReport.setToolTipText(ClientResourceBundle.getString("layout.menu.file.opens.previously.saved.report"));

        openReport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser chooser = new JFileChooser();
                chooser.addChoosableFileFilter(new FileNameExtensionFilter(ClientResourceBundle.getString("layout.menu.file.jasperReports.reports"), "jrprint"));
                if (chooser.showOpenDialog(DockableMainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        view.openReport(chooser.getSelectedFile());
                    } catch (JRException e) {
                        throw new RuntimeException(ClientResourceBundle.getString("layout.menu.file.error.opening.saved.report"), e);
                    }
                }
            }
        });
        menu.add(openReport);

        menu.addSeparator();

        final JMenuItem exit = new JMenuItem(ClientResourceBundle.getString("layout.menu.file.exit"));
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

    JMenu createOptionsMenu() {

        JMenu menu = new JMenu(ClientResourceBundle.getString("layout.menu.options"));

        final JMenuItem logicsConfigurator = new JMenuItem(ClientResourceBundle.getString("layout.menu.options.configurator"));
        logicsConfigurator.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openLogicSetupForm();
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

    JMenu createHelpMenu() {
        JMenu menu = new JMenu(ClientResourceBundle.getString("layout.menu.help"));
        final JMenuItem about = new JMenuItem(ClientResourceBundle.getString("layout.menu.help.about"));
        about.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = new JDialog(DockableMainFrame.this);
                Container contentPane = dialog.getContentPane();
                contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

                contentPane.add(new JLabel(Main.getLogo()));
                contentPane.add(new JSeparator(JSeparator.HORIZONTAL));

                String text = Main.getDisplayName();
                if (text == null)
                    text = Main.PLATFORM_TITLE;
                else
                    text = "<html><b>" + text + "</b> powered by " + Main.PLATFORM_TITLE + "</html>";
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


    public DefaultSingleCDockable createDockable(ClientAbstractWindow window, JComponent navigator) {
        DefaultSingleCDockable dockable = new DefaultSingleCDockable(window.getSID(), window.caption, navigator);
        dockable.setTitleShown(window.titleShown);
        dockable.setCloseable(true);
        return dockable;
    }

}
