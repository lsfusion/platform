package platform.fullclient.layout;

import bibliothek.gui.dock.common.*;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.layout.ThemeMap;
import bibliothek.gui.dock.common.menu.*;
import bibliothek.gui.dock.facile.menu.RootMenuPiece;
import bibliothek.gui.dock.facile.menu.SubmenuPiece;
import bibliothek.gui.dock.support.menu.SeparatingMenuPiece;
import net.sf.jasperreports.engine.JRException;
import platform.client.Log;
import platform.client.Main;
import platform.client.MainFrame;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

public class DockableMainFrame extends MainFrame {
    private CControl control;
    ViewManager view;


    private ClientNavigator mainNavigator;
    public DockableMainFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
        super(remoteNavigator);

        mainNavigator = new ClientNavigator(remoteNavigator) {

            public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
                try {
                    view.openClient(element.ID, this, false);
                } catch (JRException e) {
                    throw new RuntimeException(e);
                }
            }

            public void openRelevantForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
                if (element.isPrintForm) {
                    view.openReport(element.ID, this, true);
                } else {
                    try {
                        view.openClient(element.ID, this, true);
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
    public void runReport(RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException {
        view.openReport(mainNavigator, remoteForm);
    }

    @Override
    public void runForm(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        try {
            view.openClient(mainNavigator, remoteForm);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    private void initDockStations(ClientNavigator mainNavigator) {
        control = new CControl(this);
        view = new ViewManager(control, mainNavigator);
        control.setTheme(ThemeMap.KEY_ECLIPSE_THEME);

        try {
            DataInputStream in = new DataInputStream(new FileInputStream(new File(baseDir, "layout.data")));
            view.getForms().read(in);
            control.getResources().readStream(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        add(control.getContentArea(), BorderLayout.CENTER);
        CGrid grid = new CGrid(control);
        grid.add(0, 0, 1, 2, createDockable("Навигатор", mainNavigator));
        grid.add(0, 2, 1, 1, createDockable("Связанные формы", mainNavigator.relevantFormNavigator));
        grid.add(0, 3, 1, 1, createDockable("Классовые формы", mainNavigator.relevantClassNavigator));
        grid.add(0, 4, 1, 1, createDockable("Лог", Log.getPanel()));
        grid.add(1, 0, 5, 5, view.getGridArea());
        control.getContentArea().deploy(grid);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        for (String s : control.layouts()) {
            if (s.equals("default")) {
                control.load("default");
                break;
            }
        }
        setupMenu();
        setVisible(true);
    }


    void setupMenu() {
        JMenuBar Menubar = new JMenuBar();
        Menubar.add(createFileMenu());
        Menubar.add(createWindowMenu());
        Menubar.add(createViewMenu());
        Menubar.add(createHelpMenu());
        setJMenuBar(Menubar);
    }

    private JMenu createWindowMenu() {
        SubmenuPiece dockableMenu = new SubmenuPiece("Окно", false, new SingleCDockableListMenuPiece(control));
        return dockableMenu.getMenu();
    }

    private JMenu createViewMenu() {
        RootMenuPiece layout = new RootMenuPiece( "Вид", false );
        layout.add( new SubmenuPiece( "LookAndFeel", true, new CLookAndFeelMenuPiece( control )));
        layout.add( new SubmenuPiece( "Тема", true, new CThemeMenuPiece( control )));
        layout.add( CPreferenceMenuPiece.setup( control ));
        layout.add(new SeparatingMenuPiece(new CLayoutChoiceMenuPiece(control, false), true, false, false ));

        return layout.getMenu();
    }

    JMenu createFileMenu() {
        JMenu Menu = new JMenu("Файл");
        JMenuItem OpenReport = new JMenuItem("Открыть отчет...");
        OpenReport.setToolTipText("Открывает ранее сохраненный отчет");
        final JFrame MainFrame = this;
        OpenReport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                FileDialog Chooser = new FileDialog(MainFrame, "Отчет");
                Chooser.setFilenameFilter(new FilenameFilter() {
                    public boolean accept(File directory, String file) {
                        String filename = file.toUpperCase();
                        return filename.endsWith(".JRPRINT");
                    }
                });
                Chooser.setVisible(true);

                try {
                    view.openReport(Chooser.getFile(), Chooser.getDirectory());
                } catch (JRException e) {
                    throw new RuntimeException("Ошибка при открытии сохраненного отчета", e);
                }
            }
        });
        Menu.add(OpenReport);
        return Menu;
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
}
