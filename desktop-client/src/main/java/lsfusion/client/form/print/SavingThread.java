package lsfusion.client.form.print;

import lsfusion.interop.action.ReportPath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

public class SavingThread extends Thread {
    private List<ReportPath> reportPathList;
    public SavingThread(List<ReportPath> reportPathList) {
        setDaemon(true);
        this.reportPathList = reportPathList;
    }

    @Override
    public void run() {
        while (true) {
            try {
                for (ReportPath reportPath : reportPathList) {
                    File original = new File(reportPath.customPath);
                    File copy = new File(reportPath.targetPath);
                    if (copy.lastModified() < original.lastModified()) {
                        FileChannel source = new FileInputStream(reportPath.customPath).getChannel();
                        FileChannel destination = new FileOutputStream(reportPath.targetPath).getChannel();
                        source.transferTo(0, source.size(), destination);
                        destination.close();
                        source.close();
                    }
                }
                Thread.sleep(3000);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
