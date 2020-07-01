package lsfusion.server.logics.constraint;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.List;

public class OutFormSelector<P extends PropertyInterface> implements FormSelector<ObjectSelector> {

    private final Property<P> property;
    private final Property messageProperty;
    private final List<ScriptingLogicsModule.LPWithParams> properties;
    
    public OutFormSelector(Property<P> property, Property messageProperty, List<ScriptingLogicsModule.LPWithParams> properties) {
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

    @Override
    public Pair<FormEntity, ImRevMap<ObjectEntity, ObjectSelector>> getForm(BaseLogicsModule LM, DataSession session, ImMap<ObjectSelector, ? extends ObjectValue> mapObjectValues) {
        return new Pair<>(LM.getLogForm(property, messageProperty, properties), MapFact.EMPTYREV());
    }
}
