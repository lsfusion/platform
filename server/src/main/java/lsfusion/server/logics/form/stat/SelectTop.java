package lsfusion.server.logics.form.stat;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.util.List;

public abstract class SelectTop<T> {

    public abstract List<T> getParams();

    public abstract SelectTop<ValueClass> mapValues(ScriptingLogicsModule LM, List<ScriptingLogicsModule.TypedParameter> params);

    public abstract SelectTop<ClassPropertyInterface> mapValues(ImOrderSet<ClassPropertyInterface> orderInterfaces, int extraParams);

    public abstract SelectTop<Integer> mapValues(ExecutionContext<ClassPropertyInterface> context);

    public <P> SelectTop<P> mapValues(ImRevMap<T, P> mapValues) {
        return SingleSelectTop.NULL(); //todo
    }

    public abstract LimitOffset getLimitOffset(GroupObjectEntity group);

    public abstract ImOrderSet<T> getWindowInterfaces(GroupObjectEntity group);
}
