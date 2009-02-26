package platform.client.layout;

import bibliothek.gui.Dockable;
import bibliothek.gui.dock.DockFactory;
import bibliothek.util.xml.XElement;
import platform.client.navigator.ClientNavigator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

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
