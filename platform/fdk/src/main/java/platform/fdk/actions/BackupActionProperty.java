package platform.fdk.actions;

import platform.server.classes.ValueClass;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.File;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupActionProperty extends ScriptingActionProperty {

    private Integer firstDays;
    private Integer base;
    private String dumpDir;
    DateFormat dateFormat;
    Date date;
    String binPath;

    public BackupActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {

            DataSession session = createSession();
            DataAdapter adapter = session.sql.adapter;
            firstDays = (Integer) LM.findLCPByCompoundName("firstDaysBackupTask").read(session);
            base = (Integer) LM.findLCPByCompoundName("baseBackupTask").read(session);
            dumpDir = (String) LM.findLCPByCompoundName("dumpDirBackupTask").read(session);

            Calendar calendar = Calendar.getInstance();

            date = calendar.getTime();
            dateFormat = new SimpleDateFormat("yyyy-MM-dd");

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

            String execute = "cmd SET PGPASSWORD=" + adapter.password + " /c start \"\" /B " + path + " --host " + host + " --port " + port + " --username " +
                    adapter.userID + " --format tar --blobs --verbose --file \"" + dumpDir + dateTime + "\" " +
                    adapter.dataBase + " 2>> \"" + dumpDir + "log.txt\"";
            Runtime rt = Runtime.getRuntime();
            Process p = rt.exec(execute);
            int exitVal = p.waitFor();
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
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

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


}
