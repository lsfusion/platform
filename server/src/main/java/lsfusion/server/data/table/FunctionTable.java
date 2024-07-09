package lsfusion.server.data.table;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.expr.formula.CustomFormulaSyntax;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.TableStatKeys;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.classes.data.integral.IntegerClass;

public class FunctionTable extends Table {

    private final CustomFormulaSyntax formula;
    private final ImRevMap<String, KeyField> params;

    public FunctionTable(CustomFormulaSyntax formula, ImOrderSet<KeyField> keys, ImRevMap<String, KeyField> params) {
        super(keys);

        this.formula = formula;
        this.params = params;
    }

    @IdentityLazy
    public ClassWhere<KeyField> getClasses() {
        return getClassWhere(this);
    }

    @IdentityLazy
    public ClassWhere<Field> getClassWhere(PropertyField property) {
        return getClassWhere(this, property);
    }

    @IdentityLazy
    public TableStatKeys getTableStatKeys() {
        return getStatKeys(this, 100);
//        return ImplementTable.ignoreStatPropsNoException(() -> getStatKeys(this, 100));
    }

    @IdentityLazy
    public PropStat getStatProp(PropertyField property) {
        return getStatProp(this, property);
//        return ImplementTable.ignoreStatPropsNoException(() -> getStatProp(this, property));
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return formula.equals(((FunctionTable) o).formula) && params.equals(((FunctionTable) o).params);
    }

    public int immutableHashCode() {
        return 31 * formula.hashCode() + params.hashCode();
    }

    @Override
    public String getQuerySource(CompileSource source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getQuerySource(CompileSource source, ImMap<KeyField, String> lateralSources) {
        String querySource = formula.getSource(source.syntax, params.join(lateralSources));

        if(params.size() == keys.size() - 1) {
            KeyField key = keys.removeOrderIncl(params.valuesSet()).single();
            if(key.getName().equals("row") && key.type.equals(IntegerClass.instance))
                return "(SELECT ROW_NUMBER() OVER () AS row, t.* FROM " + querySource + " t)";
        }

        return querySource;
    }

    @Override
    public ImSet<KeyField> getLaterals() {
        return params.valuesSet();
    }
}
