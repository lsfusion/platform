package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.server.classes.ObjectValueClassSet;
import lsfusion.server.classes.ValueClassSet;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.Table;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.NotNullExprInterface;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.property.IsClassField;
import lsfusion.server.logics.table.FullTablesInterface;
import lsfusion.server.logics.table.ImplementTable;

public class InnerFollows<K> extends TwinImmutableObject {

    protected final ImMap<K, ImSet<IsClassField>> fields; // потом можно дообобщить до PropertyField'ов или даже ImSet<ImMap<PropertyField, K>>, RecursionGuard нужен будет ?
    protected InnerFollows(ImMap<K, ImSet<IsClassField>> fields) {
        this.fields = fields;
    }

    private final static InnerFollows EMPTY = new InnerFollows(MapFact.EMPTY());
    public static <K> InnerFollows<K> EMPTY() {
        return EMPTY;
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        return fields.equals(((InnerFollows)o).fields);
    }

    public int immutableHashCode() {
        return fields.hashCode();
    }

    private static <K> ImMap<K, ImSet<IsClassField>> getIsClassFields(ClassWhere<K> classWhere, ImSet<K> keys, Table skipTable) {
        ImMap<K, AndClassSet> commonClasses = classWhere.getCommonClasses(keys);
        ImFilterValueMap<K, ImSet<IsClassField>> classFields = commonClasses.mapFilterValues();
        for(int i=0,size=commonClasses.size();i<size;i++) {
            AndClassSet andClassSet = commonClasses.getValue(i);
            ValueClassSet valueClassSet = andClassSet.getValueClassSet();
            if(BaseUtils.hashEquals(andClassSet, valueClassSet) && valueClassSet instanceof ObjectValueClassSet && !valueClassSet.isEmpty()) { // нет unknown'ов и работа с Object'ами идет
                ObjectValueClassSet objectClassSet = (ObjectValueClassSet) valueClassSet;
                FullTablesInterface ift = objectClassSet.getBaseClass().fullTables;

                MExclSet<IsClassField> mFollowFields = SetFact.mExclSet();

//                // class fields
//                ImRevMap<ClassField, ObjectValueClassSet> tables = objectClassSet.getObjectClassFields();
//                if(tables.size() == 1) {
//                    ClassField classField = tables.singleKey();
//                    ImplementTable table = classField.getTable();
//                    assert classField.getObjectSet().containsAll(andClassSet, false);
//                    if(skipTable == null || !BaseUtils.hashEquals(table, skipTable)) {
//                        boolean recursionGuard = skipTable instanceof ImplementTable && ift.getFullTables(classField.getObjectSet(), classField.getTable()).contains((ImplementTable) skipTable);
//                        if(!recursionGuard)
//                            mFollowFields.exclAdd(classField);
//                    }
//                }

                // full tables
                // теоретически classField может получить более общий класс поэтому все равно надо проверять, для "промежуточных" ImplementTable classField все равно будет из многих таблиц состоять поэтоиу в обратную сторону можно не проверять
                ImSet<ImplementTable> fullTables = ift.getFullTables(objectClassSet, skipTable instanceof ImplementTable ? (ImplementTable) skipTable : null);
                for (ImplementTable fullTable : fullTables) {
                    assert fullTable.isFull();
                    mFollowFields.exclAdd(fullTable.getFullField());
                }

                ImSet<IsClassField> followFields = mFollowFields.immutable();
                if(!followFields.isEmpty())
                    classFields.mapValue(i, followFields);
            }
        }
        return classFields.immutableValue();
    }
    public InnerFollows(ClassWhere<K> classWhere, ImSet<K> keys, Table skipTable) {
        this(getIsClassFields(classWhere, keys, skipTable));
    }

    // должен быть "синхронизирован" с hasExprFollowsNotNull иначе кэши поплывут для чего последний собсно и сделан
    public ImSet<NotNullExprInterface> getExprFollows(ImMap<K, BaseExpr> joins, boolean includeInnerWithoutNotNull, boolean recursive) {
        MSet<NotNullExprInterface> set = SetFact.mSet();
        for(int i=0,size=joins.size();i<size;i++) {
            BaseExpr joinExpr = joins.getValue(i);
            ImSet<IsClassField> followFields;
            if(includeInnerWithoutNotNull && (followFields = fields.get(joins.getKey(i))) != null) {
                assert !followFields.isEmpty();
                for(IsClassField followField : followFields) {
                    set.addAll(followField.getFollowExpr(joinExpr).getExprFollows(true, includeInnerWithoutNotNull, recursive));
                }
            } else
                set.addAll(joinExpr.getExprFollows(true, includeInnerWithoutNotNull, recursive));
        }
        return set.immutable();
    }

    public boolean hasExprFollowsWithoutNotNull(ImMap<K, BaseExpr> joins) {
        for(int i=0,size=joins.size();i<size;i++) {
            BaseExpr joinExpr = joins.getValue(i);
            if(fields.get(joins.getKey(i)) != null) { // !!! исправить
                return true; // уже есть отличие с follows
            }
            if(joinExpr.hasExprFollowsWithoutNotNull())
                return true;
        }
        return false;
    }
}
