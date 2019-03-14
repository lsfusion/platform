package lsfusion.server.logics.property.derived;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.action.session.change.StructChanges;

public abstract class PullChangeProperty<T extends PropertyInterface, P extends PropertyInterface, I extends PropertyInterface> extends ChangeProperty<I> {

    // assert что constraint.isFalse
    protected final Property<T> onChange;
    public final Property<P> toChange;

    public PullChangeProperty(LocalizedString caption, ImOrderSet<I> interfaces, Property<T> onChange, Property<P> toChange) {
        super(caption, interfaces);
        this.onChange = onChange;
        this.toChange = toChange;
    }

    public boolean isChangeBetween(Property property) {
        return equals(property) || (depends(onChange, property) && depends(property, toChange));
    }

    public static ImSet<Property> getUsedChanges(Property<?> onChange, Property<?> toChange, StructChanges propChanges) {
        return SetFact.add(toChange.getUsedDataChanges(propChanges), onChange.getUsedChanges(propChanges));
    }

    public ImSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return getUsedChanges(onChange,toChange, propChanges);
    }

    @Override
    protected void fillDepends(MSet<Property> depends, boolean events) {
        depends.add(onChange);
        depends.add(toChange);
    }
}
