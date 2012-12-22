package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.session.StructChanges;

public abstract class PullChangeProperty<T extends PropertyInterface, P extends PropertyInterface, I extends PropertyInterface> extends ChangeProperty<I> {

    // assert что constraint.isFalse
    protected final CalcProperty<T> onChange;
    public final CalcProperty<P> toChange;

    public PullChangeProperty(String SID, String caption, ImOrderSet<I> interfaces, CalcProperty<T> onChange, CalcProperty<P> toChange) {
        super(SID, caption, interfaces);
        this.onChange = onChange;
        this.toChange = toChange;
    }

    public boolean isChangeBetween(CalcProperty property) {
        return equals(property) || (depends(onChange, property) && depends(property, toChange));
    }

    public static ImSet<CalcProperty> getUsedChanges(CalcProperty<?> onChange, CalcProperty<?> toChange, StructChanges propChanges) {
        return SetFact.add(toChange.getUsedDataChanges(propChanges), onChange.getUsedChanges(propChanges));
    }

    public ImSet<CalcProperty> calculateUsedChanges(StructChanges propChanges, boolean cascade) {
        return getUsedChanges(onChange,toChange, propChanges);
    }

}
