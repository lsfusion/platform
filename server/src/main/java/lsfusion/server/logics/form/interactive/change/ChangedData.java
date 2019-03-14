package lsfusion.server.logics.form.interactive.change;

import lsfusion.base.BaseUtils;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.Property;

public class ChangedData {
    
    public final FunctionSet<Property> props;
    public final boolean wasRestart;

    public final FunctionSet<Property> externalProps;

    public static ChangedData EMPTY = new ChangedData(SetFact.<Property>EMPTY(), SetFact.<Property>EMPTY(), false);

    public ChangedData(ImSet<Property> props, boolean wasRestart) {
        this(Property.getDependsOnSet(props), SetFact.<Property>EMPTY(), wasRestart);
    }

    public ChangedData(ImSet<Property> externalProps) {
        this(SetFact.<Property>EMPTY(), Property.getDependsOnSet(externalProps), false);
    }

    public ChangedData(FunctionSet<Property> props, FunctionSet<Property> externalProps, boolean wasRestart) {
        this.props = props;
        this.externalProps = externalProps;
        this.wasRestart = wasRestart;
    }

    public ChangedData merge(ChangedData data) {
        return new ChangedData(BaseUtils.merge(props, data.props), BaseUtils.merge(externalProps, data.externalProps), wasRestart || data.wasRestart);
    }
}
