package lsfusion.server.logics.action.session.table;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.builder.Join;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.session.change.Correlation;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class NoPropertyWhereTableUsage<K extends PropertyInterface> extends NoPropertyTableUsage<K> {

    // множественное наследование по сути
    private final PropertyChangeTableUsage.Correlations<K> correlations;

    public NoPropertyWhereTableUsage(ImOrderSet<Correlation<K>> correlations, String debugInfo, ImOrderSet<K> keys, Type.Getter<K> keyType) {
        super(debugInfo, keys, keyType);

        this.correlations = new PropertyChangeTableUsage.Correlations<>(this, correlations);

        initTable(keys);
    }

    @Override
    protected boolean postponeInitTable() {
        return true;
    }

    // множественное наследование
    @Override
    protected ImSet<PropertyField> getFullProps() {
        return correlations.getFullProps(super.getFullProps());
    }

    @Override
    protected IQuery<KeyField, PropertyField> fullMap(IQuery<K, String> query) {
        return correlations.fullMap(super.fullMap(query));
    }

    @Override
    protected Join<String> fullJoin(final Join<PropertyField> join, final ImMap<K, ? extends Expr> joinImplement) {
        return correlations.fullJoin(super.fullJoin(join, joinImplement), join, joinImplement);
    }

    @Override
    public boolean hasCorrelations() {
        return correlations.hasCorrelations();
    }
}
