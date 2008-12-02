package platformlocal;

import bibliothek.gui.*;
import bibliothek.gui.dock.*;
import bibliothek.gui.dock.event.DockFrontendAdapter;
import bibliothek.gui.dock.support.lookandfeel.LookAndFeelList;
import bibliothek.gui.dock.support.lookandfeel.ComponentCollector;
import bibliothek.gui.dock.facile.action.ReplaceActionGuard;
import bibliothek.gui.dock.action.actions.SimpleButtonAction;
import bibliothek.gui.dock.action.LocationHint;
import bibliothek.gui.dock.action.DefaultDockActionSource;
import bibliothek.gui.dock.action.ActionGuard;
import bibliothek.gui.dock.action.DockActionSource;
import bibliothek.gui.dock.control.SingleParentRemover;
import bibliothek.util.xml.XElement;
import bibliothek.notes.view.menu.ThemeMenu;
import bibliothek.demonstration.util.LookAndFeelMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.*;
import java.io.*;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;

/**
 * Created by IntelliJ IDEA.
 * User: ME2
 * Date: 07.08.2008
 * Time: 14:45:12
 * To change this template use File | Settings | File Templates.
 */
class Layout extends JFrame implements ComponentCollector {

    StackDockStation DefaultStation;

    Map<String,DockStation> RootStations = new HashMap();

    DockFrontend Frontend;
    LookAndFeelList LookAndFeels;
    ThemeMenu Themes;

    Layout(RemoteNavigator Navigator) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        Frontend = new DockFrontend();
        DockController Controller = Frontend.getController();

        // дизайн
        // Look&Feel'ы
        LookAndFeels = LookAndFeelList.getDefaultList();
        // пометим что мы сами будем следить за изменение Layout'а
        LookAndFeels.addComponentCollector(this);
        // темы
        Themes = new ThemeMenu(Frontend);

        // делает чтобы не удалялась основная StackForm'а
        Controller.setSingleParentRemover(new SingleParentRemover() {
            protected boolean shouldTest(DockStation dockStation) {
                return (dockStation!=DefaultStation);
            }
        });

        // можно удалять ненужные контейнеры (кроме DefaultStation)
        Controller.addActionGuard(new ReplaceActionGuard(Controller) {
            public boolean react(Dockable dockable) {
                if(dockable==DefaultStation) return false;
                return super.react(dockable);
            }
        });
        // добавим закрытие форм
        Controller.addActionGuard(new LayoutActionGuard(Controller));

        SplitDockStation SplitStation = new SplitDockStation();

        Map<FlapDockStation,String> Flaps = new HashMap();
        Flaps.put(new FlapDockStation(),BorderLayout.NORTH);
        Flaps.put(new FlapDockStation(),BorderLayout.EAST);
        Flaps.put(new FlapDockStation(),BorderLayout.SOUTH);
        Flaps.put(new FlapDockStation(),BorderLayout.WEST);

        StackDockStation StackStation = new StackDockStation();
        // the station has to be registered
        add(SplitStation, BorderLayout.CENTER);

        for(Map.Entry<FlapDockStation,String> Flap : Flaps.entrySet()) {
            add(Flap.getKey().getComponent(),Flap.getValue());
            Frontend.addRoot(Flap.getKey(),Flap.getValue()+"Flap");
        }

        Frontend.addRoot(SplitStation,"Split");

        DefaultStation = StackStation;

        ClientNavigator mainNavigator = new ClientNavigator(Navigator) {

            public void openForm(ClientNavigatorForm element) {
                try {
                    Main.Layout.DefaultStation.drop(new ClientFormDockable(element.ID, this, false));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            public void openRelevantForm(ClientNavigatorForm element) {
                try {
                    if (element.isPrintForm)
                        Main.Layout.DefaultStation.drop(new ReportDockable(element.ID, this, true));
                    else
                        Main.Layout.DefaultStation.drop(new ClientFormDockable(element.ID, this, true));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        };

        NavigatorDockable mainNavigatorForm = new NavigatorDockable(mainNavigator, "Навигатор");
        // нужно обязательно до Drop чтобы появились крестики
        Frontend.add(mainNavigatorForm,"Navigator");

        NavigatorDockable relevantFormNavigatorForm = new NavigatorDockable(mainNavigator.relevantFormNavigator, "Связанные формы");
        Frontend.add(relevantFormNavigatorForm,"relevantFormNavigator");

        NavigatorDockable relevantClassNavigatorForm = new NavigatorDockable(mainNavigator.relevantClassNavigator, "Классовые формы");
        Frontend.add(relevantClassNavigatorForm,"relevantClassNavigator");

        DefaultDockable logPanel = new DefaultDockable(Log.getPanel(), "Log");
        Frontend.add(logPanel, "Log");

        // нужно включить в FrontEnd чтобы была predefined и новые формы могли бы туда же попадать
        Frontend.add(StackStation,"Stack");
        Frontend.setHideable(StackStation,false);

/*        // сделаем чтобы Page'и шли без title'ов
        DockTitleFactory Factory = new NoStackTitleFactory(Controller.getTheme().getTitleFactory(Controller));
        Controller.getDockTitleManager().registerClient(StackDockStation.TITLE_ID,Factory);
        Controller.getDockTitleManager().registerClient(SplitDockStation.TITLE_ID,Factory);
        Controller.getDockTitleManager().registerClient(FlapDockStation.WINDOW_TITLE_ID,Factory);
*/
        // здесь чтобы сама потом подцепила галочки панелей
        setupMenu();

        Frontend.registerFactory(new ClientFormFactory(mainNavigator));
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

        LookAndFeels.write(out);
        Themes.write(out);

        out.writeInt(getExtendedState());
        setExtendedState(NORMAL);
        out.writeInt(getX());
        out.writeInt(getY());
        out.writeInt(getWidth());
        out.writeInt(getHeight());

        Frontend.write(out);

        Source.close();
    }

    void read() throws IOException{
        FileInputStream Source = new FileInputStream("layout.txt");
        DataInputStream in = new DataInputStream(Source);

        LookAndFeels.read(in);
        Themes.read(in);

        int State = in.readInt();
		setBounds(in.readInt(),in.readInt(),in.readInt(),in.readInt() );
		setExtendedState(State);

        Frontend.read(in);

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
        for(Dockable Dockable : Frontend.listDockables())
            Result.add(Dockable.getComponent());

        for(Dockable Dockable : Frontend.getController().getRegister().listDockables())
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
                    DefaultStation.drop(new ReportDockable(Chooser.getFile(),Chooser.getDirectory()));
                } catch (JRException e1) {
                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        });
        Menu.add(OpenReport);
        return Menu;
    }

    JMenu createPanelsMenu() {
        // панели считаем с Frontendа
        JMenu Menu = new JMenu("Панели");
		for(final Dockable Dockable : Frontend.listDockables()){
            if(Dockable.asDockStation()==null && Frontend.isHideable(Dockable)) {
                final JCheckBoxMenuItem Item = new JCheckBoxMenuItem(Dockable.getTitleText());
                Item.setSelected(true);
                Item.addActionListener(new ActionListener() {
	        		public void actionPerformed(ActionEvent e) {
        				if(Item.isSelected())
		        			Frontend.show(Dockable);
        				else
        					Frontend.hide(Dockable);
           			}
    		    });

        		Frontend.addFrontendListener(new DockFrontendAdapter(){
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
		Menu.add(new LookAndFeelMenu(this,LookAndFeels));
        // темы делаем
        Menu.add(Themes);
        return Menu;
    }

    Map<String,JRadioButtonMenuItem> WorkPlaces = new HashMap();
    JMenu WorkPlaceMenu = new JMenu("АРМ");
    Map<String,JMenuItem> RemoveWorkPlaces = new HashMap();
    JMenu RemoveWorkPlaceMenu = new JMenu("Удалить");

    JMenu createWorkPlaceMenu() {
        JMenuItem Save = new JMenuItem("Сохранить");
		Save.addActionListener( new ActionListener(){
			public void actionPerformed(ActionEvent e) {
                String Name = Frontend.getCurrentSetting();
                if(Name==null)
                    Name = "АРМ " + (Frontend.getSettings().size()+1);
				Frontend.save(Name);
			}
		});
        WorkPlaceMenu.add(Save);

        final JFrame MainFrame = this;
        JMenuItem SaveAs = new JMenuItem("Сохранить как...");
        SaveAs.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                String Name = JOptionPane.showInputDialog(MainFrame,"Название АРМ :","АРМ "+(Frontend.getSettings().size()+1));
                if(Name!=null)
                    Frontend.save(Name);
            }
        });
        WorkPlaceMenu.add(SaveAs);
        WorkPlaceMenu.add(RemoveWorkPlaceMenu);

        Frontend.addFrontendListener(new DockFrontendAdapter(){
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
		    		Frontend.load(Name);
			    }
    		});
            if(WorkPlaces.size()==0)
                WorkPlaceMenu.addSeparator();
            WorkPlaces.put(Name,Item);
            WorkPlaceMenu.add(Item);

            JMenuItem RemoveItem = new JMenuItem(Name);
            RemoveItem.addActionListener( new ActionListener(){
		    	public void actionPerformed(ActionEvent e) {
		    		Frontend.delete(Name);
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

class CloseAction extends SimpleButtonAction {

    CloseAction(DockController Controller) {
        setText("Close");
        setIcon(Controller.getIcons().getIcon("close"));
    }

    public void action(Dockable Form) {
        super.action(Form);
        DockStation Parent = Form.getDockParent();
        if(Parent!=null)
            Parent.drag(Form);

        // говорим о том что закрылись
        ((FormDockable)Form).closed();
    }
}

// подкидывает действия стандартные типа закрытия
class LayoutActionGuard implements ActionGuard {

    DefaultDockActionSource Source;

    LayoutActionGuard(DockController Controller) {
        Source = new DefaultDockActionSource(
                new LocationHint( LocationHint.ACTION_GUARD, LocationHint.RIGHT_OF_ALL));
        Source.add(new CloseAction(Controller));
    }

    public boolean react(Dockable dockable) {
        // заинтересованы в своих
        return (dockable instanceof FormDockable);
    }

    public DockActionSource getSource(Dockable dockable) {
        return Source;
    }
}

class NavigatorDockable extends DefaultDockable {

    NavigatorDockable(AbstractNavigator navigator, String caption) {
        super(navigator,caption);

    }
}

// уничтожаемые формы
abstract class FormDockable extends DefaultDockable {

    int formID;

    FormDockable(int iformID, ClientNavigator navigator, boolean currentSession) throws SQLException {
        this(iformID);

        createActiveComponent(navigator, currentSession);
    }

    FormDockable(int iformID, ClientNavigator navigator, RemoteForm remoteForm) throws SQLException {
        this(iformID);

        createActiveComponent(navigator, remoteForm);
    }

    FormDockable(int iformID) {
        formID = iformID;
        setFactoryID(ClientFormFactory.FACTORY_ID);
    }

    void createActiveComponent(ClientNavigator navigator, boolean currentSession) throws SQLException {
        createActiveComponent(navigator, navigator.remoteNavigator.CreateForm(formID, currentSession));
    }

    void createActiveComponent(ClientNavigator navigator, RemoteForm remoteForm) {
        setActiveComponent(getActiveComponent(navigator, remoteForm), navigator.remoteNavigator.getCaption(formID));
    }

    void setActiveComponent(Component comp, String caption) {

        setTitleText(caption);
        add(comp);
    }

    Component getActiveComponent(ClientNavigator navigator, RemoteForm remoteForm) { return null; }

//    FormDockable(String Caption) {super(Caption);}
//    FormDockable(Component Component,String Caption) {super(Component,Caption);}

    // закрываются пользователем
    abstract void closed();

}

class ReportDockable extends FormDockable {

    public ReportDockable(int iformID, ClientNavigator navigator, boolean currentSession) throws SQLException {
        super(iformID, navigator, currentSession);
    }

    public ReportDockable(int iformID, ClientNavigator navigator, RemoteForm remoteForm) throws SQLException {
        super(iformID, navigator, remoteForm);
    }

    // из файла
    ReportDockable(String FileName,String Directory) throws JRException {
        super(0);

        setActiveComponent(new JRViewer((JasperPrint)JRLoader.loadObject(Directory+FileName)),FileName);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteForm remoteForm) {

        JasperDesign design = ByteArraySerializer.deserializeReportDesign(remoteForm.getReportDesignByteArray());
        try {

            JasperReport report = JasperCompileManager.compileReport(design);
            JasperPrint print = JasperFillManager.fillReport(report,new HashMap(),ByteArraySerializer.deserializeReportData(remoteForm.getReportDataByteArray()));
            return new JRViewer(print);

        } catch (JRException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // закрываются пользователем
    void closed() {
        // пока ничего не делаем
    }
}

class ClientFormDockable extends FormDockable {

    ClientFormDockable(int iformID, ClientNavigator inavigator, boolean currentSession) throws SQLException {
        super(iformID, inavigator, currentSession);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteForm remoteForm) {
        return new ClientForm(remoteForm, navigator);
    }

    // закрываются пользователем
    void closed() {
        // надо удалить RemoteForm
    }
}

class ClientFormFactory implements DockFactory<FormDockable,Integer> {

    ClientNavigator navigator;
    ClientFormFactory(ClientNavigator inavigator) {
        navigator = inavigator;
    }

    public static final String FACTORY_ID = "clientforms";

    public String getID() {
        return FACTORY_ID;
    }

    public Integer getLayout(FormDockable formDockable, Map<Dockable, Integer> dockableIntegerMap) {
        return formDockable.formID;
    }

    public void setLayout(FormDockable formDockable, Integer integer, Map<Integer, Dockable> integerDockableMap) {
        setLayout(formDockable,integer);
    }

    public void setLayout(FormDockable formDockable, Integer integer) {
/*        try {
            clientFormDockable.setFormID(integer);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
    }

    public ClientFormDockable layout(Integer integer, Map<Integer, Dockable> integerDockableMap) {
        return layout(integer);
    }

    public ClientFormDockable layout(Integer integer) {
        try {
            return new ClientFormDockable(integer, navigator, false);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return null;
    }

    public void write(Integer integer, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(integer);
    }

    public void write(Integer integer, XElement xElement) {
        xElement.addInt("FormID",integer);
    }

    public Integer read(DataInputStream dataInputStream) throws IOException {
        return dataInputStream.readInt();
    }

    public Integer read(XElement xElement) {
        return xElement.getInt("FormID");
    }
}

/*
class Log {

    void S
}

class Status {

}
*/