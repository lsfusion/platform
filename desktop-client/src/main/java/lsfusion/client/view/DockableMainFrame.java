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
import bibliothek.gui.dock.common.intern.*;
import bibliothek.gui.dock.common.mode.ExtendedMode;
import bibliothek.gui.dock.common.theme.ThemeMap;
import bibliothek.gui.dock.control.ControllerSetupCollection;
import bibliothek.gui.dock.control.DefaultDockControllerFactory;
import bibliothek.gui.dock.control.DockRelocator;
import bibliothek.gui.dock.control.relocator.DefaultDockRelocator;
import bibliothek.gui.dock.facile.mode.Location;
import bibliothek.gui.dock.support.mode.ModeSettings;
import bibliothek.gui.dock.title.DockTitle;
import bibliothek.gui.dock.util.color.ColorManager;
import com.google.common.base.Throwables;
import lsfusion.base.ReflectionUtils;
import lsfusion.base.lambda.EProvider;
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
import lsfusion.client.form.controller.DockableRepository;
import lsfusion.client.form.controller.FormsController;
import lsfusion.client.form.print.view.EditReportInvoker;
import lsfusion.client.form.print.view.ReportDialog;
import lsfusion.client.form.property.async.ClientAsyncOpenForm;
import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.navigator.ClientNavigatorAction;
import lsfusion.client.navigator.NavigatorData;
import lsfusion.client.navigator.controller.AsyncFormController;
import lsfusion.client.navigator.controller.NavigatorController;
import lsfusion.client.navigator.controller.dispatch.ClientNavigatorActionDispatcher;
import lsfusion.client.navigator.window.ClientAbstractWindow;
import lsfusion.client.navigator.window.view.ClientWindowDockable;
import lsfusion.interop.action.*;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.navigator.window.WindowType;
import org.apache.log4j.Logger;
import org.jboss.netty.util.internal.NonReentrantLock;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.String;
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
            public long openAction(ClientNavigatorAction action, int modifiers, boolean sync) {
                return executeNavigatorAction(action, (modifiers & InputEvent.CTRL_MASK) != 0, sync);
            }
        };

        rmiQueue = new RmiQueue(tableManager, serverMessageProvider, serverMessageListProvider, this);

        actionDispatcher = new ClientNavigatorActionDispatcher(rmiQueue, mainNavigator);

        rmiQueue.setDispatcher(actionDispatcher);

        navigatorController = new NavigatorController(mainNavigator);

        mainControl = new CControl(this, new EfficientControlFactory() {
            @Override
            public DockController createController(CControl owner) {
                return new CDockController(owner, new DefaultDockControllerFactory() {
                    @Override
                    public DockRelocator createRelocator(DockController controller, ControllerSetupCollection setup) {
                        return new DefaultDockRelocator(controller, setup) {
                            @Override
                            protected void dragMousePressed(MouseEvent e, DockTitle title, Dockable dockable) {
                                boolean forbidDragAndDrop = false;
                                if (dockable instanceof CommonDockable) {
                                    CDockable window = ((CommonDockable) dockable).getDockable();
                                    forbidDragAndDrop = window instanceof SingleCDockable && windowDockables.containsKey(window);
                                }
                                if (!forbidDragAndDrop) {
                                    super.dragMousePressed(e, title, dockable);
                                }
                            }
                        };
                    }
                });
            }
        });
        
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

        navigatorController.update();

        bindUIHandlers();
    }

    private long executeNavigatorAction(ClientNavigatorAction action, boolean suppressForbidDuplicate, boolean sync) {
        return executeNavigatorAction(action.getCanonicalName(), 1, null, suppressForbidDuplicate, sync);
    }

    public void executeNavigatorAction(final String actionSID, final int type, final Runnable action, Boolean suppressForbidDuplicate) {
        executeNavigatorAction(actionSID, type, action, suppressForbidDuplicate, true);
    }

    private long executeNavigatorAction(final String actionSID, final int type, final Runnable action, Boolean suppressForbidDuplicate, boolean sync) {
        if (action != null) {
            if (lock.tryLock()) {
                return tryExecuteNavigatorAction(actionSID, type, suppressForbidDuplicate, sync);
            } else {
                SwingUtils.invokeLater(() -> {
                    Timer timer = new Timer(1000, e -> action.run());
                    timer.setRepeats(false);
                    timer.start();
                });
                return -1;
            }
        } else {
            lock.lock();
            return tryExecuteNavigatorAction(actionSID, type, suppressForbidDuplicate, sync);
        }
    }

    public AsyncFormController getAsyncFormController(long requestIndex) {
        return actionDispatcher.getAsyncFormController(requestIndex);
    }

    private void processServerResponse(ServerResponse serverResponse) throws IOException {
        //ХАК: serverResponse == null теоретически может быть при реконнекте, когда RMI-поток убивается и remote-method возвращает null
        if (serverResponse != null) {
            actionDispatcher.dispatchServerResponse(serverResponse);
        }
    }

    public boolean isInServerInvocation(long requestIndex) throws RemoteException {
        return rmiQueue.directRequest(requestIndex, new RmiRequest<Boolean>("isInServerInvocation") {
            protected Boolean doRequest(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
                return mainNavigator.remoteNavigator.isInServerInvocation(requestIndex);
            }
        });
    }

    private long tryExecuteNavigatorAction(final String actionSID, final int type, final Boolean suppressForbidDuplicate, boolean sync) {
        try {
            RmiRequest<ServerResponse> request = new RmiRequest<ServerResponse>("executeNavigatorAction") {
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
                    if(result != null) {
                        formsController.setLastCompletedRequest(result.requestIndex);
                    }
                }
            };
            if(sync) {
                rmiQueue.syncRequest(request);
            } else {
                rmiQueue.asyncRequest(request);
            }
            return request.getRequestIndex();
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

    public DockableRepository getForms() {
        return formsController.getForms();
    }

    public void clearForms() {
        formsController.getForms().clear();
    }

    public void asyncOpenForm(ClientAsyncOpenForm asyncOpenForm, long requestIndex) {
        asyncOpenForm(getAsyncFormController(requestIndex), asyncOpenForm);
    }

    public void asyncOpenForm(AsyncFormController asyncFormController, ClientAsyncOpenForm asyncOpenForm) {
        formsController.asyncOpenForm(asyncFormController, asyncOpenForm);
    }

    @Override
    public void clean() {
        formsController.clean();
        super.clean();
    }

    // copy-pasted from previous method 
    public void resetWindowsLayout() {
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

    class ClientRectGradientPainter extends RectGradientPainter implements ColorThemeChangeListener {
        public ClientRectGradientPainter(EclipseTabPane pane, Dockable dockable) {
            super(pane, dockable);
            MainController.addColorThemeChangeListener(this);
        }

        @Override
        public void updateBorder() {
            // draw no border
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

        // Unlike web-client desktop-client restores layout from layout.data, where all windows - 
        // visible and invisible - are stored. As it seems impossible to check whether the window remained (in-)visible
        // or changed its state, we store in windowDockables only visible windows.
        for (String s : mainControl.layouts()) {
            if (s.equals("default")) {
                try {
                    //проверяем, бы ли созданы новые Dockable
                    boolean dockablesChanged = false;
                    CSetting setting = (CSetting) mainControl.intern().getSetting(s);
                    if (setting != null) {
                        ModeSettings<Location, Location> modes = setting.getModes();
                        if (windowDockables.size() != modes.size()) {
                            dockablesChanged = true;
                        } else {
                            for (SingleCDockable dockable : windowDockables.keySet()) {
                                boolean isNewDockable = true;
                                for (int i = 0; i < modes.size(); i++) {
                                    if (modes.getId(i).equals("single " + dockable.getUniqueId())) {
                                        isNewDockable = false;
                                        break;
                                    }
                                }
                                if (isNewDockable) {
                                    dockablesChanged = true;
                                    break;
                                }
                            }
                        }
                    }
                    //если новые Dockable созданы не были, грузим сохранённое расположение
                    if (!dockablesChanged) {
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
    public Integer runReport(final List<String> customReportPathList, final String formCaption, final java.lang.String formSID, boolean isModal, ReportGenerationData generationData, java.lang.String printerName) throws IOException, ClassNotFoundException {
        return runReport(isModal, formCaption, generationData, printerName, new EditReportInvoker() {
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
    public Integer runReport(boolean isModal, String formCaption, ReportGenerationData generationData, String printerName, EditReportInvoker editInvoker) throws IOException, ClassNotFoundException {
        if (isModal) {
            return ReportDialog.showReportDialog(generationData, formCaption, printerName, editInvoker);
        } else {
            return formsController.openReport(generationData, formCaption, printerName, editInvoker);
        }
    }

    @Override
    public ClientFormDockable runForm(AsyncFormController asyncFormController, String canonicalName, String formSID, boolean forbidDuplicate, RemoteFormInterface remoteForm, byte[] firstChanges, FormCloseListener closeListener, String formId) {
        try {
            return formsController.openForm(asyncFormController, mainNavigator, canonicalName, formSID, forbidDuplicate, remoteForm, firstChanges, closeListener, formId);
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
            if (window.visible) {
                if (window.position == WindowType.DOCKING_POSITION) {
                    ClientWindowDockable dockable = new ClientWindowDockable(window, entry.getValue());
                    dockable.setMinimizable(false);
                    navigatorController.recordDockable(component, dockable);
                    windowDockables.put(dockable, window);
                } else {
                    add(component, window.borderConstraint);
                }
            }
        }

        if (formsWindow.visible) {
            windowDockables.put(formsController.getFormArea(), formsWindow);
        }
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

    public void closeForm(String formId) {
        for (ClientDockable openedForm : formsController.openedForms) {
            if (formId.equals(openedForm.formId)) {
                openedForm.onClosing();
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
