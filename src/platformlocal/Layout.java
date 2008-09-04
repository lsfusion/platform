package platformlocal;

import bibliothek.gui.*;
import bibliothek.gui.dock.*;
import bibliothek.gui.dock.event.DockFrontendAdapter;
import bibliothek.gui.dock.support.lookandfeel.LookAndFeelList;
import bibliothek.gui.dock.support.lookandfeel.ComponentCollector;
import bibliothek.gui.dock.title.*;
import bibliothek.gui.dock.themes.ThemeFactory;
import bibliothek.gui.dock.themes.BasicTheme;
import bibliothek.gui.dock.themes.nostack.NoStackTitleFactory;
import bibliothek.gui.dock.util.IconManager;
import bibliothek.gui.dock.util.Priority;
import bibliothek.gui.dock.facile.action.ReplaceActionGuard;
import bibliothek.gui.dock.action.actions.SimpleButtonAction;
import bibliothek.gui.dock.action.LocationHint;
import bibliothek.gui.dock.action.DefaultDockActionSource;
import bibliothek.gui.dock.action.ActionGuard;
import bibliothek.gui.dock.action.DockActionSource;
import bibliothek.gui.dock.layout.PredefinedDockSituation;
import bibliothek.gui.dock.layout.DockSituation;
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

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JRException;
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

        NavigatorDockable NavigatorForm = new NavigatorDockable(Navigator);
        // нужно обязательно до Drop чтобы появились крестики
        Frontend.add(NavigatorForm,"Navigator");

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

        Frontend.registerFactory(new ClientFormFactory(Navigator));
        try {
            read();
        } catch (IOException e) {
            SplitStation.drop(NavigatorForm);
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

        setVisible(true);
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

        JMenuItem SaveAs = new JMenuItem("Сохранить как...");
        SaveAs.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                String Name = "АРМ " + (Frontend.getSettings().size()+1);
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

    NavigatorDockable(RemoteNavigator Navigator) {
        super(new ClientNavigator(Navigator) {

            public void openForm(ClientNavigatorForm element) {
                try {
                    Main.Layout.DefaultStation.drop(new ClientFormDockable(element.ID, remoteNavigator));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        },"Navigator");
    }
}

// уничтожаемые формы
abstract class FormDockable extends DefaultDockable {

    FormDockable(String Caption) {super(Caption);}
    FormDockable(Component Component,String Caption) {super(Component,Caption);}

    // закрываются пользователем
    abstract void closed();
}

class ReportDockable extends FormDockable {

    ReportDockable(JasperPrint Print) {
        super(new JRViewer(Print),"Report");
    }

    // из файла
    ReportDockable(String FileName,String Directory) throws JRException {
        super(new JRViewer((JasperPrint)JRLoader.loadObject(Directory+FileName)),FileName);
    }

    // закрываются пользователем
    void closed() {
        // пока ничего не делаем
    }
}

class ClientFormDockable extends FormDockable {

    int FormID;
    RemoteNavigator Navigator;

    ClientFormDockable(int iFormID,RemoteNavigator iNavigator) throws SQLException {
        super("Form");
        FormID = iFormID;
        Navigator = iNavigator;
        setFactoryID(ClientFormFactory.FACTORY_ID);

        setFormID(iFormID);
    }

    Component ActiveComponent;

    void setFormID(int iFormID) throws SQLException {

        if(ActiveComponent!=null) remove(ActiveComponent);
        setTitleText(Navigator.getCaption(FormID));
        ActiveComponent = (new ClientForm(Navigator.CreateForm(FormID)));
        add(ActiveComponent);
    }

    // закрываются пользователем
    void closed() {
        // надо удалить RemoteForm
    }
}

class ClientFormFactory implements DockFactory<ClientFormDockable,Integer> {

    RemoteNavigator Navigator;
    ClientFormFactory(RemoteNavigator iNavigator) {
        Navigator = iNavigator;
    }

    public static final String FACTORY_ID = "clientforms";

    public String getID() {
        return FACTORY_ID;
    }

    public Integer getLayout(ClientFormDockable clientFormDockable, Map<Dockable, Integer> dockableIntegerMap) {
        return clientFormDockable.FormID;
    }

    public void setLayout(ClientFormDockable clientFormDockable, Integer integer, Map<Integer, Dockable> integerDockableMap) {
        setLayout(clientFormDockable,integer);
    }

    public void setLayout(ClientFormDockable clientFormDockable, Integer integer) {
        try {
            clientFormDockable.setFormID(integer);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public ClientFormDockable layout(Integer integer, Map<Integer, Dockable> integerDockableMap) {
        return layout(integer);
    }

    public ClientFormDockable layout(Integer integer) {
        try {
            return new ClientFormDockable(integer,Navigator);
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