package lsfusion.server.logics.form.stat;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.CompareEntity;

import java.sql.SQLException;

public interface FormDataInterface {

    FormEntity getFormEntity();

    BaseClass getBaseClass();
    QueryEnvironment getQueryEnv();
    DataSession getSession();
    Modifier getModifier();

    StaticDataGenerator.Hierarchy getHierarchy(boolean isReport);
    ImMap<ObjectEntity, ? extends ObjectValue> getObjectValues(ImSet<GroupObjectEntity> valueGroups);
    Where getWhere(GroupObjectEntity groupObject, ImSet<GroupObjectEntity> valueGroups, ImMap<ObjectEntity, Expr> mapExprs) throws SQLException, SQLHandledException;
    Where getValueWhere(GroupObjectEntity groupObject, ImSet<GroupObjectEntity> valueGroups, ImMap<ObjectEntity, Expr> mapExprs) throws SQLException, SQLHandledException;
    ImOrderMap<CompareEntity, Boolean> getOrders(GroupObjectEntity groupObject, ImSet<GroupObjectEntity> valueGroups);
}
