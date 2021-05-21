package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class FormInputFilterSelector<O extends ObjectSelector, V extends PropertyInterface> implements InputFilterSelector<V> {

    private final FormSelector<O> form;
    private final ImSet<ContextFilterSelector<V, O>> contextFilters;
    private final O inputObject;
    private final ImRevMap<O, V> mapObjects;

    public FormInputFilterSelector(FormSelector<O> form, ImSet<ContextFilterSelector<V, O>> contextFilters, O inputObject, ImRevMap<O, V> mapObjects) {
        this.form = form;
        this.contextFilters = contextFilters;
        this.inputObject = inputObject;
        this.mapObjects = mapObjects;
    }

    @Override
    public InputFilterEntity<?, V> getEntity() {
        Pair<FormEntity, ImRevMap<ObjectEntity, O>> staticFormMap = form.getForm(ThreadLocalContext.getBaseLM());
        if(staticFormMap == null)
            return null;

        FormEntity staticForm = staticFormMap.first;
        ImRevMap<ObjectEntity, O> mapStaticObjects = staticFormMap.second;
        ImRevMap<O, ObjectEntity> reversedMapObjects = mapStaticObjects.reverse();

        return staticForm.getInputFilterEntity(reversedMapObjects.get(inputObject),
                ContextFilterSelector.getEntities(contextFilters).mapSetValues(contextFilter -> contextFilter.mapObjects(reversedMapObjects)), mapStaticObjects.innerJoin(mapObjects));
    }

    @Override
    public <C extends PropertyInterface> FormInputFilterSelector<O, C> map(ImRevMap<V, C> map) {
        return new FormInputFilterSelector<>(form, contextFilters.mapSetValues(selector -> selector.map(map)), inputObject, mapObjects.join(map));
    }
}
