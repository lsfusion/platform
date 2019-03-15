package lsfusion.server.logics.action.flow;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.infer.ClassType;
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
        ImRevMap<I, KeyExpr> innerKeys = KeyExpr.getMapKeys(innerInterfaces.remove(innerValues.keys()));
        ImMap<I, Expr> innerExprs = MapFact.addExcl(innerKeys, DataObject.getMapExprs(innerValues));

        FlowResult result = executeExtend(context, innerKeys, innerValues, innerExprs);
        if(result == FlowResult.THROWS)
            throw new RuntimeException("Thread has been interrupted");
        return result;
    }

    protected abstract FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException, SQLHandledException;

    // потом надо будет вверх реализацию перенести
    private ActionMapImplement<?, PropertyInterface> compile;
    private boolean compiled;
    @Override
    @ManualLazy
    public ActionMapImplement<?, PropertyInterface> compile() {
        if(!compiled) {
            ActionMapImplement<?, I> extCompile = compileExtend();
            if(extCompile!=null)
                compile = extCompile.map(mapInterfaces.reverse());
            compiled = true;
        }
        return compile;
   }

    public ActionMapImplement<?, I> compileExtend() {
        return null;
    }
}
