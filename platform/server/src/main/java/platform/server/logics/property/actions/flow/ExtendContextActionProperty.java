package platform.server.logics.property.actions.flow;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.caches.ManualLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;

import java.sql.SQLException;

public abstract class ExtendContextActionProperty<I extends PropertyInterface> extends FlowActionProperty {

    protected final ImSet<I> innerInterfaces;
    protected final ImRevMap<PropertyInterface, I> mapInterfaces;

    public ExtendContextActionProperty(String sID, String caption, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces) {
        super(sID, caption, mapInterfaces.size());

        this.innerInterfaces = innerInterfaces;
        this.mapInterfaces = getMapInterfaces(mapInterfaces);
    }

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return IsClassProperty.getMapProperty(mapInterfaces.join( // по аналогии с группировкой (а точнее вместо) такая "эвристика"
                getGroupWhereProperty().mapInterfaceClasses(ClassType.FULL)));
    }
    protected abstract CalcPropertyMapImplement<?, I> getGroupWhereProperty();

    public ActionPropertyMapImplement<PropertyInterface, I> getMapImplement() {
        return new ActionPropertyMapImplement<PropertyInterface, I>(this, mapInterfaces);
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        ImMap<I, ? extends ObjectValue> innerValues = mapInterfaces.crossJoin(context.getKeys());
        ImRevMap<I, KeyExpr> innerKeys = KeyExpr.getMapKeys(innerInterfaces.remove(innerValues.keys()));
        ImMap<I, Expr> innerExprs = MapFact.addExcl(innerKeys, DataObject.getMapExprs(innerValues));

        executeExtend(context, innerKeys, innerValues, innerExprs);

        return FlowResult.FINISH;
    }

    protected abstract FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException;

    // потом надо будет вверх реализацию перенести
    private ActionPropertyMapImplement<?, PropertyInterface> compile;
    private boolean compiled;
    @Override
    @ManualLazy
    public ActionPropertyMapImplement<?, PropertyInterface> compile() {
        if(!compiled) {
            ActionPropertyMapImplement<?, I> extCompile = compileExtend();
            if(extCompile!=null)
                compile = extCompile.map(mapInterfaces.reverse());
            compiled = true;
        }
        return compile;
   }

    public ActionPropertyMapImplement<?, I> compileExtend() {
        return null;
    }
}
