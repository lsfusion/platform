package platformlocal;

import bibliothek.gui.DockStation;
import bibliothek.gui.DockController;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.*;
import bibliothek.gui.dock.layout.PredefinedDockSituation;
import bibliothek.gui.dock.control.SingleParentRemover;
import bibliothek.util.xml.XElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
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
class Layout extends JFrame implements WindowListener {

    DockStation DefaultStation;

    Map<String,DockStation> RootStations = new HashMap();

    PredefinedDockSituation Situation;

    Layout(RemoteNavigator Navigator) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setVisible(true);

        // the controller manages all operations
        DockController Controller = new DockController();

        // Add an action "replace station by child" to the controller.
        // This action allows to remove unnecessary stations by the user.
//            Controller.addActionGuard(new ReplaceActionGuard(Controller));

        // a station that shows some panels
        SplitDockStation SplitStation = new SplitDockStation();
        FlapDockStation FlapStation = new FlapDockStation();
        StackDockStation StackStation = new StackDockStation();

//        ScreenStation = new ScreenDockStation(MainFrame);

        // the station has to be registered
        add(SplitStation, BorderLayout.CENTER);
//            MainFrame.add(StackStation.getStackComponent(),BorderLayout.CENTER);
        add(FlapStation.getComponent(),BorderLayout.NORTH);
//            SplitDockStation DefaultSplitStation = new SplitDockStation();

        Controller.add(FlapStation);
//        Controller.add(ScreenStation);
//            Frontend.add(StackStation);
        Controller.add(SplitStation);
//            Frontend.setDefaultStation(SplitStation);

        RootStations.put("Flap",FlapStation);
        RootStations.put("Split",SplitStation);

        DefaultStation = StackStation;
        // делает чтобы не удалялась основная StackForm'а
        Controller.setSingleParentRemover(new SingleParentRemover() {
            protected boolean shouldTest(DockStation dockStation) {
                return (dockStation!=DefaultStation);
            }
        });

        SplitStation.drop(StackStation);

        PredefinedDockSituation Situation = new PredefinedDockSituation();
        Situation.add(new ClientFormFactory(Navigator));

        try {
            FileInputStream Source = new FileInputStream("layout.txt");
            DataInputStream in = new DataInputStream(Source);

            Situation.read(in);
            Source.close();
        } catch (IOException e) {
            Loaded = false;
        }

    }

    // для теста
    boolean Loaded = true;

    public void windowOpened(WindowEvent e) {
    }
    public void windowClosing(WindowEvent e) {
    }
    public void windowClosed(WindowEvent e) {
        try {
            FileOutputStream Source = new FileOutputStream("layout.txt");
            DataOutputStream out = new DataOutputStream(Source);
            Situation.write(RootStations,out);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    public void windowIconified(WindowEvent e) {
    }
    public void windowDeiconified(WindowEvent e) {
    }
    public void windowActivated(WindowEvent e) {
    }
    public void windowDeactivated(WindowEvent e) {
    }
}

class ClientFormDockable extends DefaultDockable {

    int FormID;
    RemoteNavigator Navigator;

    ClientFormDockable(int iFormID,RemoteNavigator iNavigator) throws SQLException {
        super("Form");
        Navigator = iNavigator;
        setFactoryID(ClientFormFactory.FACTORY_ID);

        setFormID(iFormID);
    }

    Component ActiveComponent;

    void setFormID(int iFormID) throws SQLException {

        if(ActiveComponent!=null) remove(ActiveComponent);
        ActiveComponent = (new ClientForm(Navigator.CreateForm(FormID))).getContentPane();
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