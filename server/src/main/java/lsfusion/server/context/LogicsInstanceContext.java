package lsfusion.server.context;

import lsfusion.interop.exceptions.LogMessageLogicsException;
import lsfusion.interop.action.ConfirmClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.*;
import lsfusion.server.logics.SecurityManager;
import org.apache.log4j.Logger;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.remote.RemoteForm;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.padLeft;
import static lsfusion.base.BaseUtils.replicate;
import static lsfusion.server.ServerLoggers.systemLogger;

public class LogicsInstanceContext extends AbstractContext {
    private static final Logger logger = Logger.getLogger(LogicsInstanceContext.class);

    private final LogicsInstance logicsInstance;

    public LogicsInstanceContext(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    public SecurityPolicy getSecurityPolicy() {
        return SecurityManager.serverSecurityPolicy;
    }

    public FocusListener getFocusListener() {
        return null;
    }

    public CustomClassListener getClassListener() {
        return null;
    }

    public PropertyObjectInterfaceInstance getComputer() {
        return logicsInstance.getDbManager().getServerComputerObject();
    }

    public Integer getCurrentUser() {
        return logicsInstance.getDbManager().getSystemUserObject();
    }

    public DataObject getConnection() {
        return null;
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        try {
            return new RemoteForm(formInstance, logicsInstance.getRmiManager().getExportPort(), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LogInfo getLogInfo() {
        return LogInfo.system;
    }

    @Override
    public void delayUserInteraction(ClientAction action) {
        if (!processClientAction(action)) {
            super.delayUserInteraction(action);
        }
    }

    @Override
    public Object requestUserInteraction(ClientAction action) {
        if (processClientAction(action)) {
            return null;
        }
        return super.requestUserInteraction(action);
    }

    @Override
    public boolean canBeProcessed() {
        return true;
    }

    @Override
    public Object[] requestUserInteraction(ClientAction... actions) {
        for (ClientAction action : actions) {
            if (!processClientAction(action)) {
                throw new UnsupportedOperationException("requestUserInteraction is not supported for action" + (action == null ? "" : ": " + action.getClass()));
            }
        }
        return new Object[actions.length];
    }

    private boolean processClientAction(ClientAction action) {
        if (action instanceof LogMessageClientAction) {
            LogMessageClientAction logAction = (LogMessageClientAction) action;
            if (logAction.failed) {
                throw new LogMessageLogicsException("Server error: " + logAction.message + "\n" + errorDataToTextTable(logAction.titles, logAction.data));
            } else {
                logger.error(logAction.message);
            }
            return true;
        } else if (action instanceof MessageClientAction) {
            MessageClientAction msgAction = (MessageClientAction) action;
            systemLogger.info("Server message: " + msgAction.message);
            return true;
        } else if (action instanceof ConfirmClientAction) {
            ConfirmClientAction confirmAction = (ConfirmClientAction) action;
            systemLogger.info("Server message: " + confirmAction.message);
            return true;
        }
        return false;
    }

    private String errorDataToTextTable(List<String> titles, List<List<String>> data) {
        if (titles.size() == 0) {
            return "";
        }

        int rCount = data.size() + 1;
        int cCount = titles.size();

        ArrayList<List<String>> all = new ArrayList<List<String>>();
        all.add(titles);
        all.addAll(data);

        int columnWidths[] = new int[cCount];
        for (int i = 0; i < rCount; ++i) {
            List<String> rowData = all.get(i);
            for (int j = 0; j < cCount; ++j) {
                String cellText = rowData.get(j);
                columnWidths[j] = Math.max(columnWidths[j], cellText == null ? 0 : cellText.trim().length());
            }
        }

        int tableWidth = cCount + 1; //рамки
        for (int j = 0; j < cCount; ++j) {
            tableWidth += columnWidths[j];
        }

        String br = replicate('-', tableWidth) + "\n";

        StringBuilder result = new StringBuilder(br);
        for (int i = 0; i < rCount; ++i) {
            List<String> rowData = all.get(i);
            result.append("|");
            for (int j = 0; j < cCount; ++j) {
                String cellText = rowData.get(j);
                result.append(padLeft(cellText, columnWidths[j])).append("|");
            }
            result.append("\n");
            if (i == 0) {
                result.append(br);
            }
        }
        result.append(br);

        return result.toString();
    }
}
