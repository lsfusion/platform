package test;

import platform.server.data.sql.SQLSyntax;
import platform.server.data.*;
import platform.server.data.query.*;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.data.types.Type;
import platform.interop.Compare;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.sql.SQLException;

// тестирование Source'ов
class SourceTest {

    SQLSyntax syntax;

    Table Table1;
    KeyField Table1Key1;
    KeyField Table1Key2;
    PropertyField Table1Prop1;
    PropertyField Table1Prop2;
    PropertyField Table1Prop3;
    PropertyField Table1Prop4;

    Table Table2;
    KeyField Table2Key1;
    PropertyField Table2Prop1;
    PropertyField Table2Prop2;

    Table Table3;
    KeyField Table3Key1;
    KeyField Table3Key2;
    PropertyField Table3Prop1;
    PropertyField Table3Prop2;

    Map<KeyField,KeyField> Map3To1;
    Map<KeyField,KeyField> MapBack3To1;

    SourceTest(SQLSyntax iSyntax) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        syntax = iSyntax;

        // Table 1
        Table1 = new Table("table1");
        Table1Key1 = new KeyField("key1", Type.integer);
        Table1.keys.add(Table1Key1);
        Table1Key2 = new KeyField("key2",Type.integer);
        Table1.keys.add(Table1Key2);
        Table1Prop1 = new PropertyField("prop1",Type.integer);
        Table1.properties.add(Table1Prop1);
        Table1Prop2 = new PropertyField("prop2",Type.integer);
        Table1.properties.add(Table1Prop2);
        Table1Prop3 = new PropertyField("prop3",Type.integer);
        Table1.properties.add(Table1Prop3);
        Table1Prop4 = new PropertyField("prop4",Type.integer);
        Table1.properties.add(Table1Prop4);

        // Table 2
        Table2 = new Table("table2");
        Table2Key1 = new KeyField("key1",Type.integer);
        Table2.keys.add(Table2Key1);
        Table2Prop1 = new PropertyField("prop1",Type.integer);
        Table2.properties.add(Table2Prop1);
        Table2Prop2 = new PropertyField("prop2",Type.integer);
        Table2.properties.add(Table2Prop2);

        // Table 3
        Table3 = new Table("table3");
        Table3Key1 = new KeyField("key1", Type.integer);
        Table3.keys.add(Table3Key1);
        Table3Key2 = new KeyField("key2",Type.integer);
        Table3.keys.add(Table3Key2);
        Table3Prop1 = new PropertyField("prop1",Type.integer);
        Table3.properties.add(Table3Prop1);
        Table3Prop2 = new PropertyField("prop2",Type.integer);
        Table3.properties.add(Table3Prop2);

        Map3To1 = new HashMap<KeyField, KeyField>();
        Map3To1.put(Table3Key1,Table1Key1);
        Map3To1.put(Table3Key2,Table1Key2);

        MapBack3To1 = new HashMap<KeyField, KeyField>();
        MapBack3To1.put(Table3Key1,Table1Key2);
        MapBack3To1.put(Table3Key2,Table1Key1);

        System.out.println(test2().getInsertSelect(syntax));
        System.out.println(test1().getInsertSelect(syntax));
        System.out.println(test3().getInsertSelect(syntax));
        System.out.println(test4().getInsertSelect(syntax));
    }

    // просто Union 1-й и 3-й таблицы
    ModifyQuery test1() {
        JoinQuery<KeyField,PropertyField> Join1Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table11Q = new Join<KeyField, PropertyField>(Table1, Join1Q);
        Join1Q.properties.put(Table1Prop1, Table11Q.exprs.get(Table1Prop1));
        Join1Q.and(Table11Q.inJoin);

        JoinQuery<KeyField,PropertyField> Join2Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table32Q = new Join<KeyField, PropertyField>(Table3, Map3To1, Join2Q);
        Join2Q.properties.put(Table1Prop1, Table32Q.exprs.get(Table3Prop1));
        Join2Q.and(Table32Q.inJoin);

        JoinQuery<KeyField,PropertyField> Join3Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table33Q = new Join<KeyField, PropertyField>(Table3, MapBack3To1, Join3Q);
        Join3Q.properties.put(Table1Prop1, Table33Q.exprs.get(Table3Prop2));
        Join3Q.and(Table33Q.inJoin);

        OperationQuery<KeyField,PropertyField> ResultQ = new OperationQuery<KeyField,PropertyField>(Table1.keys, Union.SUM);
        ResultQ.add(Join1Q,1);
        ResultQ.add(Join2Q,1);
        ResultQ.add(Join3Q,1);

        return new ModifyQuery(Table1,ResultQ);
    }

    // просто 2-ю с первой и поле второй не null
    ModifyQuery test3() {
        JoinQuery<KeyField,PropertyField> Result = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table1J = new Join<KeyField, PropertyField>(Table1, Result);

        Join<KeyField, PropertyField> Table2J = new Join<KeyField, PropertyField>(Table2);
        Table2J.joins.put(Table2Key1, Table1J.exprs.get(Table1Prop1));
        Result.properties.put(Table1Prop1, Table2J.exprs.get(Table2Prop2));
        Result.and(Table2J.exprs.get(Table2Prop2).getWhere());

        return new ModifyQuery(Table1,Result);
    }

    // 2 U(J(U(таблицы2 с prop2=1 и 2-м ключом таблицы1=5,таблица1),Table3),J(Table1,Table3))
    // последний Join должен уйти
    ModifyQuery test2() {
        OperationQuery<KeyField,PropertyField> UnionQ = new OperationQuery<KeyField,PropertyField>(Table1.keys,Union.OVERRIDE);

        // 1-й запрос
        JoinQuery<KeyField,PropertyField> JoinQuery = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField,PropertyField> TableJoin = new Join<KeyField, PropertyField>(Table2);
        TableJoin.joins.put(Table2Key1,JoinQuery.mapKeys.get(Table1Key1));
        JoinQuery.putKeyWhere(Collections.singletonMap(Table1Key2,5));
        JoinQuery.properties.put(Table2Prop1, TableJoin.exprs.get(Table2Prop1));
        JoinQuery.and(new CompareWhere(TableJoin.exprs.get(Table2Prop2),Type.integer.getExpr(1), Compare.EQUALS));
        UnionQ.add(JoinQuery,1);

        UnionQ.add(Table1,1);

        JoinQuery<KeyField,PropertyField> Join1Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Union1Q = new Join<KeyField, PropertyField>(UnionQ, Join1Q);
        Join1Q.properties.putAll(Union1Q.exprs);
        Join1Q.and(Union1Q.inJoin);
        Join<KeyField, PropertyField> Table31Q = new Join<KeyField, PropertyField>(Table3, Map3To1, Join1Q);
        Join1Q.properties.putAll(Table31Q.exprs);

/*        JoinQuery<KeyField,PropertyField> Join2Q = new JoinQuery<KeyField,PropertyField>(Table1.Keys);
        Join<KeyField, PropertyField> Table12Q = new Join<KeyField, PropertyField>(Table1, Join2Q);
        Join2Q.addAll(Table12Q.Exprs);
        Join2Q.add(Table12Q.InJoin);
        Join<KeyField, PropertyField> Table32Q = new Join<KeyField, PropertyField>(Table3, Map3To1, Join2Q);
        Join2Q.addAll(Table32Q.Exprs);
        Join2Q.add(Table32Q.InJoin);

        UnionQuery<KeyField,PropertyField> ResultQ = new UnionQuery<KeyField,PropertyField>(Table1.Keys,1);
        ResultQ.add(Join1Q,1);
        ResultQ.add(Join2Q,1);
  */
        return new ModifyQuery(Table1,Join1Q);
    }

    // первую группируем по полю 1, 2-й по полю 2
    ModifyQuery test4() {
        JoinQuery<KeyField,PropertyField> Join1Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table11Q = new Join<KeyField, PropertyField>(Table1, Join1Q);
        Join1Q.properties.put(Table1Prop1, Table11Q.exprs.get(Table1Prop1));
        Join1Q.properties.put(Table1Prop2, Table11Q.exprs.get(Table1Prop2));
//        Join1Q.and(Table11Q.inJoin);

        GroupQuery<PropertyField,PropertyField,PropertyField,KeyField> Group1Q = new GroupQuery<PropertyField, PropertyField, PropertyField, KeyField>(Collections.singleton(Table1Prop1),
            Join1Q,Table1Prop2,1);

        JoinQuery<KeyField,PropertyField> Join2Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table12Q = new Join<KeyField, PropertyField>(Table1, Join2Q);
        Join2Q.properties.put(Table1Prop1, Table12Q.exprs.get(Table1Prop1));
        Join2Q.properties.put(Table1Prop3, Table12Q.exprs.get(Table1Prop3));
//        Join2Q.and(Table12Q.inJoin);

        GroupQuery<PropertyField,PropertyField,PropertyField,KeyField> Group2Q = new GroupQuery<PropertyField, PropertyField, PropertyField, KeyField>(Collections.singleton(Table1Prop1),
            Join2Q,Table1Prop3,1);

        OperationQuery<PropertyField,PropertyField> UnionQ = new OperationQuery<PropertyField,PropertyField>(Collections.singleton(Table1Prop1),Union.OVERRIDE);
        UnionQ.add(Group1Q,1);
        UnionQ.add(Group2Q,1);

        JoinQuery<KeyField,PropertyField> ModiQ = new JoinQuery<KeyField,PropertyField>(Table2.keys);
        Join<PropertyField,PropertyField> JoinUnion = new Join<PropertyField,PropertyField>(UnionQ);
        JoinUnion.joins.put(Table1Prop1,ModiQ.mapKeys.get(Table2Key1));
        ModiQ.properties.putAll(JoinUnion.exprs);
        ModiQ.and(JoinUnion.inJoin);

        return new ModifyQuery(Table2,ModiQ);
    }
}
