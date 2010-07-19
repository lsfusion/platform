package platform.fullclient;

import platform.fullclient.layout.DockableMainFrame;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.form.RemoteFormInterface;
import platform.fullclient.layout.ReportDockable;
import platform.client.Main;
import platform.client.MainFrame;

import java.io.IOException;

public class FullMain extends Main {

    public static void main(final String[] args) {
        start(args, new ModuleFactory() {
            public MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
                return new DockableMainFrame(remoteNavigator);
            }

            public void runExcel(RemoteFormInterface remoteForm) {
                ReportDockable.exportToExcel(remoteForm);
            }

            public boolean isFull() {
                return true;
            }
        });
    }

}
