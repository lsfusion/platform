package paas.manager.server;

import com.google.common.base.Throwables;
import org.apache.commons.exec.ProcessDestroyer;
import platform.base.SystemUtils;

import java.util.Vector;

public class AppManagerProcessDestroyer implements ProcessDestroyer {
    private final AppManager appManager;

    private final Vector<Process> processes = new Vector<Process>();
    private final Vector<Integer> busyPorts = new Vector<Integer>();

    public AppManagerProcessDestroyer(AppManager appManager) {
        this.appManager = appManager;
    }

    @Override
    public boolean add(Process process) {
        return processes.add(process);
    }

    @Override
    public boolean remove(Process process) {
        return processes.remove(process);
    }

    @Override
    public int size() {
        return processes.size();
    }

    public void addPort(int port) {
        busyPorts.addElement(port);
    }

    public void removePort(int port) {
        busyPorts.removeElement(port);
    }

    public void shutdown() {
        synchronized (busyPorts) {
            for (int port : busyPorts) {
                try {
                    appManager.stopApplication(port);
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            }
        }

        int attempts = 0;
        while (size() != 0) {
            ++attempts;
            SystemUtils.sleep(1000);
            // завершение managed логики - это просто System.exit(), что должно быть довольно быстро, поэтому ждём не более 20 секунд
            if (attempts == 20) {
                break;
            }
        }

        //force kill все оставшиеся процессы
        synchronized (processes) {
            for (Process p : processes) {
                try {
                    p.destroy();
                } catch (Throwable t) {
                    System.err.println("Unable to terminate process during process shutdown");
                }
            }
        }
    }
}
