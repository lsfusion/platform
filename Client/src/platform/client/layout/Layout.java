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

    public StackDockStation defaultStation;

    Map<String,DockStation> rootStations = new HashMap<String, DockStation>();

    DockFrontend frontend;
    LookAndFeelList lookAndFeels;
    ThemeMenu themes;

    public Layout(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
        super();

        setIconImage(new ImageIcon(getClass().getResource("lsfusion.gif")).getImage());

        UserInfo userInfo = (UserInfo) new ObjectInputStream(new ByteArrayInputStream(remoteNavigator.getCurrentUserInfoByteArray())).readObject();
        setTitle("LS Fusion - " + userInfo.firstName + " " + userInfo.lastName);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setSize(800, 600);
        frontend = new DockFrontend();
        DockController Controller = frontend.getController();

        // дизайн
        // Look&Feel'ы
        lookAndFeels = LookAndFeelList.getDefaultList();
        // пометим что мы сами будем следить за изменение Layout'а
        lookAndFeels.addComponentCollector(this);
        // темы
        themes = new ThemeMenu(frontend);

        // делает чтобы не удалялась основная StackForm'а
        Controller.setSingleParentRemover(new SingleParentRemover() {
            protected boolean shouldTest(DockStation dockStation) {
                return (dockStation!= defaultStation);
            }
        });

        // можно удалять ненужные контейнеры (кроме DefaultStation)
        Controller.addActionGuard(new ReplaceActionGuard(Controller) {
            public boolean react(Dockable dockable) {
                return dockable== defaultStation && super.react(dockable);
            }
        });
        // добавим закрытие форм
        Controller.addActionGuard(new LayoutActionGuard(Controller));

        SplitDockStation SplitStation = new SplitDockStation();

        Map<FlapDockStation,String> Flaps = new HashMap<FlapDockStation, String>();
        Flaps.put(new FlapDockStation(),BorderLayout.NORTH);
        Flaps.put(new FlapDockStation(),BorderLayout.EAST);
        Flaps.put(new FlapDockStation(),BorderLayout.SOUTH);
        Flaps.put(new FlapDockStation(),BorderLayout.WEST);

        StackDockStation StackStation = new StackDockStation();
        // the station has to be registered
        add(SplitStation, BorderLayout.CENTER);

        for(Map.Entry<FlapDockStation,String> Flap : Flaps.entrySet()) {
            add(Flap.getKey().getComponent(),Flap.getValue());
            frontend.addRoot(Flap.getKey(),Flap.getValue()+"Flap");
        }

        frontend.addRoot(SplitStation,"Split");

        defaultStation = StackStation;

        ClientNavigator mainNavigator = new ClientNavigator(remoteNavigator) {

            public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException, JRException {
                Main.layout.defaultStation.drop(new ClientFormDockable(element.ID, this, false));
            }

            public void openRelevantForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException, JRException {
                if (element.isPrintForm)
                    Main.layout.defaultStation.drop(new ReportDockable(element.ID, this, true));
                else
                    Main.layout.defaultStation.drop(new ClientFormDockable(element.ID, this, true));
            }
        };

        NavigatorDockable mainNavigatorForm = new NavigatorDockable(mainNavigator, "Навигатор");
        // нужно обязательно до Drop чтобы появились крестики
        frontend.add(mainNavigatorForm,"remoteNavigator");

        NavigatorDockable relevantFormNavigatorForm = new NavigatorDockable(mainNavigator.relevantFormNavigator, "Связанные формы");
        frontend.add(relevantFormNavigatorForm,"relevantFormNavigator");

        NavigatorDockable relevantClassNavigatorForm = new NavigatorDockable(mainNavigator.relevantClassNavigator, "Классовые формы");
        frontend.add(relevantClassNavigatorForm,"relevantClassNavigator");

        DefaultDockable logPanel = new DefaultDockable(Log.getPanel(), "Log");
        frontend.add(logPanel, "Log");

        // нужно включить в FrontEnd чтобы была predefined и новые формы могли бы туда же попадать
        frontend.add(StackStation,"Stack");
        frontend.setHideable(StackStation,false);

/*        // сделаем чтобы Page'и шли без title'ов
        DockTitleFactory Factory = new NoStackTitleFactory(Controller.getTheme().getTitleFactory(Controller));
        Controller.getDockTitleManager().registerClient(StackDockStation.TITLE_ID,Factory);
        Controller.getDockTitleManager().registerClient(SplitDockStation.TITLE_ID,Factory);
        Controller.getDockTitleManager().registerClient(FlapDockStation.WINDOW_TITLE_ID,Factory);
*/
        // здесь чтобы сама потом подцепила галочки панелей
        setupMenu();

        frontend.registerFactory(new ClientFormFactory(mainNavigator));
        try {
            read();
        } catch (IOException e) {
            SplitStation.drop(mainNavigatorForm);
            SplitStation.drop(relevantFormNavigatorForm);
            SplitStation.drop(relevantClassNavigatorForm);
            SplitStation.drop(logPanel);
            SplitStation.drop(StackStation);
        }

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e)
            {
                setVisible(false);
                try {
                    write();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

/*        String[] columnNames = {"First Name",
                                "Last Name",
                                "Sport",
                                "# of Years",
                                "Vegetarian"};

        Object[][] data = {
            {"Mary", "Campione",
             "Snowboarding", new Integer(5), new Boolean(false)},
            {"Alison", "Huml",
             "Rowing", new Integer(3), new Boolean(true)},
            {"Kathy", "Walrath",
             "Knitting", new Integer(2), new Boolean(false)},
            {"Sharon", "Zakhour",
             "Speed reading", new Integer(20), new Boolean(true)},
            {"Philip", "Milne",
             "Pool", new Integer(10), new Boolean(false)}
        };

        final JTable table = new JTable(data, columnNames);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);

        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        final JButton button = new JButton("Test");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                JButton butt1 = new JButton();
                JButton butt2 = new JButton();
                panel.add(butt1);
                butt1.setNextFocusableComponent(table);
                table.setNextFocusableComponent(button);
//                butt1.setNextFocusableComponent(null);
                panel.remove(butt1);
//                table.setNextFocusableComponent(table);
            }
        });
        
        panel.add(table);
        panel.add(button);

        DefaultDockable dock = new DefaultDockable(panel, "Table");

        setVisible(true);

        SplitStation.drop(dock);

        for (DockableDisplayer displayer : SplitStation.getDisplayers()) {
            if (displayer instanceof BasicDockableDisplayer) {
                ((BasicDockableDisplayer)displayer).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
            }
        }
        Container cont = table.getFocusCycleRootAncestor();
        cont.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());

        panel.setFocusCycleRoot(true);
//        panel.setFocusTraversalPolicyProvider(true);
        panel.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());

//        Container cont = table.getFocusCycleRootAncestor();
        System.out.println(cont);
        System.out.println(cont.getFocusTraversalPolicy());
        table.setNextFocusableComponent(button);
        System.out.println(cont.getFocusTraversalPolicy()); */
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

    void read() throws IOException{
        FileInputStream Source = new FileInputStream("layout.txt");
        DataInputStream in = new DataInputStream(Source);

        lookAndFeels.read(in);
        themes.read(in);

        int State = in.readInt();
		setBounds(in.readInt(),in.readInt(),in.readInt(),in.readInt() );
		setExtendedState(State);

        frontend.read(in);

        Source.close();
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
            public void actionPerformed(ActionEvent e) {
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
                } catch (JRException e1) {
                    e1.printStackTrace();
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

    Map<String,JRadioButtonMenuItem> WorkPlaces = new HashMap<String, JRadioButtonMenuItem>();
    JMenu WorkPlaceMenu = new JMenu("АРМ");
    Map<String,JMenuItem> RemoveWorkPlaces = new HashMap<String, JMenuItem>();
    JMenu RemoveWorkPlaceMenu = new JMenu("Удалить");

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