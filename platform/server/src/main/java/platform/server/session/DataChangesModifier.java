package platform.server.session;

import platform.server.logics.property.DataProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.data.query.Join;

import java.util.Map;

public class DataChangesModifier extends TableModifier<DataChangesModifier.UsedChanges> {

    final SessionChanges session;
    final DataChanges changes;

    public DataChangesModifier(TableModifier modifier, DataChanges changes) {
        this.session = modifier.getSession();
        this.changes = changes;
    }

    public SessionChanges getSession() {
        return session;
    }

    protected static class UsedChanges extends TableChanges<UsedChanges> {
        DataChanges changes;

        public UsedChanges(DataProperty property, PropertyChange<ClassPropertyInterface> change) {
            changes = new DataChanges(property, change);
        }

        public UsedChanges() {
            this.changes = new DataChanges();
        }

        @Override
        public boolean hasChanges() {
            return super.hasChanges() || changes.hasChanges();
        }

        @Override
        public void add(UsedChanges add) {
            super.add(add);
            changes = new DataChanges(changes,add.changes);
        }

        @Override
        public boolean equals(Object o) {
            return this==o || o instanceof UsedChanges && changes.equals(((UsedChanges)o).changes) && super.equals(o);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + changes.hashCode();
        }
    }

    public UsedChanges newChanges() {
        return new UsedChanges();
    }

    public UsedChanges used(Property property, UsedChanges usedChanges) {
        PropertyChange<ClassPropertyInterface> dataChange;
        if(property instanceof DataProperty && (dataChange = changes.get((DataProperty) property))!=null)
            usedChanges = new UsedChanges((DataProperty) property, dataChange);
        return usedChanges;
    }

    // переносим DataProperty на выход, нужно на самом деле сделать calculateExpr и if равняется то keyExpr, иначе старое значение но по старому значению будет false
    public <E extends PropertyInterface> Expr changed(Property<E> property, Map<E, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        PropertyChange<ClassPropertyInterface> dataChange;
        if(property instanceof DataProperty && (dataChange = changes.getObject(property))!=null) {
            Join<String> join = dataChange.getQuery("value").join((Map<ClassPropertyInterface,Expr>) joinImplement);
            if(changedWhere !=null) changedWhere.add(join.getWhere());
            return join.getExpr("value");
        } else // иначе не трогаем
            return null;
    }

}
