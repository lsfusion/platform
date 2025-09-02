package lsfusion.server.logics.action.flow;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public abstract class ExtendContextAction<I extends PropertyInterface> extends FlowAction {

    protected final ImSet<I> innerInterfaces;
    protected final ImRevMap<PropertyInterface, I> mapInterfaces;

    public ExtendContextAction(LocalizedString caption, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces) {
        super(caption, mapInterfaces.size());

        this.innerInterfaces = innerInterfaces;
        this.mapInterfaces = getMapInterfaces(mapInterfaces);
    }

    protected ImSet<I> getExtendInterfaces() {
        return innerInterfaces.remove(mapInterfaces.valuesSet());
    }

    @IdentityInstanceLazy
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return IsClassProperty.getMapProperty(mapInterfaces.innerJoin( // по аналогии с группировкой (а точнее вместо) такая "эвристика"
                calcGroupWhereProperty().mapInterfaceClasses(ClassType.wherePolicy)));
    }
    protected abstract PropertyMapImplement<?, I> calcGroupWhereProperty();

    public ActionMapImplement<PropertyInterface, I> getMapImplement() {
        return new ActionMapImplement<>(this, mapInterfaces);
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<I, ? extends ObjectValue> innerValues = mapInterfaces.crossJoin(context.getKeys());
        ImRevMap<I, KeyExpr> innerKeys = KeyExpr.getMapKeys(getExtendInterfaces());
        ImMap<I, Expr> innerExprs = MapFact.addExcl(innerKeys, DataObject.getMapExprs(innerValues));

        FlowResult result = executeExtend(context, innerKeys, innerValues, innerExprs);
        if(result.isThrows())
            throw new RuntimeException("Thread has been interrupted");
        return result;
    }

    protected abstract FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException, SQLHandledException;

    @Override
    public ActionMapImplement<?, PropertyInterface> compile(ImSet<Action<?>> recursiveAbstracts) {
        ActionMapImplement<?, I> extCompile = compileExtend(recursiveAbstracts);
        if(extCompile!=null)
            return extCompile.map(mapInterfaces.reverse());
        return null;
    }

    public ActionMapImplement<?, I> compileExtend(ImSet<Action<?>> recursiveAbstracts) {
        return null;
    }

    @Override
    protected ActionMapImplement<?, PropertyInterface> aspectReplace(ActionReplacer replacer, ImSet<Action<?>> recursiveAbstracts) {
        ActionMapImplement<?, I> extReplace = replaceExtend(replacer, recursiveAbstracts);
        if(extReplace!=null)
            return extReplace.map(mapInterfaces.reverse());
        return null;
    }

    public ActionMapImplement<?, I> replaceExtend(ActionReplacer replacer, ImSet<Action<?>> recursiveAbstracts) {
        return null;
    }
}
