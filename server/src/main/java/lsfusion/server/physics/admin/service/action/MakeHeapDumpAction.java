package lsfusion.server.physics.admin.service.action;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.WriteClientAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MakeHeapDumpAction extends InternalAction {

    public MakeHeapDumpAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        try { 
            File heapFile = new File("heap-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) + ".hprof");
            int pid = getProcessID();
            Runtime.getRuntime().exec(String.format("jmap -dump:file=%s %s", heapFile.getAbsolutePath(), pid));
            while(!heapFile.exists())
                Thread.sleep(1000);
            context.delayUserInteraction(new WriteClientAction(new RawFileData(heapFile), heapFile.getName(), null, false, true));
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
