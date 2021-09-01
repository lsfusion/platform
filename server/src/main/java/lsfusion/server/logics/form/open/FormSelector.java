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
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

// polymorph form selector
public interface FormSelector<O extends ObjectSelector> {
    
    ValueClass getBaseClass(O object);
    boolean isSingleGroup(O object);

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

    // async merge
    FormSelector<O> merge(FormSelector formSelector);
}
