package lsfusion.client.view;

import bibliothek.extension.gui.dock.theme.EclipseTheme;
import bibliothek.extension.gui.dock.theme.eclipse.OwnedRectEclipseBorder;
import bibliothek.extension.gui.dock.theme.eclipse.stack.EclipseTabPane;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.*;
import bibliothek.gui.DockController;
import bibliothek.gui.Dockable;
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
import bibliothek.gui.dock.util.color.ColorManager;
import com.google.common.base.Throwables;
import lsfusion.base.ReflectionUtils;
import lsfusion.base.lambda.EProvider;
import lsfusion.base.lambda.ERunnable;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.TableManager;
import lsfusion.client.base.log.Log;
import lsfusion.client.base.view.ClientDockable;
import lsfusion.client.base.view.ColorThemeChangeListener;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.controller.MainController;
import lsfusion.client.controller.remote.AsyncListener;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.controller.remote.RmiRequest;
import lsfusion.client.form.controller.FormsController;
import lsfusion.client.form.print.view.EditReportInvoker;
import lsfusion.client.form.print.view.ReportDialog;
import lsfusion.client.form.property.cell.classes.controller.EditorEventQueue;
import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.navigator.ClientNavigatorAction;
import lsfusion.client.navigator.NavigatorData;
import lsfusion.client.navigator.controller.NavigatorController;
import lsfusion.client.navigator.controller.dispatch.ClientNavigatorActionDispatcher;
import lsfusion.client.navigator.window.ClientAbstractWindow;
import lsfusion.client.navigator.window.view.ClientWindowDockable;
import lsfusion.interop.action.*;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.navigator.window.WindowType;
import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;
import org.jboss.netty.util.internal.NonReentrantLock;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.mergeLinked;
import static lsfusion.client.ClientResourceBundle.getString;

public class DockableMainFrame extends MainFrame implements AsyncListener {
    private static final Logger logger = Logger.getLogger(DockableMainFrame.class);

    private final ClientNavigatorActionDispatcher actionDispatcher;

    private final LinkedHashMap<SingleCDockable, ClientAbstractWindow> windowDockables = new LinkedHashMap<>();
    private final CControl mainControl;
    private final FormsController formsController;

    private final NavigatorController navigatorController;
    private final ClientNavigator mainNavigator;

    private NonReentrantLock lock = new NonReentrantLock();

    private final TableManager tableManager = new TableManager();

    private final EProvider<String> serverMessageProvider = new EProvider<String>() {
        @Override
        public String getExceptionally() throws Exception {
            return remoteNavigator == null ? null : remoteNavigator.getRemoteActionMessage();
        }
        @Override
        public void interrupt(boolean cancelable) {
            try {
                remoteNavigator.interrupt(cancelable);
            } catch (Exception ignored) {
            }
        }
    };

    private final EProvider<List<Object>> serverMessageListProvider = new EProvider<List<Object>>() {
        @Override
        public List<Object> getExceptionally() throws Exception {
            return remoteNavigator == null ? null : remoteNavigator.getRemoteActionMessageList();
        }
        @Override
        public void interrupt(boolean cancelable) {
            try {
                remoteNavigator.interrupt(cancelable);
            } catch (Exception ignored) {
            }
        }
    };

    @Override
    public void onAsyncStarted() {
    }

    @Override
    public void onAsyncFinished() {
    }

    private final RmiQueue rmiQueue;

    public DockableMainFrame(RemoteNavigatorInterface remoteNavigator, String userName) throws IOException {
        super(remoteNavigator, userName);

        NavigatorData navigatorData = NavigatorData.deserializeListClientNavigatorElementWithChildren(remoteNavigator.getNavigatorTree());

        mainNavigator = new ClientNavigator(remoteNavigator, navigatorData.root, navigatorData.windows) {
            @Override
            public void openAction(ClientNavigatorAction action, int modifiers) {
                executeNavigatorAction(action, (modifiers & InputEvent.CTRL_MASK) != 0);
            }
        };

        rmiQueue = new RmiQueue(tableManager, serverMessageProvider, serverMessageListProvider, this);

        actionDispatcher = new ClientNavigatorActionDispatcher(rmiQueue, mainNavigator);

        rmiQueue.setDispatcher(actionDispatcher);

        navigatorController = new NavigatorController(mainNavigator);

        mainControl = new CControl(this);
        
        ReflectionUtils.setPrivateFieldValue(DockController.class, mainControl.getController(), "colors", new ColorManager(mainControl.getController()) {
            @Override
            public Color get(String id) {
                if (id.equals("stack.border") || 
                        id.equals("stack.tab.border") || 
                        id.equals("stack.tab.border.selected") || 
                        id.equals("stack.tab.border.selected.focused") || 
                        id.equals("stack.tab.border.selected.focuslost") ||
                        id.equals("stack.tab.border.disabled")) {
                    return SwingDefaults.getComponentBorderColor();
                } else if (id.equals("stack.tab.text") ||
                        id.equals("stack.tab.text.selected") ||
                        id.equals("stack.tab.text.selected.focused") ||
                        id.equals("stack.tab.text.selected.focuslost") ||
                        id.equals("stack.tab.text.disabled")) {
                    return SwingDefaults.getTableCellForeground();
                } else if (id.equals("stack.tab.top.selected") ||
                        id.equals("stack.tab.top.selected.focused") ||
                        id.equals("stack.tab.top.selected.focuslost") ||
                        id.equals("stack.tab.bottom.selected") ||
                        id.equals("stack.tab.bottom.selected.focused") ||
                        id.equals("stack.tab.bottom.selected.focuslost")) {
                    return SwingDefaults.getSelectionColor();
                }
                return super.get(id);
            }
        });


        formsController = new FormsController(mainControl, mainNavigator);

        initDockStations(navigatorData);

        setupMenu();

        navigatorController.update();

        bindUIHandlers();

        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EditorEventQueue());
    }

    private void executeNavigatorAction(ClientNavigatorAction action, boolean suppressForbidDuplicate) {
        executeNavigatorAction(action.getCanonicalName(), 1, null, suppressForbidDuplicate);
    }

    public void executeNavigatorAction(final String actionSID, final int type, final Runnable action, Boolean suppressForbidDuplicate) {
        if (action != null) {
            if (lock.tryLock()) {
                tryExecuteNavigatorAction(actionSID, type, suppressForbidDuplicate);
            } else {
                SwingUtils.invokeLater(new ERunnable() {
                    @Override
                    public void run() throws Exception {
                        Timer timer = new Timer(1000, new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                action.run();
                            }
                        });
                        timer.setRepeats(false);
                        timer.start();
                    }
                });
            }
        } else {
            lock.lock();
            tryExecuteNavigatorAction(actionSID, type, suppressForbidDuplicate);
        }
    }

    private void processServerResponse(ServerResponse serverResponse) throws IOException {
        //ХАК: serverResponse == null теоретически может быть при реконнекте, когда RMI-поток убивается и remote-method возвращает null
        if (serverResponse != null) {
            actionDispatcher.dispatchResponse(serverResponse);
        }
    }

    public boolean isInServerInvocation(long requestIndex) throws RemoteException {
        return rmiQueue.directRequest(requestIndex, new RmiRequest<Boolean>("isInServerInvocation") {
            protected Boolean doRequest(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
                return mainNavigator.remoteNavigator.isInServerInvocation(requestIndex);
            }
        });
    }

    private void tryExecuteNavigatorAction(final String actionSID, final int type, final Boolean suppressForbidDuplicate) {
        try {
            rmiQueue.syncRequest(new RmiRequest<ServerResponse>("executeNavigatorAction") {
                @Override
                protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
                    return remoteNavigator.executeNavigatorAction(requestIndex, lastReceivedRequestIndex, actionSID, type);
                }

                @Override
                protected void onResponseGetFailed(long requestIndex, Exception e) throws Exception {
                    processServerResponse(new ServerResponse(requestIndex, new ClientAction[] {new ExceptionClientAction(e)}, isInServerInvocation(requestIndex)));
                }

                @Override
                protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
                    if(suppressForbidDuplicate != null && suppressForbidDuplicate && result != null) {
                        for(ClientAction action : result.actions) // хак, но весь механизм forbidDuplicate один большой хак
                            if(action instanceof FormClientAction)
                                ((FormClientAction) action).forbidDuplicate = false;                            
                    }
                    processServerResponse(result);
                }
            });
        } finally {
            lock.unlock();
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
                    formsController.getForms().write(out);
                    mainControl.getResources().writeStream(out);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void clearForms() {
        formsController.getForms().clear();
    }
    
    class ClientRectGradientPainter extends RectGradientPainter implements ColorThemeChangeListener {
        public ClientRectGradientPainter(EclipseTabPane pane, Dockable dockable) {
            super(pane, dockable);
            MainController.addColorThemeChangeListener(this);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, SwingDefaults.getComponentHeight());
        }

        @Override
        public void colorThemeChanged() {
            getPane().updateFullBorder();
        }
    }

    // важно, что в случае каких-либо Exception'ов при восстановлении форм нужно все игнорировать и открывать расположение "по умолчанию"
    private void initDockStations(NavigatorData navigatorData) {
        mainControl.getThemes().remove(ThemeMap.KEY_BASIC_THEME);
        mainControl.getThemes().remove(ThemeMap.KEY_BUBBLE_THEME);
        mainControl.getThemes().remove(ThemeMap.KEY_FLAT_THEME);
        mainControl.getThemes().remove(ThemeMap.KEY_SMOOTH_THEME);

        loadLayout();
        
        mainControl.setTheme(ThemeMap.KEY_ECLIPSE_THEME);
        
        // c/p ArchGradientPainter.FACTORY
        mainControl.getController().getProperties().set(EclipseTheme.TAB_PAINTER, new TabPainter(){
            public TabComponent createTabComponent(EclipseTabPane pane, Dockable dockable ){
                return new ClientRectGradientPainter(pane, dockable);
            }

            public TabPanePainter createDecorationPainter( EclipseTabPane pane ){
                return new LinePainter( pane );
            }

            public InvisibleTab createInvisibleTab( InvisibleTabPane pane, Dockable dockable ){
                return new DefaultInvisibleTab( pane, dockable );
            }

            public Border getFullBorder( BorderedComponent owner, DockController controller, Dockable dockable ){
                return new OwnedRectEclipseBorder( owner, controller, true );
            }
        });
        
        mainControl.getController().getProperties().set(EclipseTheme.BORDER_MODIFIER, border -> {
            if (border != null) {
                return BorderFactory.createLineBorder(SwingDefaults.getComponentBorderColor());
            }
            return null;
        });

        // создаем все окна и их виды
        initWindows(navigatorData);

        CGrid mainGrid = createGrid();
        CContentArea mainContentArea = mainControl.getContentArea();
        mainContentArea.deploy(mainGrid);
        mainControl.getLocationManager().refresh(); // есть баг похоже, что при инициализации грида, не обновляется dockable.mode, как следствие в history не попадает location, и при setVisible (в >=14 версии из-за https://github.com/Benoker/DockingFrames/commit/ab648db502ffa2783c734f8db4ed5ce4b42cef32) окно улетает в WorkingArea

        setContent(mainContentArea);

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
                formsController.getForms().read(in);
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
    public Integer runReport(final List<ReportPath> customReportPathList, final String formSID, boolean isModal, ReportGenerationData generationData, String printerName) throws IOException, ClassNotFoundException {
        return runReport(isModal, generationData, printerName, new EditReportInvoker() {
            @Override
            public boolean hasCustomReports() throws RemoteException {
                return !customReportPathList.isEmpty();
            }
            
            @Override
            public void invokeAddReport() throws RemoteException {
                try {
                    MainController.addReportPathList(customReportPathList, formSID);
                } catch (Exception e) {
                    throw new RuntimeException(getString("form.error.printing.form"), e);
                }
            }

            @Override
            public void invokeRecreateReport() throws RemoteException {
                try {
                    MainController.recreateReportPathList(customReportPathList, formSID);
                } catch (Exception e) {
                    throw new RuntimeException(getString("form.error.printing.form"), e);
                }
            }

            @Override
            public void invokeEditReport() throws RemoteException {
                try {
                    MainController.editReportPathList(customReportPathList);
                } catch (Exception e) {
                    throw new RuntimeException(getString("form.error.printing.form"), e);
                }
            }

            @Override
            public void invokeDeleteReport() throws RemoteException {
                try {
                    MainController.deleteReportPathList(customReportPathList);
                } catch (Exception e) {
                    throw new RuntimeException(getString("form.error.printing.form"), e);
                }
            }
        });
    }

    @Override
    public Integer runReport(boolean isModal, ReportGenerationData generationData, String printerName, EditReportInvoker editInvoker) throws IOException, ClassNotFoundException {
        if (isModal) {
            return ReportDialog.showReportDialog(generationData, printerName, editInvoker);
        } else {
            return formsController.openReport(generationData, printerName, editInvoker);
        }
    }

    @Override
    public ClientFormDockable runForm(String canonicalName, String formSID, boolean forbidDuplicate, RemoteFormInterface remoteForm, byte[] firstChanges, FormCloseListener closeListener) {
        try {
            return formsController.openForm(mainNavigator, canonicalName, formSID, forbidDuplicate, remoteForm, firstChanges, closeListener);
        } catch (Exception e) {
            if(closeListener != null)
                closeListener.formClosed(true);

            Throwables.propagate(e);
        }
        return null;
    }

    private void initWindows(NavigatorData navigatorData) {
        ClientAbstractWindow formsWindow;
        LinkedHashMap<ClientAbstractWindow, JComponent> windows = new LinkedHashMap<>();

        try {
            windows.put(navigatorData.logs, Log.recreateLogPanel());
            windows.put(navigatorData.status, status);

            formsWindow = navigatorData.forms;
        } catch (Exception e) {
            throw new RuntimeException("Error getting common windows:", e);
        }

        navigatorController.initWindowViews();

        windows = mergeLinked(windows, navigatorController.getWindowsViews());

        // инициализируем dockables
        for (Map.Entry<ClientAbstractWindow, JComponent> entry : windows.entrySet()) {
            ClientAbstractWindow window = entry.getKey();
            JComponent component = entry.getValue();
            if (window.position == WindowType.DOCKING_POSITION) {
                ClientWindowDockable dockable = new ClientWindowDockable(window, entry.getValue());
                dockable.setMinimizable(false);
                navigatorController.recordDockable(component, dockable);
                windowDockables.put(dockable, window);
            } else {
                add(component, window.borderConstraint);
            }
        }

        windowDockables.put(formsController.getFormArea(), formsWindow);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
//        menuBar.add(createViewMenu());
//        menuBar.add(createOptionsMenu());
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

                // удаляем файл с расположением, чтобы этим же действием лечить возможные нестыковки синхронизации в разных версиях DockingFrames
                File layoutFile = new File(baseDir, "layout.data");
                if (layoutFile.exists()) {
                    try {
                        layoutFile.delete();
                    } catch (SecurityException ignored) {}
                }
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
                        formsController.openReport(chooser.getSelectedFile());
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
        return new JMenu(getString("layout.menu.options"));
    }

    private JMenu createHelpMenu() {
        JMenu menu = new JMenu(getString("layout.menu.help"));
        final JMenuItem about = new JMenuItem(getString("layout.menu.help.about"));
        about.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = new JDialog(DockableMainFrame.this, true);
                Container contentPane = dialog.getContentPane();
                contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

                JLabel label = new JLabel(MainController.getLogo());
                label.setBorder(new EmptyBorder(10, 10, 10, 10));
                contentPane.add(label);
                contentPane.add(new JSeparator(JSeparator.HORIZONTAL));

                String text = MainController.getHelpTitle();
                JLabel labelName = new JLabel(text);
                labelName.setFont(labelName.getFont().deriveFont(Font.PLAIN, MainFrame.getIntUIFontSize(12)));
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

    public void activateForm(String formCanonicalName) {
        for (ClientDockable openedForm : formsController.openedForms) {
            if (openedForm.getCanonicalName() != null && openedForm.getCanonicalName().equals(formCanonicalName)) {
                openedForm.toFront();
                openedForm.requestFocusInWindow();
                openedForm.onOpened();
                break;
            }
        }
    }

    public void maximizeForm() {
        //setExtendedMode вызывается для одной формы, но влияет на все
        if(!formsController.openedForms.isEmpty()) {
            formsController.openedForms.get(0).setExtendedMode(ExtendedMode.MAXIMIZED);
        }
    }
}
