package lsfusion.server.logics.debug;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.property.PropertyInterface;

public class ParamDebugInfo<P extends PropertyInterface> {

    public final ImRevMap<String, P> paramsToInterfaces;
    public final ImMap<String, String> paramsToClassFQN;

    public ParamDebugInfo(ImRevMap<String, P> paramsToInterfaces, ImMap<String, String> paramsToClassFQN) {
        this.paramsToInterfaces = paramsToInterfaces;
        this.paramsToClassFQN = paramsToClassFQN;
    }
}
