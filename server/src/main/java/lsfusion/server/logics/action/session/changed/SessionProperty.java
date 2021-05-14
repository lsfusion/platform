package lsfusion.server.logics.action.session.changed;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.SimpleIncrementProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class SessionProperty<T extends PropertyInterface> extends SimpleIncrementProperty<T> {

    public final Property<T> property;
    public final PrevScope scope;

    public SessionProperty(LocalizedString caption, Property<T> property, PrevScope scope) {
        super(caption, property.getFriendlyOrderInterfaces());
        this.property = property;
        this.scope = scope;
    }

    public abstract OldProperty<T> getOldProperty();

    public abstract ChangedProperty<T> getChangedProperty();

    @Override
    public ImSet<OldProperty> getParseOldDepends() {
        return SetFact.singleton(getOldProperty());
    }

    @Override
    public ImSet<SessionProperty> getSessionCalcDepends(boolean events) {
        return super.getSessionCalcDepends(events).addExcl(this); // вызываем super так как могут быть вычисляемые события внутри (желательно для SetOrDropped оптимизации)
    }

    @Override
    public boolean aspectDebugHasAlotKeys() {
        return property.hasAlotKeys();
    }
}
