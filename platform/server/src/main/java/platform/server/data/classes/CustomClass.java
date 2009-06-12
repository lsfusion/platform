package platform.server.data.classes;

import platform.server.data.classes.where.ConcreteCustomClassSet;
import platform.server.data.classes.where.CustomClassSet;
import platform.server.data.classes.where.UpClassSet;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.types.ObjectType;
import platform.server.data.types.Type;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.data.ObjectTable;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.groups.AbstractNode;
import platform.server.session.SQLSession;
import platform.server.view.navigator.NavigatorElement;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public abstract class CustomClass extends AbstractNode implements ObjectClass, ValueClass {

    public Type getType() {
        return ObjectType.instance;
    }

    final Collection<CustomClass> parents;
    public final List<CustomClass> children;

    public String toString() {
        return caption;
    }

    public String caption;
    public Integer ID;
    public CustomClass(Integer iID, String iCaption, CustomClass... iParents) {
        ID = iID;
        caption = iCaption;
        parents = new ArrayList<CustomClass>();
        children = new ArrayList<CustomClass>();

        for (CustomClass parent : iParents) addParent(parent);
    }

    public void saveClassChanges(SQLSession session, DataObject value) throws SQLException {
        ObjectTable classTable = getBaseClass().table;
        session.updateInsertRecord(classTable,Collections.singletonMap(classTable.key,value),
                Collections.singletonMap(classTable.objectClass,(ObjectValue)new DataObject(ID, SystemClass.instance)));
    }

    public void addParent(CustomClass parentClass) {
        // проверим что в Parent'ах нету этого класса
        for(CustomClass parent : parents)
            if(parent.isChild(parentClass)) return;

        Iterator<CustomClass> i = parents.iterator();
        while(i.hasNext())
            if(parentClass.isChild(i.next())) i.remove();

        parents.add(parentClass);
        parentClass.children.add(this);
    }

    public UpClassSet getUpSet() {
        return new UpClassSet(this);
    }

    public BaseClass getBaseClass() {
        return parents.iterator().next().getBaseClass();
    }

    public boolean isChild(CustomClass parentClass) {
        if(parentClass==this) return true;

        for(CustomClass parent : parents)
            if (parent.isChild(parentClass)) return true;

        return false;
    }

    public boolean isCompatibleParent(ValueClass remoteClass) {
        return remoteClass instanceof CustomClass && ((CustomClass)remoteClass).isChild(this);
    }

    public CustomClass findClassID(int idClass) {
        if(ID.equals(idClass)) return this;

        for(CustomClass child : children) {
            CustomClass findClass = child.findClassID(idClass);
            if(findClass!=null) return findClass;
        }

        return null;
    }

    public ConcreteCustomClass findConcreteClassID(int idClass) {
        return (ConcreteCustomClass) findClassID(idClass);
    }

    public CustomClassSet commonParents(CustomClass toCommon) {
        commonClassSet1(true);
        toCommon.commonClassSet2(false,null,true);

        CustomClassSet result = new CustomClassSet();
        commonClassSet3(result,null,true);
        return result;
    }

    Map<CustomClass,CustomClassSet> cacheChilds = new HashMap<CustomClass,CustomClassSet>();

    // получает классы у которого есть оба интерфейса
    public CustomClassSet commonChilds(CustomClass toCommon) {
        CustomClassSet result = null;
        if(BusinessLogics.activateCaches) result = cacheChilds.get(toCommon);
        if(result!=null) return result;
        result = new CustomClassSet();
        commonClassSet1(false);
        toCommon.commonClassSet2(false,null,false);

        commonClassSet3(result,null,false);
        if(BusinessLogics.activateCaches) cacheChilds.put(toCommon,result);
        return result;
    }


    public void fillParents(Collection<CustomClass> parentSet) {
        if (parentSet.contains(this)) return;
        parentSet.add(this);

        for(CustomClass parent : parents)
            parent.fillParents(parentSet);
    }

    // заполняет список классов
    public void fillChilds(Collection<CustomClass> classSet) {
        if (classSet.contains(this))
            return;

        classSet.add(this);

        for(CustomClass child : children)
            child.fillChilds(classSet);
    }

    // заполняет список классов
    public void fillConcreteChilds(Collection<ConcreteCustomClass> classSet) {
        if(this instanceof ConcreteCustomClass) {
            ConcreteCustomClass concreteThis = (ConcreteCustomClass) this;
            if(classSet.contains(concreteThis)) return;
            classSet.add(concreteThis);
        }

        for(CustomClass child : children)
            child.fillConcreteChilds(classSet);
    }

    // заполняет все нижние классы имплементации
    public abstract void fillNextConcreteChilds(ConcreteCustomClassSet classSet);

    public Collection<ConcreteCustomClass> getConcreteChildren() {

        Collection<ConcreteCustomClass> result = new ArrayList<ConcreteCustomClass>();
        fillConcreteChilds(result);
        return result;
    }

    public DataProperty externalID;
    public DataProperty getExternalID() {

        if (externalID != null) return externalID;
        for (CustomClass parent : parents) {
            DataProperty parentID = parent.getExternalID();
            if (parentID != null) return parentID;
        }

        return null;
    }

    public void getDiffSet(ConcreteObjectClass diffClass,Collection<CustomClass> addClasses,Collection<CustomClass> removeClasses) {
        if(diffClass instanceof UnknownClass) { // если неизвестный то все добавляем
            fillParents(addClasses);
            return;
        }

        commonClassSet1(true); // check
        if(diffClass!=null) ((CustomClass)diffClass).commonClassSet2(false,removeClasses,true);

        commonClassSet3(null,addClasses,true);
    }


    int check = 0;
    // 1-й шаг расставляем пометки 1
    private void commonClassSet1(boolean up) {
        if(check ==1) return;
        check = 1;
        for(CustomClass child : (up? parents : children))
            child.commonClassSet1(up);
    }

    // 2-й шаг пометки
    // 2 - верхний общий класс
    // 3 - просто общий класс
    private void commonClassSet2(boolean set,Collection<CustomClass> free,boolean up) {
        if(!set) {
            if(check >0) {
                if(check !=1) return;
                check = 2;
                set = true;
            } else
                if(free!=null) free.add(this);
        } else {
            if(check ==3 || check ==2) {
                check = 3;
                return;
            }

            check = 3;
        }

        for(CustomClass child : (up? parents : children))
            child.commonClassSet2(set,free,up);
    }

    // 3-й шаг выводит в Set, и сбрасывает пометки
    private void commonClassSet3(CustomClassSet common,Collection<CustomClass> free,boolean up) {
        if(check ==0) return;
        if(common!=null && check ==2) common.add(this);
        if(free!=null && check ==1) free.add(this);

        check = 0;

        for(CustomClass child : (up? parents : children))
            child.commonClassSet3(common,free,up);
    }

    public DataObject getRandomObject(SQLSession session, Random randomizer) throws SQLException {
        JoinQuery<String,Object> query = new JoinQuery<String,Object>(Collections.singletonMap("object",new KeyExpr("object")));
        query.and(query.mapKeys.get("object").getIsClassWhere(getUpSet()));
        List<Map<String,DataObject>> result = new ArrayList<Map<String,DataObject>>(query.executeSelectClasses(session,getBaseClass()).keySet());
        return result.get(randomizer.nextInt(result.size())).get("object");
    }

    public List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects) {
        return objects.get(this);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(0);
        outStream.writeUTF(caption);
        outStream.writeInt(ID);
        outStream.writeBoolean(!children.isEmpty());
    }

    public ArrayList<NavigatorElement> relevantElements = new ArrayList<NavigatorElement>();

    // проверяет находятся ли он и все верхние в OrObjectClassSet'е
    public boolean upInSet(UpClassSet upSet, ConcreteCustomClassSet set) {
        if(upSet.has(this)) return true; // по child'ам уже не идем они явно все тоже есть
        if(this instanceof ConcreteCustomClass && !set.contains((ConcreteCustomClass) this)) return false;
        for(CustomClass child : children)
            if(!child.upInSet(upSet, set)) return false;
        return true;
    }

    public abstract ConcreteCustomClass getSingleClass();
}
