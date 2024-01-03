package lsfusion.server.logics.navigator.controller.init;

import lsfusion.base.BaseUtils;
import lsfusion.server.base.task.PublicTask;
import lsfusion.server.base.task.Task;
import lsfusion.server.logics.controller.init.SimpleBLTask;
import lsfusion.server.logics.controller.remote.RemoteLogicsLoader;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import org.apache.log4j.Logger;

import java.util.Set;

public class ExportRmiObjectTask extends SimpleBLTask {

    private RemoteLogicsLoader remoteLogicsLoader;

    public void setRemoteLogicsLoader(RemoteLogicsLoader remoteLogicsLoader) {
        this.remoteLogicsLoader = remoteLogicsLoader;
    }

    private PublicTask initDevTask;

    public void setInitDevTask(PublicTask initDevTask) {
        this.initDevTask = initDevTask;
    }

    @Override
    public Set<Task> getAllDependencies() {
        Set<Task> result = super.getAllDependencies();

        if(!SystemProperties.lightStart)
            result = BaseUtils.addSet(result, initDevTask);

        return result;
    }

    @Override
    public String getCaption() {
        return "Exporting RMI logics object (port: " + remoteLogicsLoader.getPort() + ")";
    }

    @Override
    public void run(Logger logger) {
        remoteLogicsLoader.exportRmiObject();
    }
}
