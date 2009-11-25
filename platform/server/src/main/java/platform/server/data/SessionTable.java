package platform.server.data;

import platform.base.BaseUtils;
import platform.server.classes.ConcreteClass;
import platform.server.data.where.classes.ClassWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.data.SQLSession;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// временная таблица на момент сессии
public abstract class SessionTable<This extends SessionTable> extends Table {

    protected SessionTable(String iName) {
        super(iName);
    }

    protected SessionTable(String iName,ClassWhere<KeyField> iClasses,Map<PropertyField,ClassWhere<Field>> iPropertyClasses) {
        super(iName,iClasses,iPropertyClasses);
    }

    public String getName(SQLSyntax Syntax) {
        return Syntax.getSessionTableName(name);
    }

    public abstract This createThis(ClassWhere<KeyField> iClasses,Map<PropertyField,ClassWhere<Field>> iPropertyClasses);

    public This insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update) throws SQLException {

        Map<KeyField, ConcreteClass> keyClasses = DataObject.getMapClasses(keyFields);

        Map<PropertyField, ClassWhere<Field>> orPropertyClasses = new HashMap<PropertyField, ClassWhere<Field>>(); 
        for(Map.Entry<PropertyField,ObjectValue> propertyField : propFields.entrySet()) {
            ClassWhere<Field> existedPropertyClasses = propertyClasses.get(propertyField.getKey());
            if(propertyField.getValue() instanceof DataObject) {
                ClassWhere<Field> insertClasses = new ClassWhere<Field>(BaseUtils.merge(keyClasses,
                        Collections.singletonMap(propertyField.getKey(),((DataObject)propertyField.getValue()).objectClass)));
                if(existedPropertyClasses!=null)
                    insertClasses = insertClasses.or(existedPropertyClasses);
                orPropertyClasses.put(propertyField.getKey(),insertClasses);
            } else
                orPropertyClasses.put(propertyField.getKey(),existedPropertyClasses!=null?existedPropertyClasses:ClassWhere.<Field>STATIC(false));
        }

        if(update)
            session.updateInsertRecord(this,keyFields,propFields);
        else
            session.insertRecord(this,keyFields,propFields);
        
        return createThis(classes.or(new ClassWhere<KeyField>(keyClasses)), orPropertyClasses);
    }

    public This writeKeys(SQLSession session,List<Map<KeyField,DataObject>> rows) throws SQLException {
        session.deleteKeyRecords(this, new HashMap<KeyField, Integer>());

        ClassWhere<KeyField> writeClasses = new ClassWhere<KeyField>();
        for(Map<KeyField, DataObject> row : rows) {
            writeClasses = writeClasses.or(new ClassWhere<KeyField>(DataObject.getMapClasses(row)));
            session.insertRecord(this,row,new HashMap<PropertyField, ObjectValue>());
        }

        return createThis(writeClasses, new HashMap<PropertyField, ClassWhere<Field>>());
    }
}
