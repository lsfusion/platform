package platform.fullclient;

import jasperapi.ReportGenerator;
import platform.client.Main;
import platform.client.MainFrame;
import platform.fullclient.layout.DockableMainFrame;
import platform.interop.ServerInfo;
import platform.interop.form.ReportGenerationData;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

public class FullMain extends Main {

    public static void main(final String[] args) {
        start(args, new ModuleFactory() {
            public MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws IOException {
                return new DockableMainFrame(remoteNavigator);
            }

            public void openInExcel(ReportGenerationData generationData) {
                ReportGenerator.exportToExcelAndOpen(generationData, timeZone);
            }

            public boolean isFull() {
                return true;
            }

            public SwingWorker<List<ServerInfo>, ServerInfo> getServerHostEnumerator(MutableComboBoxModel serverHostModel, String waitMessage) {
                return new ServerHostEnumerator(serverHostModel, waitMessage);
            }
        });
    }

}
