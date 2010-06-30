package platform.client;

import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.form.RemoteFormInterface;
import platform.client.layout.DockingMainFrame;
import platform.client.layout.ReportDockable;

import java.io.IOException;

public class FullMain extends Main {

    public static void main(final String[] args) {
        start(args, new ModuleFactory() {
            public MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
                return new DockingMainFrame(remoteNavigator);
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
