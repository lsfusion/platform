package platform.client.layout;

import bibliothek.demonstration.util.LookAndFeelMenu;
import bibliothek.gui.DockController;
import bibliothek.gui.DockFrontend;
import bibliothek.gui.DockStation;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.DefaultDockable;
import bibliothek.gui.dock.FlapDockStation;
import bibliothek.gui.dock.SplitDockStation;
import bibliothek.gui.dock.StackDockStation;
import bibliothek.gui.dock.station.split.SplitDockProperty;
import bibliothek.gui.dock.station.split.SplitDockTree;
import bibliothek.gui.dock.control.SingleParentRemover;
import bibliothek.gui.dock.event.DockFrontendAdapter;
import bibliothek.gui.dock.facile.action.ReplaceActionGuard;
import bibliothek.gui.dock.support.lookandfeel.ComponentCollector;
import bibliothek.gui.dock.support.lookandfeel.LookAndFeelList;
import bibliothek.notes.view.menu.ThemeMenu;
import net.sf.jasperreports.engine.JRException;
import platform.client.Main;
import platform.client.Log;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.interop.UserInfo;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.*;

public class Layout extends JFrame implements ComponentCollector {

    public Layout(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
        super();

        setIconImage(new ImageIcon(getClass().getResource("/platform/images/lsfusion.jpg")).getImage());

        drawCurrentUser(remoteNavigator);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setSize(1024, 768);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        ClientNavigator mainNavigator = new ClientNavigator(remoteNavigator) {

            public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException, JRException {
                Main.layout.defaultStation.drop(new ClientFormDockable(element.ID, this, false));
            }

            public void openRelevantForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException, JRException {
                Main.layout.defaultStation.drop(element.isPrintForm?new ReportDockable(element.ID, this, true):new ClientFormDockable(element.ID, this, true));
            }
        };

        initDockStations(mainNavigator);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we)
            {
                setVisible(false);
                try {
                    write();
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при сохранении расположения форм", e);
                }
            }
        });

    }

    public void drawCurrentUser(RemoteNavigatorInterface remoteNavigator) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(remoteNavigator.getCurrentUserInfoByteArray()));
        setTitle("LS Fusion - " + inputStream.readObject());
    }

    private DockFrontend frontend;
    private LookAndFeelList lookAndFeels;
    private ThemeMenu themes;

    public StackDockStation defaultStation;

    private void initDockStations(ClientNavigator mainNavigator) {

        frontend = new DockFrontend();
        DockController controller = frontend.getController();

        // дизайн
        // Look&Feel'ы
        lookAndFeels = LookAndFeelList.getDefaultList();
        // пометим что мы сами будем следить за изменение Layout'а
        lookAndFeels.addComponentCollector(this);
        // темы
        themes = new ThemeMenu(frontend);

        // делает чтобы не удалялась основная StackForm'а
        controller.setSingleParentRemover(new SingleParentRemover() {
            protected boolean shouldTest(DockStation dockStation) {
                return (dockStation!= defaultStation);
            }
        });

        // можно удалять ненужные контейнеры (кроме DefaultStation)
        controller.addActionGuard(new ReplaceActionGuard(controller) {
            public boolean react(Dockable dockable) {
                return dockable== defaultStation && super.react(dockable);
            }
        });
        // добавим закрытие форм
        controller.addActionGuard(new LayoutActionGuard(controller));

        // добавляем основную SplitDockStation

        SplitDockStation mainSplitStation = new SplitDockStation();
        add(mainSplitStation.getComponent(), BorderLayout.CENTER);
        frontend.addRoot(mainSplitStation, "Main");

        // добавляем со всех краев по FlapDockStation'у

        Map<FlapDockStation,String> flaps = new HashMap<FlapDockStation, String>();
        flaps.put(new FlapDockStation(),BorderLayout.NORTH);
        flaps.put(new FlapDockStation(),BorderLayout.EAST);
        flaps.put(new FlapDockStation(),BorderLayout.SOUTH);
        flaps.put(new FlapDockStation(),BorderLayout.WEST);

        for(Map.Entry<FlapDockStation,String> Flap : flaps.entrySet()) {
            add(Flap.getKey().getComponent(), Flap.getValue());
            frontend.addRoot(Flap.getKey(), "Flap" + Flap.getValue());
        }

        // Station куда будут кидаться все открываемые формы
        StackDockStation stackStation = new StackDockStation();
        frontend.add(stackStation, "Forms");
        frontend.setHideable(stackStation, false);

        defaultStation = stackStation;

        // Создаем все служебные Station
        NavigatorDockable mainNavigatorForm = new NavigatorDockable(mainNavigator, "Навигатор");
        frontend.add(mainNavigatorForm,"remoteNavigator");

        NavigatorDockable relevantFormNavigatorForm = new NavigatorDockable(mainNavigator.relevantFormNavigator, "Связанные формы");
        frontend.add(relevantFormNavigatorForm,"relevantFormNavigator");

        NavigatorDockable relevantClassNavigatorForm = new NavigatorDockable(mainNavigator.relevantClassNavigator, "Классовые формы");
        frontend.add(relevantClassNavigatorForm,"relevantClassNavigator");

        DefaultDockable logPanel = new DefaultDockable(Log.getPanel(), "Лог");
        frontend.add(logPanel, "Log");

/*        // сделаем чтобы Page'и шли без title'ов
        DockTitleFactory Factory = new NoStackTitleFactory(controller.getTheme().getTitleFactory(controller));
        controller.getDockTitleManager().registerClient(StackDockStation.TITLE_ID,Factory);
        controller.getDockTitleManager().registerClient(SplitDockStation.TITLE_ID,Factory);
        controller.getDockTitleManager().registerClient(FlapDockStation.WINDOW_TITLE_ID,Factory);
*/
        // здесь чтобы сама потом подцепила галочки панелей
        setupMenu();

        frontend.registerFactory(new ClientFormFactory(mainNavigator));
        try {
            read();
        } catch (IOException e) {

            // расположение по умолчанию
            SplitDockTree dockTree = new SplitDockTree();

            SplitDockTree.Key services = dockTree.vertical(dockTree.put(mainNavigatorForm),
                                         dockTree.vertical(dockTree.put(relevantFormNavigatorForm),
                                         dockTree.vertical(dockTree.put(relevantClassNavigatorForm),
                                                           dockTree.put(logPanel), 0.5), 0.3), 0.4);

            SplitDockTree.Key forms = dockTree.put(stackStation);

            dockTree.root(dockTree.horizontal(services, forms, 0.2));

            mainSplitStation.dropTree(dockTree);
        }

    }

    void write() throws IOException {
        FileOutputStream Source = new FileOutputStream("layout.txt");
        DataOutputStream out = new DataOutputStream(Source);

        lookAndFeels.write(out);
        themes.write(out);

        out.writeInt(getExtendedState());
        setExtendedState(NORMAL);
        out.writeInt(getX());
        out.writeInt(getY());
        out.writeInt(getWidth());
        out.writeInt(getHeight());

        frontend.write(out);

        Source.close();
    }

    static boolean readed = false;

    void read() throws IOException{
        FileInputStream source = new FileInputStream("layout.txt");
        DataInputStream in = new DataInputStream(source);

        lookAndFeels.read(in);
        themes.read(in);

        int State = in.readInt();
		setBounds(in.readInt(),in.readInt(),in.readInt(),in.readInt() );
		setExtendedState(State);

        readed = true;
        frontend.read(in);
        readed = false;

        source.close();
    }

    // настраивает меню
    void setupMenu() {
        JMenuBar Menubar = new JMenuBar();

        Menubar.add(createFileMenu());
        Menubar.add(createPanelsMenu());
        Menubar.add(createWindowMenu());
        Menubar.add(createWorkPlaceMenu());
        Menubar.add(createHelpMenu());

        setJMenuBar(Menubar);
    }

    // список компонентов которые следят за look&feel
    public Collection<Component> listComponents() {

        Set<Component> Result = new HashSet<Component>();
        for(Dockable Dockable : frontend.listDockables())
            Result.add(Dockable.getComponent());

        for(Dockable Dockable : frontend.getController().getRegister().listDockables())
            Result.add(Dockable.getComponent());

        Result.add(this);

        return Result;
    }

    JMenu createFileMenu() {
        // File
        JMenu Menu = new JMenu( "Файл" );
        JMenuItem OpenReport = new JMenuItem("Открыть отчет...");
        OpenReport.setToolTipText("Открывает ранее сохраненный отчет");
        final JFrame MainFrame = this;
        OpenReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent ae) {
                FileDialog Chooser = new FileDialog(MainFrame,"Отчет");
                Chooser.setFilenameFilter(new FilenameFilter(){
                    public boolean accept(File directory, String file) {
                        String filename = file.toUpperCase();
                        return filename.endsWith(".JRPRINT");
                    }
                });
                Chooser.setVisible(true);

                try {
                    defaultStation.drop(new ReportDockable(Chooser.getFile(),Chooser.getDirectory()));
                } catch (JRException e) {
                    throw new RuntimeException("Ошибка при открытии сохраненного отчета", e);
                }
            }
        });
        Menu.add(OpenReport);
        return Menu;
    }

    JMenu createPanelsMenu() {
        // панели считаем с Frontendа
        JMenu Menu = new JMenu("Панели");
		for(final Dockable Dockable : frontend.listDockables()){
            if(Dockable.asDockStation()==null && frontend.isHideable(Dockable)) {
                final JCheckBoxMenuItem Item = new JCheckBoxMenuItem(Dockable.getTitleText());
                Item.setSelected(true);
                Item.addActionListener(new ActionListener() {
	        		public void actionPerformed(ActionEvent e) {
        				if(Item.isSelected())
		        			frontend.show(Dockable);
        				else
        					frontend.hide(Dockable);
           			}
    		    });

        		frontend.addFrontendListener(new DockFrontendAdapter(){
    		        public void hidden(DockFrontend Frontend,Dockable Affected) {
	    		    	if(Affected==Dockable)
		    		    	Item.setSelected( false );
    			    }

    	    		public void shown(DockFrontend Frontend,Dockable Affected) {
	    	    		if(Affected==Dockable)
		    	    		Item.setSelected(true);
			        }
                });

                Menu.add(Item);
            }
        }
        return Menu;
    }

    JMenu createWindowMenu() {
        JMenu Menu = new JMenu( "Окно" );
		Menu.add(new LookAndFeelMenu(this, lookAndFeels));
        // темы делаем
        Menu.add(themes);
        return Menu;
    }

    private final Map<String,JRadioButtonMenuItem> WorkPlaces = new HashMap<String, JRadioButtonMenuItem>();
    private final JMenu WorkPlaceMenu = new JMenu("АРМ");
    private final Map<String,JMenuItem> RemoveWorkPlaces = new HashMap<String, JMenuItem>();
    private final JMenu RemoveWorkPlaceMenu = new JMenu("Удалить");

    JMenu createWorkPlaceMenu() {
        JMenuItem Save = new JMenuItem("Сохранить");
		Save.addActionListener( new ActionListener(){
			public void actionPerformed(ActionEvent e) {
                String Name = frontend.getCurrentSetting();
                if(Name==null)
                    Name = "АРМ " + (frontend.getSettings().size()+1);
				frontend.save(Name);
			}
		});
        WorkPlaceMenu.add(Save);

        final JFrame MainFrame = this;
        JMenuItem SaveAs = new JMenuItem("Сохранить как...");
        SaveAs.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                String Name = JOptionPane.showInputDialog(MainFrame,"Название АРМ :","АРМ "+(frontend.getSettings().size()+1));
                if(Name!=null)
                    frontend.save(Name);
            }
        });
        WorkPlaceMenu.add(SaveAs);
        WorkPlaceMenu.add(RemoveWorkPlaceMenu);

        frontend.addFrontendListener(new DockFrontendAdapter(){
            public void read(DockFrontend Frontend,String Name) {
                // считали св-во
                createWorkPlaceLoadItem(Name);
                if(Frontend.getCurrentSetting().equals(Name))
                    selectItem(Name);
            }

            void selectItem(String Name) {
                for(Map.Entry<String,JRadioButtonMenuItem> WorkPlace : WorkPlaces.entrySet())
                    WorkPlace.getValue().setSelected(false);
                createWorkPlaceLoadItem(Name).setSelected(true);
            }

            public void saved(DockFrontend Frontend,String Name) {
                // сохранили св-во
                createWorkPlaceLoadItem(Name);
                selectItem(Name);
            }

            public void loaded(DockFrontend Frontend,String Name) {
                selectItem(Name);
            }

            public void deleted(DockFrontend Frontend,String Name) {

                // нужно удалить
                WorkPlaceMenu.remove(WorkPlaces.get(Name));
                WorkPlaces.remove(Name);
                RemoveWorkPlaceMenu.remove(RemoveWorkPlaces.get(Name));
                RemoveWorkPlaces.remove(Name);
            }
        });

        return WorkPlaceMenu;
    }

	JRadioButtonMenuItem createWorkPlaceLoadItem(final String Name){
        JRadioButtonMenuItem Item = WorkPlaces.get(Name);
        if(Item==null) {
            Item = new JRadioButtonMenuItem(Name);
            Item.addActionListener( new ActionListener(){
		    	public void actionPerformed(ActionEvent e) {
		    		frontend.load(Name);
			    }
    		});
            if(WorkPlaces.size()==0)
                WorkPlaceMenu.addSeparator();
            WorkPlaces.put(Name,Item);
            WorkPlaceMenu.add(Item);

            JMenuItem RemoveItem = new JMenuItem(Name);
            RemoveItem.addActionListener( new ActionListener(){
		    	public void actionPerformed(ActionEvent e) {
		    		frontend.delete(Name);
			    }
    		});

            RemoveWorkPlaces.put(Name,RemoveItem);
            RemoveWorkPlaceMenu.add(RemoveItem);
        }
        return Item;
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
}

/*
class Log {

    void S
}

class Status {

}
*/