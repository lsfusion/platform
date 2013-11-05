package lsfusion.server.form.instance;

import lsfusion.base.BaseUtils;
import lsfusion.base.FunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.server.logics.property.CalcProperty;

public class ChangedData {
    
    public final FunctionSet<CalcProperty> props;
    public final boolean wasRestart;
    
    public static ChangedData EMPTY = new ChangedData(SetFact.<CalcProperty>EMPTY(), false);

    public ChangedData(FunctionSet<CalcProperty> props, boolean wasRestart) {
        this.props = props;
        this.wasRestart = wasRestart;
    }

    public ChangedData merge(ChangedData data) {
        return new ChangedData(BaseUtils.merge(props, data.props), wasRestart || data.wasRestart);
    }
}
