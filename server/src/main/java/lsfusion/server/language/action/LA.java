package lsfusion.server.language.action;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.flow.CaseAction;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.ListAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.ActionOrPropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.ActionOrPropertyUtils;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.List;

public class LA<T extends PropertyInterface> extends LAP<T, Action<T>> {

    public Action<T> action;

    @Override
    public Action<T> getActionOrProperty() {
        return action;
    }

    public LA(Action<T> action) {
        super(action);
        this.action = action;
    }

    public LA(Action<T> action, ImOrderSet<T> listInterfaces) {
        super(action, listInterfaces);
        this.action = action;
    }

    public void execute(DataSession session, ExecutionStack stack, ObjectValue... objects) throws SQLException, SQLHandledException {
        execute((ExecutionEnvironment)session, stack, objects);
    }

    public void execute(ExecutionEnvironment session, ExecutionStack stack, ObjectValue... objects) throws SQLException, SQLHandledException {
        execute(session, stack, null, objects);
    }

    public void execute(ExecutionEnvironment session, ExecutionStack stack, FormEnvironment<T> formEnv, ObjectValue... objects) throws SQLException, SQLHandledException {
        action.execute(getMapValues(objects), session, stack, formEnv);
    }

    public FlowResult execute(ExecutionContext<?> context, ObjectValue... objects) throws SQLException, SQLHandledException {
        return action.execute(context.override(getMapValues(objects), (FormEnvironment<T>) null));
    }

    public <X extends PropertyInterface> FlowResult execute(ExecutionContext<X> context) throws SQLException, SQLHandledException {
        return action.execute(BaseUtils.immutableCast(context.override(MapFact.EMPTY())));
    }

    public ValueClass[] getInterfaceClasses() { // obsolete
        return listInterfaces.mapList(action.getInterfaceClasses(ClassType.obsolete)).toArray(new ValueClass[listInterfaces.size()]); // тут все равно obsolete
    }

    public ValueClass[] getInterfaceClasses(ClassType classType) {
        return action.getInterfaceClasses(listInterfaces, classType);
    }

    public <U extends PropertyInterface> ActionMapImplement<T, U> getImplement(U... mapping) {
        return new ActionMapImplement<>(action, getRevMap(mapping));
    }
    public <U extends PropertyInterface> ActionMapImplement<T, U> getImplement(ImOrderSet<U> mapping) {
        return new ActionMapImplement<>(action, getRevMap(mapping));
    }

    public <P extends PropertyInterface> void addToContextMenuFor(LAP<P, ActionOrProperty<P>> mainProperty, LocalizedString contextMenuCaption) {
        mainProperty.getActionOrProperty().setContextMenuAction(action.getSID(), contextMenuCaption);
    }

    public <P extends PropertyInterface> void setAsEventActionFor(String actionSID, LAP<P, ActionOrProperty<P>> mainProperty) {
        assert listInterfaces.size() <= mainProperty.listInterfaces.size();

        //мэпим входы по порядку, у этого экшна входов может быть меньше
        ActionMapImplement<T, P> actionImplement = new ActionMapImplement<>(action, getRevMap(mainProperty.listInterfaces));

        mainProperty.getActionOrProperty().setEventAction(actionSID, actionImplement);
    }

    public void addOperand(boolean hasWhen, List<ResolveClassSet> signature, boolean optimisticAsync, Version version, Object... params) {
        ImList<ActionOrPropertyInterfaceImplement> readImplements = ActionOrPropertyUtils.readImplements(listInterfaces, params);
        ActionMapImplement<?, PropertyInterface> actImpl = (ActionMapImplement<?, PropertyInterface>)readImplements.get(0);
        if (action instanceof ListAction) {
            ((ListAction) action).addAction(actImpl, version);
        } else if (hasWhen) {
            ((CaseAction) action).addCase((PropertyMapImplement<?, PropertyInterface>)readImplements.get(1), actImpl, optimisticAsync, version);
        } else {
            ((CaseAction) action).addOperand(actImpl, signature, optimisticAsync, version);
        }
    }
}
