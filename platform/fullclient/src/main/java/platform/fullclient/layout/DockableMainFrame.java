package platform.fullclient.layout;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CGrid;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.SingleCDockable;
import bibliothek.gui.dock.common.menu.*;
import bibliothek.gui.dock.common.theme.ThemeMap;
import bibliothek.gui.dock.facile.menu.RootMenuPiece;
import bibliothek.gui.dock.facile.menu.SubmenuPiece;
import bibliothek.gui.dock.support.menu.SeparatingMenuPiece;
import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import platform.client.Log;
import platform.client.Main;
import platform.client.MainFrame;
import platform.client.descriptor.view.LogicsDescriptorView;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.interop.exceptions.LoginException;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.RemoteException;

public class DockableMainFrame extends MainFrame {
    private CControl control;
    ViewManager view;


    private ClientNavigator mainNavigator;

    public DockableMainFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
        super(remoteNavigator);

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

        initDockStations(mainNavigator);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                super.windowClosing(event);
                try {
                    DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(baseDir, "layout.data")));
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
                ReportDialog dlg = new ReportDialog(Main.frame,  remoteForm);
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

    // важно, что в случае каких-либо Exception'ов при восстановлении форм нужно все игнорировать и открывать расположение "по умолчанию"
    private void initDockStations(ClientNavigator mainNavigator) {

        control = new CControl(this);
        view = new ViewManager(control, mainNavigator);
        control.setTheme(ThemeMap.KEY_ECLIPSE_THEME);

        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(new File(baseDir, "layout.data")));
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

        add(control.getContentArea(), BorderLayout.CENTER);
        CGrid grid = new CGrid(control);
        grid.add(0, 0, 1, 2, createDockable("Навигатор", mainNavigator));
        grid.add(0, 2, 1, 1, createDockable("Связанные формы", mainNavigator.relevantFormNavigator));
        grid.add(0, 3, 1, 1, createDockable("Классовые формы", mainNavigator.relevantClassNavigator));
        grid.add(0, 4, 1, 1, createDockable("Лог", Log.getPanel()));
        grid.add(1, 0, 5, 5, view.getGridArea());
        grid.add(0, 5, 6, 0.15, createStatusDockable(status));
        control.getContentArea().deploy(grid);

        setupMenu();
//        setVisible(true);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }


    void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createWindowMenu());
        menuBar.add(createViewMenu());
        menuBar.add(createOptionsMenu());
        menuBar.add(createHelpMenu());
        setJMenuBar(menuBar);
    }

    private JMenu createWindowMenu() {
        SubmenuPiece dockableMenu = new SubmenuPiece("Окно", false, new SingleCDockableListMenuPiece(control));
        return dockableMenu.getMenu();
    }

    private JMenu createViewMenu() {
        RootMenuPiece layout = new RootMenuPiece("Вид", false);
        layout.add(new SubmenuPiece("LookAndFeel", true, new CLookAndFeelMenuPiece(control)));
        layout.add(new SubmenuPiece("Тема", true, new CThemeMenuPiece(control)));
        layout.add(CPreferenceMenuPiece.setup(control));
        layout.add(new SeparatingMenuPiece(new CLayoutChoiceMenuPiece(control, false), true, false, false));

        return layout.getMenu();
    }

    JMenu createFileMenu() {
        JMenu Menu = new JMenu("Файл");
        JMenuItem openReport = new JMenuItem("Открыть отчет...");
        openReport.setToolTipText("Открывает ранее сохраненный отчет");
        final JFrame MainFrame = this;
        openReport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                FileDialog chooser = new FileDialog(MainFrame, "Отчет");
                chooser.setFilenameFilter(new FilenameFilter() {
                    public boolean accept(File directory, String file) {
                        String filename = file.toUpperCase();
                        return filename.endsWith(".JRPRINT");
                    }
                });
                chooser.setVisible(true);

                try {
                    view.openReport(chooser.getFile(), chooser.getDirectory());
                } catch (JRException e) {
                    throw new RuntimeException("Ошибка при открытии сохраненного отчета", e);
                }
            }
        });
        Menu.add(openReport);
        return Menu;
    }

    JMenu createOptionsMenu() {

        JMenu menu = new JMenu("Настройки");

        final JMenuItem changeUser = new JMenuItem("Изменить пользователя");
        changeUser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    final JTextField login = new JTextField();
                    final JPasswordField jpf = new JPasswordField();
                    JOptionPane jop = new JOptionPane(new Object[]{new JLabel("Логин"), login, new JLabel("Пароль"), jpf},
                            JOptionPane.QUESTION_MESSAGE,
                            JOptionPane.OK_CANCEL_OPTION);
                    JDialog dialog = jop.createDialog("Введите логин и пароль");
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

                        boolean check = Main.remoteLogics.checkUser(login.getText(), password);
                        if (check) {
                            Main.frame.remoteNavigator.relogin(login.getText());
                            Main.frame.updateUser();
                        } else
                            throw new RuntimeException();

                    }
                } catch (LoginException e) {
                    throw new RuntimeException(e);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        menu.add(changeUser);

        final JMenuItem logicsConfigurator = new JMenuItem("Настройка бизнес-логики");
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
        JMenu Menu = new JMenu("Справка");
        final JMenuItem About = new JMenuItem("О программе");
        About.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        Menu.add(About);
        return Menu;
    }


    public SingleCDockable createDockable(String title, JComponent navigator) {
        DefaultSingleCDockable dockable = new DefaultSingleCDockable(title, title, navigator);
        dockable.setCloseable(true);
        //dockable.setDefaultLocation(CDockable.ExtendedMode.MINIMIZED, CLocation.base().minimalEast());
        //dockable.setDefaultLocation(CDockable.ExtendedMode.EXTERNALIZED, CLocation.external(0, 0, 300, 300));
        return dockable;
    }

    public SingleCDockable createStatusDockable(JComponent navigator) {
        DefaultSingleCDockable dockable = new DefaultSingleCDockable("Log", "", navigator);
        dockable.setTitleShown(false);
        dockable.setResizeLockedVertically(true);
        dockable.setExternalizable(false);
        dockable.setCloseable(false);
        return dockable;
    }
}
