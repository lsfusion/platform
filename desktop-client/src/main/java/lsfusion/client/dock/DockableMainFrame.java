package lsfusion.client.dock;

import bibliothek.gui.dock.common.CContentArea;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CGrid;
import bibliothek.gui.dock.common.SingleCDockable;
import bibliothek.gui.dock.common.intern.CSetting;
import bibliothek.gui.dock.common.menu.CLayoutChoiceMenuPiece;
import bibliothek.gui.dock.common.menu.CPreferenceMenuPiece;
import bibliothek.gui.dock.common.menu.CThemeMenuPiece;
import bibliothek.gui.dock.common.menu.SingleCDockableListMenuPiece;
import bibliothek.gui.dock.common.mode.ExtendedMode;
import bibliothek.gui.dock.common.theme.ThemeMap;
import bibliothek.gui.dock.facile.menu.RootMenuPiece;
import bibliothek.gui.dock.facile.menu.SubmenuPiece;
import bibliothek.gui.dock.support.menu.SeparatingMenuPiece;
import com.google.common.base.Throwables;
import lsfusion.base.DefaultFormsType;
import lsfusion.client.*;
import lsfusion.client.descriptor.view.LogicsDescriptorView;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.dispatch.ClientNavigatorActionDispatcher;
import lsfusion.client.logics.DeSerializer;
import lsfusion.client.navigator.*;
import lsfusion.interop.AbstractWindowType;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.mergeLinked;
import static lsfusion.client.ClientResourceBundle.getString;

public class DockableMainFrame extends MainFrame {
    private static final Logger logger = Logger.getLogger(DockableMainFrame.class);

    private final ClientNavigatorActionDispatcher actionDispatcher;

    private final LinkedHashMap<SingleCDockable, ClientAbstractWindow> windowDockables = new LinkedHashMap<SingleCDockable, ClientAbstractWindow>();
    private final CControl mainControl;
    private final DockableManager dockableManager;

    private final NavigatorController navigatorController;
    private final ClientNavigator mainNavigator;

    public DockableMainFrame(RemoteNavigatorInterface remoteNavigator) throws IOException {
        super(remoteNavigator);

        DeSerializer.NavigatorData navigatorData = DeSerializer.deserializeListClientNavigatorElementWithChildren(remoteNavigator.getNavigatorTree());

        mainNavigator = new ClientNavigator(remoteNavigator, navigatorData.root, navigatorData.windows) {
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

    public void focusPageIfNeeded() {
        ClientFormDockable pageToFocus = null;
        DefaultFormsType showDefaultForms = null;
        try {
            showDefaultForms = remoteNavigator.showDefaultForms();
            List<String> savedForms;
            switch (showDefaultForms) {
                case DEFAULT:
                    savedForms = remoteNavigator.getDefaultForms();
                    break;
                case RESTORE:
                    savedForms = dockableManager.getForms().getFormsList();
                    break;
                case NONE:
                default:
                    savedForms = new ArrayList<String>();
            }
            dockableManager.getForms().clear();
            ClientFormDockable page;
            for (String formSID : savedForms) {
                page = dockableManager.openForm(mainNavigator, formSID);
                if (pageToFocus == null) {
                    pageToFocus = page;
                }
            }
        } catch (Exception e) {
            logger.error("Error loading default layout", e);
        } finally {
            if (pageToFocus != null) {
                pageToFocus.intern().getController().getFocusController().setFocusedDockable(pageToFocus.intern(), null, true, true, true);

                if (showDefaultForms == DefaultFormsType.DEFAULT) {
                    pageToFocus.setExtendedMode(ExtendedMode.MAXIMIZED);
                }
            }
        }
    }

    // важно, что в случае каких-либо Exception'ов при восстановлении форм нужно все игнорировать и открывать расположение "по умолчанию"
    private void initDockStations() {
        mainControl.setTheme(ThemeMap.KEY_FLAT_THEME);

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
    public void runReport(final String reportSID, boolean isModal, ReportGenerationData generationData) throws IOException, ClassNotFoundException {
        EditReportInvoker editInvoker = new EditReportInvoker() {
            @Override
            public void invokeEditReport() throws RemoteException {
                new ClientFormController(reportSID, Main.remoteNavigator.createForm(reportSID, null, false, true), mainNavigator).runEditReport();
            }
        };

        runReport(reportSID, isModal, generationData, editInvoker);
    }

    @Override
    public void runReport(final String reportSID, boolean isModal, ReportGenerationData generationData, EditReportInvoker editInvoker) throws IOException, ClassNotFoundException {
        if (isModal) {
            ReportDialog.showReportDialog(generationData, editInvoker);
        } else {
            dockableManager.openReport(reportSID, generationData, editInvoker);
        }
    }

    @Override
    public void runForm(RemoteFormInterface remoteForm, FormCloseListener closeListener) {
        try {
            dockableManager.openForm(mainNavigator, remoteForm, closeListener);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    private void initWindows() {
        ClientAbstractWindow formsWindow;
        LinkedHashMap<ClientAbstractWindow, JComponent> windows = new LinkedHashMap<ClientAbstractWindow, JComponent>();

        try {
            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(remoteNavigator.getCommonWindows()));

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
                // deploy почему-то вываливается с ошибкой... похоже на баг в DockingFrames,
                // но т.к. в данном случае мы контролируем, что ложить в лэйаут, то просто выраубаем валидацию..
                // mainControl.getContentArea().deploy(createGrid()); <=> ...dropTree(.., true)
                mainControl.getContentArea().getCenter().dropTree(createGrid().toTree(), false);

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
        // todo: изменение LAF пока не работает
//        layout.add(new SubmenuPiece(getString("layout.menu.view.look.and.feel"), true, new CLookAndFeelMenuPiece(mainControl)));
        layout.add(new SubmenuPiece(getString("layout.menu.view.theme"), true, new CThemeMenuPiece(mainControl)));
        layout.add(CPreferenceMenuPiece.setup(mainControl));

        return layout.getMenu();
    }

    private JMenu createFileMenu() {

        JMenu menu = new JMenu(getString("layout.menu.file"));

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
                if (Main.configurationAccessAllowed) {
                    openLogicSetupForm();
                } else {
                    JOptionPane.showMessageDialog(null, getString("descriptor.view.access.denied"), getString("descriptor.view.error"), JOptionPane.ERROR_MESSAGE);
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
                    text = Main.LSFUSION_TITLE;
                } else {
                    text = "<html><b>" + text + "</b> powered by " + Main.LSFUSION_TITLE + "</html>";
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
