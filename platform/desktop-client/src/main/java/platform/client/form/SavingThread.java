package platform.client.form;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Set;

public class SavingThread extends Thread {
    private Map<String, String> pathMap;
    public SavingThread(Map<String, String> pathMap) {
        setDaemon(true);
        this.pathMap = pathMap;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Set<Map.Entry<String, String>> entrySet = pathMap.entrySet();
                for (Map.Entry<String, String> entry : entrySet) {
                    File original = new File(entry.getKey());
                    File copy = new File(entry.getValue());
                    if (copy.lastModified() < original.lastModified()) {
                        FileChannel source = new FileInputStream(entry.getKey()).getChannel();
                        FileChannel destination = new FileOutputStream(entry.getValue()).getChannel();
                        source.transferTo(0, source.size(), destination);
                        destination.close();
                        source.close();
                    }
                }
                Thread.sleep(3000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
