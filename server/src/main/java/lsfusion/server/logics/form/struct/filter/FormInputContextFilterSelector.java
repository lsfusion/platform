package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.form.interactive.action.input.FormInputFilterSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

// not used for now, see ActionObjectSelector
public class FormInputContextFilterSelector<V extends PropertyInterface, IO extends ObjectSelector,  O extends ObjectSelector> extends ContextFilterSelector<V, O> {

    private final FormInputFilterSelector<IO, V> formInput;
    private final O object;

    public FormInputContextFilterSelector(FormInputFilterSelector<IO, V> formInput, O object) {
        this.formInput = formInput;
        this.object = object;
    }

    private ContextFilterEntity<?, V, O> getEntity() {
        return formInput.getEntity().getFilter(object);
    }

    @Override
    public ImSet<? extends ContextFilterEntity<?, V, O>> getEntities() {
        return SetFact.singleton(getEntity());
    }

    @Override
    public <C extends PropertyInterface> ContextFilterSelector<C, O> map(ImRevMap<V, C> map) {
        return new FormInputContextFilterSelector<>(formInput.map(map), object);
    }

    @Override
    public PropertyMapImplement<?, V> getWhereProperty(ImRevMap<O, V> objects) {
        return getEntity().getWhereProperty(objects);
    }
}
