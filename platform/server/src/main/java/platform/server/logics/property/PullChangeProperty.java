package platform.server.logics.property;

import platform.base.QuickSet;
import platform.server.session.StructChanges;

import java.util.List;

public abstract class PullChangeProperty<T extends PropertyInterface, P extends PropertyInterface, I extends PropertyInterface> extends ChangeProperty<I> {

    // assert что constraint.isFalse
    protected final Property<T> onChange;
    protected final Property<P> toChange;

    public PullChangeProperty(String SID, String caption, List<I> interfaces, Property<T> onChange, Property<P> toChange) {
        super(SID, caption, interfaces);
        this.onChange = onChange;
        this.toChange = toChange;
    }

    public boolean isChangeBetween(Property property) {
        return equals(property) || (depends(onChange, property) && depends(property, toChange));
    }

    public static QuickSet<Property> getUsedChanges(Property<?> onChange, Property<?> toChange, StructChanges propChanges) {
        return QuickSet.add(toChange.getUsedDataChanges(propChanges), onChange.getUsedChanges(propChanges));
    }

    protected QuickSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return getUsedChanges(onChange,toChange, propChanges);
    }

}
