package lsfusion.server.logics.form.open;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.action.input.InputContextProperty;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

// polymorph form selector
public interface FormSelector<O extends ObjectSelector> {
    
    ValueClass getBaseClass(O object);
    
    FormEntity getNFStaticForm();
    default FormEntity getStaticForm(BaseLogicsModule LM) {
        return getForm(LM).first; // always not null since session is null
    }
    default Pair<FormEntity, ImRevMap<ObjectEntity, O>> getForm(BaseLogicsModule LM) {
        try {
            return getForm(LM, null, MapFact.EMPTY()); // always not null since session is null
        } catch (SQLException | SQLHandledException e) { // can't be since session is null
            throw Throwables.propagate(e);
        }
    }
    Pair<FormEntity, ImRevMap<ObjectEntity, O>> getForm(BaseLogicsModule LM, DataSession session, ImMap<O, ? extends ObjectValue> mapObjectValues) throws SQLException, SQLHandledException;
    default FormEntity getStaticForm(BaseLogicsModule LM, CustomClass customClass) {
        return getStaticForm(LM);
    } 

    default <P extends PropertyInterface> InputContextProperty<?, P> getInputContextProperty(BaseLogicsModule LM, O object, ImSet<ContextFilterEntity<?, P, O>> contextFilters, ImRevMap<O, P> mapObjects) {
        Pair<FormEntity, ImRevMap<ObjectEntity, O>> staticForm = getForm(LM);
        if(staticForm == null)
            return null;

        return staticForm.first.getInputContextProperty(LM, staticForm.second.singleKey(), contextFilters.mapSetValues(contextFilter -> contextFilter.mapObjects(staticForm.second.reverse())), staticForm.second.join(mapObjects));
    }
    
    // async merge
    FormSelector<O> merge(FormSelector formSelector);
}
