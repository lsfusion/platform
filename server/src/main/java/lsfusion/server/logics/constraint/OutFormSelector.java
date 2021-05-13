package lsfusion.server.logics.constraint;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class OutFormSelector<P extends PropertyInterface> implements FormSelector<ObjectSelector> {

    private final Property<P> property;
    private final Property messageProperty;
    private final ImList<PropertyMapImplement<?,P>> properties;

    public OutFormSelector(Property<P> property, Property messageProperty, ImList<PropertyMapImplement<?, P>> properties) {
        this.property = property;
        this.messageProperty = messageProperty;
        this.properties = properties;
    }

    @Override
    public ValueClass getBaseClass(ObjectSelector object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormEntity getNFStaticForm() {
        return null;
    }

    // strictly speaking if properties is not null we can do form "completely" static (for example return in getNFStaticForm)
    // IdentityStrongLazy is important to avoid memory leaks
    @Override
    @IdentityStrongLazy
    public FormEntity getStaticForm(BaseLogicsModule LM) {
        return LM.getLogForm(property, messageProperty, properties);
    }

    @Override
    public Pair<FormEntity, ImRevMap<ObjectEntity, ObjectSelector>> getForm(BaseLogicsModule LM, DataSession session, ImMap<ObjectSelector, ? extends ObjectValue> mapObjectValues) {
        return new Pair<>(getStaticForm(LM), MapFact.EMPTYREV());
    }

    @Override
    public FormSelector<ObjectSelector> merge(FormSelector formSelector) {
        return null;
    }
}
