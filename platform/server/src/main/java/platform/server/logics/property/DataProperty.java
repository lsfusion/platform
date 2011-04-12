package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.query.Join;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class DataProperty extends UserProperty {

    public ValueClass value;
    
    public DataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes);        
        this.value = value;
    }

    public static List<ClassPropertyInterface> getInterfaces(ValueClass[] classes) {
        List<ClassPropertyInterface> interfaces = new ArrayList<ClassPropertyInterface>();
        for(ValueClass interfaceClass : classes)
            interfaces.add(new ClassPropertyInterface(interfaces.size(),interfaceClass));
        return interfaces;
    }

    public <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        SimpleChanges modifierChanges = modifier.getChanges();
        return (derivedChange != null ? derivedChange.getUsedChanges(modifier) : modifier.newChanges()).
                addChanges(new SimpleChanges(modifierChanges, BaseUtils.merge(ClassProperty.getValueClasses(interfaces), Collections.singleton(value)), true)).
                addChanges(new SimpleChanges(modifierChanges, this));
    }

    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {

        ExprChanges session = modifier.getSession();

        Expr dataExpr = getExpr(joinImplement);

        ExprCaseList cases = new ExprCaseList();

        // ручные изменения
        Join<String> changedJoin = session.getDataChange(this, joinImplement);
        if(changedJoin!=null)
            cases.add(changedJoin.getWhere(),changedJoin.getExpr("value"));

        // блок с удалением
        Where removeWhere = session.getRemoveWhere(value,dataExpr);
        for(ClassPropertyInterface remove : interfaces)
            removeWhere = removeWhere.or(session.getRemoveWhere(remove.interfaceClass,joinImplement.get(remove)));
        cases.add(removeWhere.and(dataExpr.getWhere()), Expr.NULL);

        // производные изменения
        if(derivedChange !=null) {
            PropertyChange<ClassPropertyInterface> defaultChanges = derivedChange.getDataChanges(modifier).get(this);
            if(defaultChanges !=null) {
                Join<String> defaultJoin = defaultChanges.join(joinImplement);
                cases.add(defaultJoin.getWhere(),defaultJoin.getExpr("value"));
            }
        }

        if(changedWhere !=null) changedWhere.add(cases.getUpWhere());
        cases.add(Where.TRUE,dataExpr);
        return cases.getExpr();
    }

    public ValueClass getValueClass() {
        return value;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        session.changeProperty(this, keys, value, groupLast);
    }
}
