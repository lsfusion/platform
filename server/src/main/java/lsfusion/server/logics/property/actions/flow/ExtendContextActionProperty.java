package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.*;

import java.sql.SQLException;

public abstract class ExtendContextActionProperty<I extends PropertyInterface> extends FlowActionProperty {

    protected final ImSet<I> innerInterfaces;
    protected final ImRevMap<PropertyInterface, I> mapInterfaces;

    public ExtendContextActionProperty(LocalizedString caption, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces) {
        super(caption, mapInterfaces.size());

        this.innerInterfaces = innerInterfaces;
        this.mapInterfaces = getMapInterfaces(mapInterfaces);
    }

    protected ImSet<I> getExtendInterfaces() {
        return innerInterfaces.remove(mapInterfaces.valuesSet());
    }

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return IsClassProperty.getMapProperty(mapInterfaces.innerJoin( // по аналогии с группировкой (а точнее вместо) такая "эвристика"
                calcGroupWhereProperty().mapInterfaceClasses(ClassType.wherePolicy)));
    }
    protected abstract CalcPropertyMapImplement<?, I> calcGroupWhereProperty();

    public ActionPropertyMapImplement<PropertyInterface, I> getMapImplement() {
        return new ActionPropertyMapImplement<>(this, mapInterfaces);
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<I, ? extends ObjectValue> innerValues = mapInterfaces.crossJoin(context.getKeys());
        ImRevMap<I, KeyExpr> innerKeys = KeyExpr.getMapKeys(innerInterfaces.remove(innerValues.keys()));
        ImMap<I, Expr> innerExprs = MapFact.addExcl(innerKeys, DataObject.getMapExprs(innerValues));

        FlowResult result = executeExtend(context, innerKeys, innerValues, innerExprs);
        if(result == FlowResult.THROWS)
            throw new RuntimeException("Thread has been interrupted by user");
        return result;
    }

    protected abstract FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException, SQLHandledException;

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

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return false;
    }
}
