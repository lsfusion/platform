package lsfusion.server.logics.debug;

import lsfusion.server.logics.property.PropertyInterface;

import java.util.Map;

public class ActionDebugInfo {
    public final Map<String, PropertyInterface> paramsToInterfaces;
    public final Map<String, String> paramsToClassFQN;
    
    public final String moduleName;
    public final int line;
    public final int offset;
    public final boolean delegateExecute;

    public ActionDebugInfo(Map<String, PropertyInterface> paramsToInterfaces, Map<String, String> paramsToClassFQN, String moduleName,
                           int line, int offset, boolean delegateExecute) {
        
        this.paramsToInterfaces = paramsToInterfaces;
        this.paramsToClassFQN = paramsToClassFQN;

        this.moduleName = moduleName;
        this.line = line;
        this.offset = offset;
        this.delegateExecute = delegateExecute;
    }

    public String getMethodName() {
        return "action_" + line + "_" + offset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ActionDebugInfo that = (ActionDebugInfo) o;

        return line == that.line &&
               offset == that.offset &&
               moduleName.equals(that.moduleName);
    }

    @Override
    public int hashCode() {
        int result = moduleName.hashCode();
        result = 31 * result + line;
        result = 31 * result + offset;
        return result;
    }
}
