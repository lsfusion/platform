package lsfusion.server.form.instance;

import lsfusion.base.BaseUtils;
import lsfusion.base.FunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.CalcProperty;

public class ChangedData {
    
    public final FunctionSet<CalcProperty> props;
    public final boolean wasRestart;

    public final FunctionSet<CalcProperty> externalProps;

    public static ChangedData EMPTY = new ChangedData(SetFact.<CalcProperty>EMPTY(), SetFact.<CalcProperty>EMPTY(), false);

    public ChangedData(ImSet<CalcProperty> props, boolean wasRestart) {
        this(CalcProperty.getDependsOnSet(props), SetFact.<CalcProperty>EMPTY(), wasRestart);
    }

    public ChangedData(ImSet<CalcProperty> externalProps) {
        this(SetFact.<CalcProperty>EMPTY(), CalcProperty.getDependsOnSet(externalProps), false);
    }

    public ChangedData(FunctionSet<CalcProperty> props, FunctionSet<CalcProperty> externalProps, boolean wasRestart) {
        this.props = props;
        this.externalProps = externalProps;
        this.wasRestart = wasRestart;
    }

    public ChangedData merge(ChangedData data) {
        return new ChangedData(BaseUtils.merge(props, data.props), BaseUtils.merge(externalProps, data.externalProps), wasRestart || data.wasRestart);
    }
}
