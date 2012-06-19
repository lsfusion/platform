package platform.server.logics.property;

import platform.base.QuickSet;
import platform.server.session.StructChanges;

import java.util.List;

public abstract class PullChangeProperty<T extends PropertyInterface, P extends PropertyInterface, I extends PropertyInterface> extends ChangeProperty<I> {

    // assert что constraint.isFalse
    protected final CalcProperty<T> onChange;
    protected final CalcProperty<P> toChange;

    public PullChangeProperty(String SID, String caption, List<I> interfaces, CalcProperty<T> onChange, CalcProperty<P> toChange) {
        super(SID, caption, interfaces);
        this.onChange = onChange;
        this.toChange = toChange;
    }

    public boolean isChangeBetween(CalcProperty property) {
        return equals(property) || (depends(onChange, property) && depends(property, toChange));
    }

    public static QuickSet<CalcProperty> getUsedChanges(CalcProperty<?> onChange, CalcProperty<?> toChange, StructChanges propChanges) {
        return QuickSet.add(toChange.getUsedDataChanges(propChanges), onChange.getUsedChanges(propChanges));
    }

    protected QuickSet<CalcProperty> calculateUsedChanges(StructChanges propChanges, boolean cascade) {
        return getUsedChanges(onChange,toChange, propChanges);
    }

}
