package lsfusion.server.logics.service;

import com.google.common.base.Throwables;
import lsfusion.base.IOUtils;
import lsfusion.interop.action.ExportFileClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MakeHeapDumpActionProperty extends ScriptingActionProperty {

    public MakeHeapDumpActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try { 
            File heapFile = new File("heap-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().getTime()) + ".hprof");
            int pid = getProcessID();
            Runtime.getRuntime().exec(String.format("jmap -dump:file=%s %s", heapFile.getAbsolutePath(), pid));
            while(!heapFile.exists())
                Thread.sleep(1000);
            context.delayUserInteraction(new ExportFileClientAction(heapFile.getName(), IOUtils.getFileBytes(heapFile)));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    
    private int getProcessID() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        java.lang.management.RuntimeMXBean runtime =
                java.lang.management.ManagementFactory.getRuntimeMXBean();
        java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");
        jvm.setAccessible(true);
        sun.management.VMManagement mgmt =
                (sun.management.VMManagement) jvm.get(runtime);
        java.lang.reflect.Method pid_method =
                mgmt.getClass().getDeclaredMethod("getProcessId");
        pid_method.setAccessible(true);
        return (Integer) pid_method.invoke(mgmt);
    }
}
