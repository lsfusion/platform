package platform.server.logics.property.actions;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.classes.ValueClass;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.flow.ChangeFlowType;
import platform.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;

public abstract class CustomActionProperty extends ActionProperty<ClassPropertyInterface> {

    protected CustomActionProperty(String sID, ValueClass... classes) {
        this(sID, "sys", classes);
    }

    protected CustomActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, IsClassProperty.getInterfaces(classes));
    }

    protected abstract void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException;

    public final FlowResult aspectExecute(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        if(IsClassProperty.fitInterfaceClasses(context.getSession().getCurrentClasses(context.getKeys()))) // если подходит по классам выполнем
            executeCustom(context);
        return FlowResult.FINISH;
    }

    public CalcPropertyMapImplement<?, ClassPropertyInterface> getWhereProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }
    
    protected boolean isVolatile() {
        return false;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.VOLATILE && isVolatile();
    }
}
