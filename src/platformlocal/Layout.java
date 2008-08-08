package platformlocal;

import bibliothek.gui.DockStation;
import bibliothek.gui.DockController;
import bibliothek.gui.Dockable;
import bibliothek.gui.DockFrontend;
import bibliothek.gui.dock.*;
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: ME2
 * Date: 07.08.2008
 * Time: 14:45:12
 * To change this template use File | Settings | File Templates.
 */
class Layout extends JFrame {

    DockStation DefaultStation;

    Map<String,DockStation> RootStations = new HashMap();

    PredefinedDockSituation Situation;

    Layout(RemoteNavigator Navigator) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setVisible(true);

        DockController Controller = new DockController();

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

        SplitDockStation SplitStation = new SplitDockStation();
        FlapDockStation FlapStation = new FlapDockStation();
        StackDockStation StackStation = new StackDockStation();
        // the station has to be registered
        add(SplitStation, BorderLayout.CENTER);
        add(FlapStation.getComponent(),BorderLayout.NORTH);
        Controller.add(FlapStation);
        Controller.add(SplitStation);

        DefaultStation = StackStation;
        NavigatorDockable NavigatorForm = new NavigatorDockable(Navigator);
        SplitStation.drop(NavigatorForm);
        SplitStation.drop(StackStation);

        RootStations.put("Flap",FlapStation);
        RootStations.put("Split",SplitStation);

        Situation = new PredefinedDockSituation();
        Situation.add(new ClientFormFactory(Navigator));

        Situation.put("Flap",FlapStation);
        Situation.put("Split",SplitStation);
        Situation.put("Stack",StackStation);
        Situation.put("Navigator",NavigatorForm);


        try {
            FileInputStream Source = new FileInputStream("layout.txt");
            DataInputStream in = new DataInputStream(Source);

            Situation.read(in);
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
                    Situation.write(RootStations,out);
                    Source.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // создадим действие закрытия Dockable'а
//        CloseAction Close = new CloseAction();
//        DefaultDockActionSource Source = new DefaultDockActionSource(
//                new LocationHint(LocationHint.DOCKABLE,LocationHint.RIGHT_OF_ALL));
//        Source.add(Close);

    }

    // для теста
    boolean Loaded = true;

}

class CloseAction extends SimpleButtonAction {

    CloseAction() {
        setText("Close");
        setIcon(new ImageIcon("close.png"));
    }

    public void action(Dockable Form) {
        super.action(Form);
        DockStation Parent = Form.getDockParent();
        if(Parent!=null)
            Parent.drag(Form);
    }
}

// подкидывает действия стандартные типа закрытия
class LayoutActionGuard implements ActionGuard {

    public boolean react(Dockable dockable) {
        // заинтересованы в своих
        return (dockable instanceof FormDockable);
    }

    public DockActionSource getSource(Dockable dockable) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

class FormDockable extends DefaultDockable {

    FormDockable(String Caption) {super(Caption);}
    FormDockable(Component Component,String Caption) {super(Component,Caption);}
}

class NavigatorDockable extends FormDockable {

    NavigatorDockable(RemoteNavigator Navigator) {
        super(new ClientNavigator(Navigator),"Navigator");
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
        ActiveComponent = (new ClientForm(Navigator.CreateForm(FormID)));
        add(ActiveComponent);
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