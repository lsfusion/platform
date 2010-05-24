package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.session.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.data.PropertyField;
import platform.server.data.query.Join;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.view.form.client.RemoteFormView;
import platform.base.BaseUtils;
import platform.interop.action.ClientAction;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.sql.SQLException;

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
        SessionChanges session = modifier.getSession();
        return (derivedChange != null ? derivedChange.getUsedChanges(modifier) : modifier.newChanges()).addChanges(
                session.getSessionChanges(this).add(new SessionChanges(session, ClassProperty.getValueClasses(interfaces), true)));
    }

    @Override
    public <U extends Changes<U>> U getUsedDataChanges(Modifier<U> modifier) {
        return modifier.newChanges().addChanges(new SessionChanges(modifier.getSession(), ClassProperty.getValueClasses(interfaces), false));
    }

    @Override
    protected void fillDepends(Set<Property> depends, boolean derived) {
        if(derived && derivedChange !=null) derivedChange.fillDepends(depends);
    }

    public DerivedChange<?,?> derivedChange = null;

    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {

        SessionChanges session = modifier.getSession();

        Expr dataExpr = getExpr(joinImplement);

        // ручные изменения
        ExprCaseList cases = new ExprCaseList();
        DataChangeTable dataChange = session.data.get(this);
        if(dataChange!=null) {
            Join<PropertyField> changedJoin = dataChange.join(BaseUtils.join(dataChange.mapKeys, joinImplement));
            cases.add(changedJoin.getWhere(),changedJoin.getExpr(dataChange.value));
        }

        // блок с удалением
        Where removeWhere = session.getRemoveWhere(value,dataExpr);
        for(ClassPropertyInterface remove : interfaces)
            removeWhere = removeWhere.or(session.getRemoveWhere(remove.interfaceClass,joinImplement.get(remove)));
        cases.add(removeWhere.and(dataExpr.getWhere()), Expr.NULL);

        // свойства по умолчанию
        if(derivedChange !=null) {
            PropertyChange<ClassPropertyInterface> defaultChanges = derivedChange.getDataChanges(modifier).get(this);
            if(defaultChanges !=null) {
                Join<String> defaultJoin = defaultChanges.getQuery("value").join(joinImplement);
                cases.add(defaultJoin.getWhere(),defaultJoin.getExpr("value"));
            }
        }

        if(changedWhere !=null) changedWhere.add(cases.getUpWhere());
        cases.add(Where.TRUE,dataExpr);
        return cases.getExpr();
    }

    protected ValueClass getValueClass() {
        return value;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteFormView executeForm) throws SQLException {
        session.changeProperty(this, keys, value);
    }

}
