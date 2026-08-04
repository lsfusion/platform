package lsfusion.server.physics.admin.service.action;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.WriteClientAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MakeHeapDumpAction extends InternalAction {

    public MakeHeapDumpAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        try {
            String name = "heap-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
            File heapFile = new File(name + ".hprof");
            Runtime.getRuntime().exec(String.format("jmap -dump:file=%s %s", heapFile.getAbsolutePath(), getProcessID()));
            while(!heapFile.exists())
                Thread.sleep(1000);
            context.delayUserInteraction(new WriteClientAction(new NamedFileData(new FileData(new RawFileData(heapFile), BaseUtils.getFileExtension(heapFile)), name), name, false, true));
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
        }
    }

    private long getProcessID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return Long.parseLong(name.substring(0, name.indexOf('@')));
    }
}
