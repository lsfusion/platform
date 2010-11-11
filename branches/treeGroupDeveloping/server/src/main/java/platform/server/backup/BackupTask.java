package platform.server.backup;

import platform.server.data.sql.DataAdapter;
import platform.server.logics.scheduler.SchedulerTask;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupTask implements SchedulerTask {
    private DataAdapter adapter;
    private int backupHour;
    private int firstDays;
    private int base;
    private String dumpDir;
    private String binPath;
    private Date date;
    private DateFormat dateFormat;
    boolean dayBackup = false;

    public BackupTask(DataAdapter adapter, int backupHour, int firstDays, int base, String dumpDir) {
        this.adapter = adapter;
        this.backupHour = backupHour;
        this.firstDays = firstDays;
        this.base = base;
        this.dumpDir = dumpDir;
        Calendar calendar = Calendar.getInstance();
        date = calendar.getTime();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    public String getBinPath() {
        return binPath;
    }

    public void setBinPath(String binPath) {
        this.binPath = binPath;
    }

    public String getID() {
        return "dump";
    }

    private int getRank(String s) {
        Date oldDate = new Date();
        try {
            oldDate = dateFormat.parse(s);
        } catch (ParseException e) {
            return -1;
        }
        int dayDiff = Math.round((date.getTime() - oldDate.getTime()) / 86400000);
        if (dayDiff < firstDays) {
            return dayDiff;
        }
        dayDiff -= firstDays;
        int k = 1;
        while (true) {
            if (dayDiff < Math.pow(base, k)) {
                return firstDays + k;
            }
            dayDiff -= (int) Math.pow(base, k);
            k++;
        }
    }

    private void deleteFile(String s) {
        File f = new File(s);
        f.delete();
    }

    public void execute() throws Exception {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour != backupHour) {
            dayBackup = false;
            return;
        }

        if (dayBackup) {
            return;
        }
        dayBackup = true;
        Date date = new Date();
        String dateTime = dateFormat.format(date);
        File f = new File(dumpDir);
        f.mkdir();
        String[] children = f.list();
        ArrayList<String> fileList = new ArrayList<String>(Arrays.asList(children));
        ArrayList<String> dateList = new ArrayList<String>();
        fileList.add(dateTime);

        String path = "\"" + ((binPath == null) ? "" : binPath) + "pg_dump.exe" + "\"";
        String server = adapter.server;
        String host = "", port = "5432";
        if (server.contains(":")) {
            host = server.substring(0, server.lastIndexOf(':'));
            port = server.substring(server.lastIndexOf(':') + 1);
        } else {
            host = server;
        }

        String execute = "cmd /c start \"\" /B " + path + " --host " + host + " --port " + port + " --username " +
                adapter.userID + " --format tar --blobs --verbose --file \"" + dumpDir + dateTime + "\" " +
                adapter.dataBase + " 2>> \"" + dumpDir + "log.txt\"";
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec(execute);

        int exitVal = p.waitFor();
        //System.out.println("ExitValue: " + exitVal);
        Collections.sort(fileList);

        for (String s : fileList) {
            if (getRank(s) != -1) {
                dateList.add(s);
            }
        }

        String prev = "";
        for (String s : dateList) {
            if (getRank(s) == getRank(prev)) {
                deleteFile(dumpDir + s);
            }
            prev = s;
        }

    }
}
