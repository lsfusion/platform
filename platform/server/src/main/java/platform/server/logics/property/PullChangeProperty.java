package platform.server.logics.property;

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
}
