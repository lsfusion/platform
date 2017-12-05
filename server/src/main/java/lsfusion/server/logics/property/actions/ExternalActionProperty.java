package lsfusion.server.logics.property.actions;

import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.util.List;

public abstract class ExternalActionProperty extends SystemExplicitActionProperty {

    protected String connectionString;
    protected List<LCP> targetPropList;

    public ExternalActionProperty(int paramsCount, String connectionString, List<LCP> targetPropList) {
        super(SetFact.toOrderExclSet(paramsCount, new GetIndex<PropertyInterface>() {
            @Override
            public PropertyInterface getMapValue(int i) {
                return new PropertyInterface();
            }
        }));
        this.connectionString = connectionString;
        this.targetPropList = targetPropList;
    }

    protected String replaceParams(ExecutionContext context, String connectionString) {
        return replaceParams(context, connectionString, null).first;
    }

    protected Pair<String, String> replaceParams(ExecutionContext context, String connectionString, String exec) {
        ImCol<? extends ObjectValue> values = context.getKeys().values();
        for (int i = values.size(); i > 0; i--) {
            String regex = "\\(\\$" + i + "\\)";
            String replacement = String.valueOf(values.get(i - 1).getValue());
            connectionString = connectionString.replaceAll(regex, replacement);
            if (exec != null)
                exec = exec.replaceAll(regex, replacement);
        }
        return Pair.create(connectionString, exec);
    }

    @Override
    public CalcPropertyMapImplement<?, ClassPropertyInterface> calcWhereProperty() {
        return DerivedProperty.createNull();
    }

    @Override
    protected boolean ignoreFitClassesCheck() {
        return true;
    }
}