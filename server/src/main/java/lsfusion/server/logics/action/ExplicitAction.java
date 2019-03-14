package lsfusion.server.logics.action;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// с явным задание классов параметров (where определяется этими классами)
public abstract class ExplicitAction extends BaseAction<ClassPropertyInterface> {
    protected ExplicitAction(ValueClass... classes) {
        this(LocalizedString.NONAME, classes);
    }

    protected ExplicitAction(ImOrderSet interfaces) {
        super(LocalizedString.NONAME, interfaces);
    }

    protected ExplicitAction(LocalizedString caption, ValueClass[] classes) {
        super(caption, IsClassProperty.getInterfaces(classes));
    }

    protected abstract void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException;

    public boolean allowNullValue;
    protected abstract boolean allowNulls();
    
    protected boolean checkNulls(ImSet<ClassPropertyInterface> dataKeys) {
        if (allowNullValue)
            return false;

        if (allowNulls())
            return false;

        return dataKeys.size() < interfaces.size();
    }

    public final FlowResult aspectExecute(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<ClassPropertyInterface,DataObject> dataKeys = DataObject.filterDataObjects(context.getKeys());
        if(checkNulls(dataKeys.keys()))
            proceedNullException();
        else {
            if(IsClassProperty.fitInterfaceClasses(context.getSession().getCurrentClasses(dataKeys).removeIncl(getNoClassesInterfaces()))) { // если подходит по классам выполнем
                if (this instanceof ScriptingAction)
                    ((ScriptingAction) this).commonExecuteCustomDelegate(context);
                else
                    executeCustom(context);
            }
        }
        return FlowResult.FINISH;
    }

    protected ImSet<ClassPropertyInterface> getNoClassesInterfaces() {
        return SetFact.EMPTY();
    }
    
    public PropertyMapImplement<?, ClassPropertyInterface> calcWhereProperty() {
        return IsClassProperty.getProperty(interfaces.removeIncl(getNoClassesInterfaces()));
    }

    protected boolean isSync() {
        return false;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.SYNC && isSync())
            return true;
        return super.hasFlow(type);
    }
}
