package platform.fullclient.layout;

import bibliothek.gui.dock.common.*;
import bibliothek.gui.dock.common.intern.CDockable;
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
import platform.client.Log;
import platform.client.Main;
import platform.client.MainFrame;
import platform.client.descriptor.view.LogicsDescriptorView;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.client.navigator.NavigatorView;
import platform.fullclient.navigator.NavigatorController;
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
import java.util.List;

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
        navigatorController.update();
        try {
            if (remoteNavigator.showDefaultForms()) {
                ArrayList<String> ids = remoteNavigator.getDefaultForms();
                view.getForms().getFormsList().clear();
                ClientFormDockable page = null;
                for (String id : ids) {
                    page = view.openClient(id.trim(), mainNavigator, false);
                }
                if (page != null) {
                    page.setExtendedMode(ExtendedMode.MAXIMIZED);
                }
            } else {
                ArrayList<String> savedForms = new ArrayList<String>(view.getForms().getFormsList());
                view.getForms().getFormsList().clear();
                for (String id : savedForms) {
                    view.openClient(id, mainNavigator, false);
                }
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

    Map<CDockable, Rectangle> dockables = new HashMap<CDockable, Rectangle>();

    // важно, что в случае каких-либо Exception'ов при восстановлении форм нужно все игнорировать и открывать расположение "по умолчанию"
    private void initDockStations(ClientNavigator mainNavigator, NavigatorController navigatorController) {

        control = new CControl(this);
        view = new ViewManager(control, mainNavigator);
        control.setTheme(ThemeMap.KEY_ECLIPSE_THEME);

//        DataInputStream in = null;
//        try {
//            in = new DataInputStream(new FileInputStream(new File(baseDir, "layout.data")));
//            view.getForms().read(in);
//            control.getResources().readStream(in);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (in != null) {
//                try {
//                    in.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

        add(control.getContentArea(), BorderLayout.CENTER);
        dockables.put(createDockable("relevantForms", "Связанные формы", mainNavigator.relevantFormNavigator), new Rectangle(0, 70, 20, 29));
        dockables.put(createDockable("relevantClassForms", "Классовые формы", mainNavigator.relevantClassNavigator), new Rectangle(0, 70, 20, 29));
        dockables.put(createDockable("log", "Лог", Log.getPanel()), new Rectangle(0, 70, 20, 29));

        for (NavigatorView view : navigatorController.getAllViews()) {
            DefaultSingleCDockable dockable = createDockable(view.getSID(), view.getCaption(), view);
            dockable.setTitleShown(view.isTitleShown());
            navigatorController.recordDockable(view, dockable);
            dockables.put(dockable, new Rectangle(view.getDockX(), view.getDockY(), view.getDockWidth(), view.getDockHeight()));
        }

        dockables.put(view.getGridArea(), new Rectangle(20, 20, 80, 79));
        dockables.put(createStatusDockable(status), new Rectangle(0, 99, 100, 1));

        CGrid grid = createGrid();
        control.getContentArea().deploy(grid);
        setupMenu();

//        for (String s : control.layouts()) {
//            if (s.equals("default")) {
//                try {
//                    control.load("default");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    control.getContentArea().deploy(grid); // иначе покажется пустая форма
//                }
//                break;
//            }
//        }
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
        for (CDockable dockable : dockables.keySet()) {
            Rectangle rectangle = dockables.get(dockable);
            grid.add(rectangle.x, rectangle.y, rectangle.width, rectangle.height, dockable);
        }
        return grid;
    }

    private JMenu createWindowMenu() {
        RootMenuPiece dockableMenu = new RootMenuPiece("Окно", false, new SingleCDockableListMenuPiece(control));
        dockableMenu.add(new SeparatingMenuPiece(new CLayoutChoiceMenuPiece(control, false), true, false, false));

        final JMenuItem reload = new JMenuItem("Расположение по умолчанию");
        reload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                control.getContentArea().deploy(createGrid());
            }
        });
        dockableMenu.getMenu().addSeparator();
        dockableMenu.getMenu().add(reload);

        return dockableMenu.getMenu();
    }

    private JMenu createViewMenu() {
        RootMenuPiece layout = new RootMenuPiece("Вид", false);
        layout.add(new SubmenuPiece("LookAndFeel", true, new CLookAndFeelMenuPiece(control)));
        layout.add(new SubmenuPiece("Тема", true, new CThemeMenuPiece(control)));
        layout.add(CPreferenceMenuPiece.setup(control));

        return layout.getMenu();
    }

    JMenu createFileMenu() {

        JMenu menu = new JMenu("Файл");

        final JMenuItem changeUser = new JMenuItem("Сменить пользователя");
        changeUser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {

                    while (true) {
                        final JTextField login = new JTextField();
                        final JPasswordField jpf = new JPasswordField();
                        JOptionPane jop = new JOptionPane(new Object[]{new JLabel("Логин"), login, new JLabel("Пароль"), jpf},
                                JOptionPane.QUESTION_MESSAGE,
                                JOptionPane.OK_CANCEL_OPTION);
                        JDialog dialog = jop.createDialog(DockableMainFrame.this, "Введите логин и пароль");
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
                                    JOptionPane.showMessageDialog(DockableMainFrame.this, DebugUtils.getInitialCause(e).getMessage(), "Смена пользователя", JOptionPane.ERROR_MESSAGE);
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

        JMenuItem openReport = new JMenuItem("Открыть отчет");
        openReport.setToolTipText("Открывает ранее сохраненный отчет");

        openReport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser chooser = new JFileChooser();
                chooser.addChoosableFileFilter(new FileNameExtensionFilter("Отчеты JasperReport", "jrprint"));
                if (chooser.showOpenDialog(DockableMainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        view.openReport(chooser.getSelectedFile());
                    } catch (JRException e) {
                        throw new RuntimeException("Ошибка при открытии сохраненного отчета", e);
                    }
                }
            }
        });
        menu.add(openReport);

        menu.addSeparator();

        final JMenuItem exit = new JMenuItem("Выход");
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

        JMenu menu = new JMenu("Настройки");

        final JMenuItem logicsConfigurator = new JMenuItem("Конфигуратор");
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
        JMenu menu = new JMenu("Справка");
        final JMenuItem about = new JMenuItem("О программе");
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


    public DefaultSingleCDockable createDockable(String id, String title, JComponent navigator) {
        DefaultSingleCDockable dockable = new DefaultSingleCDockable(id, title, navigator);
        dockable.setCloseable(true);
        return dockable;
    }

    public SingleCDockable createStatusDockable(JComponent navigator) {
        DefaultSingleCDockable dockable = new DefaultSingleCDockable("Log", "Лог", navigator);

        dockable.setTitleShown(false);
        dockable.setCloseable(false);
        return dockable;
    }
}
