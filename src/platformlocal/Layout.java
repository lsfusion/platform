package platformlocal;

import bibliothek.gui.*;
import bibliothek.gui.dock.*;
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
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;
import java.io.*;

import net.sf.jasperreports.engine.JasperPrint;
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

    Layout(RemoteNavigator Navigator) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setVisible(true);

        Frontend = new DockFrontend();
        DockController Controller = Frontend.getController();

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
        SplitStation.drop(NavigatorForm);
        // нужно включить в FrontEnd чтобы была predefined и новые формы могли бы туда же попадать
        Frontend.add(StackStation,"Stack");
        Frontend.setHideable(StackStation,false);
        SplitStation.drop(StackStation);

/*        // сделаем чтобы Page'и шли без title'ов
        DockTitleFactory Factory = new NoStackTitleFactory(Controller.getTheme().getTitleFactory(Controller));
        Controller.getDockTitleManager().registerClient(StackDockStation.TITLE_ID,Factory);
        Controller.getDockTitleManager().registerClient(SplitDockStation.TITLE_ID,Factory);
        Controller.getDockTitleManager().registerClient(FlapDockStation.WINDOW_TITLE_ID,Factory);
*/
        Frontend.registerFactory(new ClientFormFactory(Navigator));
        try {
            FileInputStream Source = new FileInputStream("layout.txt");
            DataInputStream in = new DataInputStream(Source);

            Frontend.read(in);
            Source.close();
        } catch (IOException e) {
            Loaded = false;
        }

        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                try {
                    FileOutputStream Source = new FileOutputStream("layout.txt");
                    DataOutputStream out = new DataOutputStream(Source);
                    Frontend.write(out);
                    Source.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        setupMenu();
    }

    // для теста
    boolean Loaded = true;

    // настраивает меню
    void setupMenu() {
        JMenuBar Menubar = new JMenuBar();
        LookAndFeelList LookAndFeels = LookAndFeelList.getDefaultList();
        // пометим что мы сами будем следить за изменение Layout'а
        LookAndFeels.addComponentCollector(this);

        JMenu WindowMenu = new JMenu( "Window" );
		WindowMenu.add(new LookAndFeelMenu(this,LookAndFeels));

        // темы делаем
/*        JMenu ThemeMenu = new JMenu("Theme");
        for(ThemeFactory Factory : DockUI.getDefaultDockUI().getThemes()) {
            JMenuItem Item = new JMenuItem(Factory.getName());
            Item.setToolTipText(Factory.getDescription());
            final DockTheme Theme = Factory.create();
            Item.addActionListener( new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    Frontend.getController().setTheme(Theme);
                }
            });
            ThemeMenu.add(Item);
        }*/
        WindowMenu.add(new ThemeMenu(Frontend));
		Menubar.add(WindowMenu);

        setJMenuBar(Menubar);
    }

    // список компонентов которые следят за look&feel
    public Collection<Component> listComponents() {
        Collection<Component> Result = new ArrayList();
        Result.add(this);
        return Result;
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
        super(new ClientNavigator(Navigator),"Navigator");
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