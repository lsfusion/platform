package platform.server.logics.property.actions.flow;

import platform.server.caches.IdentityLazy;
import platform.server.classes.CustomClass;
import platform.server.data.type.Type;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static platform.base.BaseUtils.reverse;

public class ListActionProperty extends KeepContextActionProperty {

    private final List<ActionPropertyMapImplement<?, PropertyInterface>> actions;
    private boolean isAbstract = false;

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> ListActionProperty(String sID, String caption, List<I> innerInterfaces, List<ActionPropertyMapImplement<?, I>> actions)  {
        this(sID, caption, false, innerInterfaces, actions);
    }

    public <I extends PropertyInterface> ListActionProperty(String sID, String caption, boolean isAbstract, List<I> innerInterfaces, List<ActionPropertyMapImplement<?, I>> actions)  {
        super(sID, caption, innerInterfaces.size());

        this.actions = DerivedProperty.mapActionImplements(reverse(getMapInterfaces(innerInterfaces)), actions);
        this.isAbstract = isAbstract;

        finalizeInit();
    }

    @IdentityLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        List<CalcPropertyInterfaceImplement<PropertyInterface>> listWheres = new ArrayList<CalcPropertyInterfaceImplement<PropertyInterface>>();
        for(ActionPropertyMapImplement<?, PropertyInterface> action : actions)
            listWheres.add(action.mapWhereProperty());
        return DerivedProperty.createUnion(interfaces, listWheres);
    }

    public Set<ActionProperty> getDependActions() {
        Set<ActionProperty> depends = new HashSet<ActionProperty>();
        for(ActionPropertyMapImplement<?, PropertyInterface> action : actions)
            depends.add(action.property);
        return depends;
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        FlowResult result = FlowResult.FINISH;

        for (ActionPropertyMapImplement<?, PropertyInterface> action : actions) {
            FlowResult actionResult = action.execute(context);
            if (actionResult != FlowResult.FINISH) {
                result =  actionResult;
                break;
            }
        }

        return result;
    }

    @Override
    public Type getSimpleRequestInputType() {
        Type type = null;
        for (ActionPropertyMapImplement<?, PropertyInterface> action : actions) {
            Type actionRequestType = action.property.getSimpleRequestInputType();
            if (actionRequestType != null) {
                if (type == null) {
                    type = actionRequestType;
                } else {
                    type = type.getCompatible(actionRequestType);
                    if (type == null) {
                        return null;
                    }
                }
            }
        }
        return type;
    }

    @Override
    public CustomClass getSimpleAdd() {
        CustomClass result = null;
        for (ActionPropertyMapImplement<?, PropertyInterface> action : actions) {
            CustomClass simpleAdd = action.property.getSimpleAdd();
            if (simpleAdd != null) {
                if (result == null) {
                    result = simpleAdd;
                } else {
                    return null;
                }
            }
        }
        return result;
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        PropertyInterface result = null;
        for (ActionPropertyMapImplement<?, PropertyInterface> action : actions) {
            PropertyInterface simpleDelete = action.mapSimpleDelete();
            if (simpleDelete != null) {
                if (result == null) {
                    result = simpleDelete;
                } else {
                    return null;
                }
            }
        }
        return result;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void addAction(ActionPropertyMapImplement<?, PropertyInterface> action) {
        actions.add(action);
    }

    @Override
    public List<ActionPropertyMapImplement<?, PropertyInterface>> getList() {
        List<ActionPropertyMapImplement<?, PropertyInterface>> result = new ArrayList<ActionPropertyMapImplement<?, PropertyInterface>>();
        for(ActionPropertyMapImplement<?, PropertyInterface> action : actions)
            result.addAll(action.getList());
        return result;
    }
}
