package lsfusion.server.logics.debug;

public class ActionDebugInfo extends DebugInfo {

    public final ActionDelegationType delegationType;

    public ActionDebugInfo(String moduleName, int line, int offset, ActionDelegationType delegationType) {
        super(moduleName, line, offset);
        this.delegationType = delegationType;
    }
}
