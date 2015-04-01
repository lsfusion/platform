package lsfusion.server.logics.debug;

public class ActionDebugInfo extends DebugInfo {

    public final ActionDelegationType delegationType;

    public String toString() {
        return moduleName + "(" + line + ":" + offset + ")";
    }

    public ActionDebugInfo(String moduleName, int line, int offset, ActionDelegationType delegationType) {
        super(moduleName, line, offset);
        this.delegationType = delegationType;
    }

    public String getMethodName(boolean firstInLine) {
        return "action_" + line + (firstInLine ? "" : "_" + offset);
    }
}
