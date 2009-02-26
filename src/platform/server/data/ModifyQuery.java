package platform.server.data;

import platform.server.data.query.CompiledQuery;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class ModifyQuery {
    Table table;
    JoinQuery<KeyField, PropertyField> change;

    public ModifyQuery(Table iTable, JoinQuery<KeyField, PropertyField> iChange) {
        table = iTable;
        change = iChange;
    }

    public String getUpdate(SQLSyntax Syntax) {

        int UpdateModel = Syntax.updateModel();
        if(UpdateModel==2) {
            // Oracl'вская модель Update'а
            Map<KeyField,String> KeySelect = new HashMap<KeyField, String>();
            Map<PropertyField,String> PropertySelect = new HashMap<PropertyField, String>();
            Collection<String> WhereSelect = new ArrayList<String>();
            CompiledQuery<KeyField, PropertyField> ChangeCompile = change.compile(Syntax);
            String FromSelect = ChangeCompile.fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);

            for(KeyField Key : table.keys)
                WhereSelect.add(table.getName(Syntax)+"."+Key.Name+"="+KeySelect.get(Key));

            List<KeyField> KeyOrder = new ArrayList<KeyField>();
            List<PropertyField> PropertyOrder = new ArrayList<PropertyField>();
            String SelectString = Syntax.getSelect(FromSelect, Source.stringExpr(
                    Source.mapNames(KeySelect,ChangeCompile.keyNames,KeyOrder),
                    Source.mapNames(PropertySelect,ChangeCompile.propertyNames,PropertyOrder)),
                    Source.stringWhere(WhereSelect),"","","");

            String SetString = "";
            for(KeyField Field : KeyOrder)
                SetString = (SetString.length()==0?"":SetString+",") + Field.Name;
            for(PropertyField Field : PropertyOrder)
                SetString = (SetString.length()==0?"":SetString+",") + Field.Name;

            return "UPDATE " + table.getName(Syntax) + " SET ("+SetString+") = ("+SelectString+") WHERE EXISTS ("+SelectString+")";
        } else {
            Map<KeyField,String> KeySelect = new HashMap<KeyField, String>();
            Map<PropertyField,String> PropertySelect = new HashMap<PropertyField, String>();
            Collection<String> WhereSelect = new ArrayList<String>();

            String WhereString = "";
            String FromSelect;

            if(UpdateModel==1) {
                // SQL-серверная модель когда она подхватывает первый Join и старую таблицу уже не вилит
                // построим JoinQuery куда переJoin'им все эти поля (оптимизатор уберет все дублирующиеся таблицы)
                JoinQuery<KeyField, PropertyField> UpdateQuery = new JoinQuery<KeyField, PropertyField>(table.keys);
                Join<KeyField, PropertyField> TableJoin = new Join<KeyField, PropertyField>(table, UpdateQuery);
                TableJoin.noAlias = true;
                UpdateQuery.and(TableJoin.inJoin);

                Join<KeyField, PropertyField> ChangeJoin = new Join<KeyField, PropertyField>(change, UpdateQuery);
                UpdateQuery.and(ChangeJoin.inJoin);
                for(PropertyField ChangeField : change.properties.keySet())
                    UpdateQuery.properties.put(ChangeField, ChangeJoin.exprs.get(ChangeField));
                FromSelect = UpdateQuery.compile(Syntax).fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);
            } else {
                FromSelect = change.compile(Syntax).fillSelect(KeySelect, PropertySelect, WhereSelect, Syntax);

                for(KeyField Key : table.keys)
                    WhereSelect.add(table.getName(Syntax)+"."+Key.Name+"="+KeySelect.get(Key));
            }

            for(String Where : WhereSelect)
                WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;

            String SetString = "";
            for(Map.Entry<PropertyField,String> SetProperty : PropertySelect.entrySet())
                SetString = (SetString.length()==0?"":SetString+",") + SetProperty.getKey().Name + "=" + SetProperty.getValue();

            return "UPDATE " + Syntax.getUpdate(table.getName(Syntax)," SET "+SetString,FromSelect,(WhereString.length()==0?"":" WHERE "+WhereString));
        }
    }

    public String getInsertLeftKeys(SQLSyntax syntax) {

        // делаем для этого еще один запрос
        JoinQuery<KeyField, PropertyField> leftKeysQuery = new JoinQuery<KeyField, PropertyField>(table.keys);
        // при Join'им ModifyQuery
        leftKeysQuery.and(new Join<KeyField, PropertyField>(change,leftKeysQuery).inJoin);
        // исключим ключи которые есть
        leftKeysQuery.and((new Join<KeyField, PropertyField>(table,leftKeysQuery)).inJoin.not());

        return (new ModifyQuery(table,leftKeysQuery)).getInsertSelect(syntax);
    }

    public String getInsertSelect(SQLSyntax Syntax) {

        CompiledQuery<KeyField, PropertyField> ChangeCompile = change.compile(Syntax);

        String InsertString = "";
        for(KeyField KeyField : ChangeCompile.keyOrder)
            InsertString = (InsertString.length()==0?"":InsertString+",") + KeyField.Name;
        for(PropertyField PropertyField : ChangeCompile.propertyOrder)
            InsertString = (InsertString.length()==0?"":InsertString+",") + PropertyField.Name;

        return "INSERT INTO " + table.getName(Syntax) + " (" + InsertString + ") " + ChangeCompile.getSelect(Syntax);
    }

    void outSelect(DataSession Session) throws SQLException {
        System.out.println("Table");
        table.outSelect(Session);
        System.out.println("Source");
        change.outSelect(Session);
    }
}
