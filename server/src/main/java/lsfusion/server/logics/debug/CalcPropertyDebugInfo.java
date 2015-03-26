package lsfusion.server.logics.debug;

public class CalcPropertyDebugInfo extends DebugInfo {
    public CalcPropertyDebugInfo(String moduleName, int line, int offset) {
        super(moduleName, line, offset);
    }

    @Override
    public String toString() {
        return moduleName + " (" + line + ":" + offset + ")";
    }
}
