package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.*;

public abstract class ExtendContextActionProperty<I extends PropertyInterface> extends FlowActionProperty {

    protected final Collection<I> innerInterfaces;
    protected final Map<PropertyInterface, I> mapInterfaces;

    public ExtendContextActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces) {
        super(sID, caption, mapInterfaces.size());

        this.innerInterfaces = innerInterfaces;
        this.mapInterfaces = getMapInterfaces(mapInterfaces);
    }

    @IdentityLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return IsClassProperty.getMapProperty(BaseUtils.join(mapInterfaces, // по аналогии с группировкой (а точнее вместо) такая "эвристика"
                getGroupWhereProperty().mapInterfaceCommonClasses(null)));
    }
    protected abstract CalcPropertyMapImplement<?, I> getGroupWhereProperty();

    public ActionPropertyMapImplement<PropertyInterface, I> getMapImplement() {
        return new ActionPropertyMapImplement<PropertyInterface, I>(this, mapInterfaces);
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        Map<I, DataObject> innerValues = crossJoin(mapInterfaces, context.getKeys());
        Map<I, KeyExpr> innerKeys = KeyExpr.getMapKeys(filterNot(innerInterfaces, innerValues.keySet()));
        Map<I, Expr> innerExprs = BaseUtils.merge(innerKeys, DataObject.getMapExprs(innerValues));

        executeExtend(context, innerKeys, innerValues, innerExprs);

        return FlowResult.FINISH;
    }

    protected abstract FlowResult executeExtend(ExecutionContext<PropertyInterface> context, Map<I, KeyExpr> innerKeys, Map<I, DataObject> innerValues, Map<I, Expr> innerExprs) throws SQLException;

    // потом надо будет вверх реализацию перенести
    private ActionPropertyMapImplement<?, PropertyInterface> compile;
    private boolean compiled;
    @Override
    @ManualLazy
    public ActionPropertyMapImplement<?, PropertyInterface> compile() {
        if(!compiled) {
            ActionPropertyMapImplement<?, I> extCompile = compileExtend();
            if(extCompile!=null)
                compile = extCompile.map(reverse(mapInterfaces));
            compiled = true;
        }
        return compile;
   }

    public ActionPropertyMapImplement<?, I> compileExtend() {
        return null;
    }
}
