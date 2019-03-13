package lsfusion.server.logics.form.open;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

// polymorph form selector
public interface FormSelector<O extends ObjectSelector> {
    
    ValueClass getBaseClass(O object);
    
    FormEntity getStaticForm();
    Pair<FormEntity, ImRevMap<ObjectEntity, O>> getForm(BaseLogicsModule LM, DataSession session, ImMap<O, ? extends ObjectValue> mapObjectValues) throws SQLException, SQLHandledException;
}
