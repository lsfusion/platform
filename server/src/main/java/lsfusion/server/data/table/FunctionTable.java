package lsfusion.server.data.table;

import lsfusion.base.BaseUtils;
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
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.classes.data.time.TimeClass;

public class FunctionTable extends Table {

    private final CustomFormulaSyntax formula;
    private final ImRevMap<String, KeyField> params;

    private final Type onlyKey;

    public FunctionTable(CustomFormulaSyntax formula, ImOrderSet<KeyField> keys, ImRevMap<String, KeyField> params, Type onlyKey) {
        super(keys);

        this.formula = formula;
        this.params = params;

        this.onlyKey = onlyKey;
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
        return formula.equals(((FunctionTable) o).formula) && params.equals(((FunctionTable) o).params) && BaseUtils.nullEquals(onlyKey, ((FunctionTable) o).onlyKey);
    }

    public int immutableHashCode() {
        return 31 * (31 * formula.hashCode() + params.hashCode()) + BaseUtils.nullHash(onlyKey);
    }

    @Override
    public String getQuerySource(CompileSource source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getQuerySource(String alias, CompileSource source, ImMap<KeyField, String> lateralSources) {
        String querySource = formula.getSource(source.syntax, params.join(lateralSources));

        String fieldAliases = "";
        if (params.size() == keys.size() - 1) {
            KeyField key = keys.removeOrderIncl(params.valuesSet()).single();
            // maybe with ordinality could be used, but we don't know all the columns
            if (key.getName().equals("row") && key.type.equals(IntegerClass.instance))
                return "(SELECT ROW_NUMBER() OVER () AS row, value.* FROM " + querySource + " value)" + " " + alias;

            Type onlyKey = this.onlyKey;
            if(onlyKey != null) { // generate_series has really odd behaviour with column names - column gets alias as its name
                String keyName = key.getName(source.syntax);

                if(onlyKey instanceof DateClass || onlyKey instanceof TimeClass)
                    return "(SELECT " + onlyKey.getCast("q.value", source.syntax, source.env) + " AS " + keyName + " FROM " + querySource + " q(value))" + " " + alias;

                fieldAliases = "(" + keyName + ")";
            }
        }

        return querySource + " " + alias + fieldAliases;
    }

    @Override
    public ImSet<KeyField> getLaterals() {
        return params.valuesSet();
    }
}
