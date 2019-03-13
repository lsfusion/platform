package lsfusion.server.logics.property.actions;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.IsClassProperty;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;

// с явным задание классов параметров (where определяется этими классами)
public abstract class ExplicitActionProperty extends BaseActionProperty<ClassPropertyInterface> {
    protected ExplicitActionProperty(ValueClass... classes) {
        this(LocalizedString.NONAME, classes);
    }

    protected ExplicitActionProperty(ImOrderSet interfaces) {
        super(LocalizedString.NONAME, interfaces);
    }

    protected ExplicitActionProperty(LocalizedString caption, ValueClass[] classes) {
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
                if (this instanceof ScriptingActionProperty)
                    ((ScriptingActionProperty) this).commonExecuteCustomDelegate(context);
                else
                    executeCustom(context);
            }
        }
        return FlowResult.FINISH;
    }

    protected ImSet<ClassPropertyInterface> getNoClassesInterfaces() {
        return SetFact.EMPTY();
    }
    
    public CalcPropertyMapImplement<?, ClassPropertyInterface> calcWhereProperty() {
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
