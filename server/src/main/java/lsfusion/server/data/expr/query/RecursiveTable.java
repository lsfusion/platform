package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.exec.materialize.NotMaterializable;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.TableStatKeys;
import lsfusion.server.data.table.*;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.classes.data.DataClass;

public class RecursiveTable extends Table implements NotMaterializable {

    protected String name;

    private final TableStatKeys statKeys;
    protected final ClassWhere<KeyField> classes;
    
    public final boolean noInnerFollows;
    
    public RecursiveTable(String name, ImSet<KeyField> keys, ClassWhere<KeyField> classes, TableStatKeys statKeys, boolean noInnerFollows) {
        super(keys.sort());

        this.name = name;

        this.statKeys = statKeys;
        this.classes = classes;
        this.noInnerFollows = noInnerFollows;
    }

    public ClassWhere<KeyField> getClasses() {
        return classes;
    }

    public TableStatKeys getTableStatKeys() {
        return statKeys;
    }

    @IdentityLazy
    public ClassWhere<Field> getClassWhere(PropertyField property) {
        return getClassWhere(this, property);
    }

    @Override
    public boolean calcTwins(TwinImmutableObject o) {
        return name.equals(((RecursiveTable) o).name) && classes.equals(((RecursiveTable)o).classes) && statKeys.equals(((RecursiveTable)o).statKeys) && noInnerFollows == ((RecursiveTable) o).noInnerFollows;
    }

    @Override
    public int immutableHashCode() {
        return 31 * (31 * (31 * (31 * name.hashCode()) + statKeys.hashCode()) + classes.hashCode()) + (noInnerFollows ? 1 : 0);
    }

    public String toString() {
        return name;
    }

    @IdentityLazy
    public PropStat getStatProp(PropertyField property) {
        return getStatProp(this, property);
    }

    public String getQuerySource(CompileSource source) {
        source.env.addNotMaterializable(this);

        return name;
    }
}
