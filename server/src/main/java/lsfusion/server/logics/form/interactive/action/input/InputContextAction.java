package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.action.async.QuickAccess;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class InputContextAction<P extends PropertyInterface, V extends PropertyInterface> {

    public final String image;
    public final String keyStroke;
    public final ImList<QuickAccess> quickAccessList;
    
    public final Action<P> action;

    public final ImRevMap<P, V> mapValues; // external context

    public InputContextAction(String image, ImList<QuickAccess> quickAccessList, Action<P> action, ImRevMap<P, V> mapValues) {
        this(image, null, quickAccessList, action, mapValues);
    }

    public InputContextAction(String image, String keyStroke, ImList<QuickAccess> quickAccessList, Action<P> action, ImRevMap<P, V> mapValues) {
        this.image = image;
        this.keyStroke = keyStroke;
        this.quickAccessList = quickAccessList;
        this.action = action;

        this.mapValues = mapValues;
        assert singleInterface() == null || singleInterface() != null;
    }

    // input value 
    public P singleInterface() {
        ImSet<P> extendInterfaces = action.interfaces.removeIncl(mapValues.keys());
        if(extendInterfaces.isEmpty())
            return null;
        return extendInterfaces.single();        
    }

    public ImMap<V, ValueClass> getInterfaceClasses() {
        return mapValues.innerCrossJoin(action.getInterfaceClasses(ClassType.wherePolicy));
    }

    public <C extends PropertyInterface> InputContextAction<P, C> map(ImRevMap<V, C> map) {
        return new InputContextAction<>(image, keyStroke, quickAccessList, action, mapValues.join(map));
    }

    public AsyncMapEventExec<V> getAsyncEventExec() {
        AsyncMapEventExec<P> asyncEventExec = action.getAsyncEventExec(false);
        if(asyncEventExec != null && !asyncEventExec.needOwnPushResult())
            return asyncEventExec.mapInner(mapValues);
        return null;
    }

    public void execute(ExecutionContext<V> context, ObjectValue userValue) throws SQLException, SQLHandledException {
        ExecutionContext<P> mappedContext = context.map(mapValues);

        P stringInterface = singleInterface();
        if(stringInterface != null) // adding string param if it is used
            mappedContext = mappedContext.override(BaseUtils.<ImMap<P, ObjectValue>>immutableCast(mappedContext.getKeys()).addExcl(stringInterface, userValue));

        action.execute(mappedContext);
    }
}
