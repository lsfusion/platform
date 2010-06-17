package platform.server.classes;

import platform.interop.Data;
import platform.server.auth.SecurityPolicy;
import platform.server.caches.ManualLazy;
import platform.server.classes.sets.ConcreteCustomClassSet;
import platform.server.classes.sets.CustomClassSet;
import platform.server.classes.sets.UpClassSet;
import platform.server.data.SQLSession;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.group.AbstractNode;
import platform.server.logics.table.ObjectTable;
import platform.server.view.form.CustomClassView;
import platform.server.view.form.CustomObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.navigator.ClassNavigatorForm;
import platform.server.view.navigator.NavigatorElement;
import platform.server.view.navigator.NavigatorForm;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public abstract class CustomClass extends AbstractNode implements ObjectClass, ValueClass {

    public Type getType() {
        return ObjectType.instance;
    }

    public final Collection<CustomClass> parents;
    public final List<CustomClass> children;

    public String toString() {
        return caption;
    }

    public String caption;
    public Integer ID;
    public CustomClass(Integer ID, String caption, CustomClass... parents) {
        this.ID = ID;
        this.caption = caption;
        this.parents = new ArrayList<CustomClass>();
        children = new ArrayList<CustomClass>();

        for (CustomClass parent : parents) {
            this.parents.add(parent);
            parent.children.add(this);
            assert parent.childs==null;
        }
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public UpClassSet getUpSet() {
        return new UpClassSet(this);
    }

    public BaseClass getBaseClass() {
        return parents.iterator().next().getBaseClass();
    }

    public boolean isChild(CustomClass parentClass) {
        return parentClass.getChilds().contains(this);
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
    public void fillChilds(Set<CustomClass> classSet) {
        classSet.add(this);

        for(CustomClass child : children)
            child.fillChilds(classSet);
    }

    private Set<CustomClass> childs = null;
    @ManualLazy
    public Set<CustomClass> getChilds() {
        if(childs==null) {
            childs = new HashSet<CustomClass>();
            fillChilds(childs);
        }
        return childs;
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
        Query<String,Object> query = new Query<String,Object>(Collections.singletonMap("object",new KeyExpr("object")));
        query.and(query.mapKeys.get("object").isClass(getUpSet()));
        List<Map<String,DataObject>> result = new ArrayList<Map<String,DataObject>>(query.executeClasses(session,getBaseClass()).keySet());
        return result.get(randomizer.nextInt(result.size())).get("object");
    }

    public List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects) {
        return objects.get(this);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(Data.OBJECT);
        outStream.writeBoolean(this instanceof ConcreteCustomClass);
        outStream.writeUTF(caption);
        outStream.writeInt(ID);

        outStream.writeByte(children.size());
        for (CustomClass cls : children)
            cls.serialize(outStream);
    }

    private List<NavigatorElement> relevantElements = new ArrayList<NavigatorElement>();
    public void addRelevant(NavigatorElement element) {
        relevantElements.add(element);        
    }

    public List<NavigatorElement> getRelevantElements(BusinessLogics<?> BL, SecurityPolicy securityPolicy) {
        List<CustomClass> upParents = new ArrayList<CustomClass>();
        fillParents(upParents);

        List<NavigatorElement> result = new ArrayList<NavigatorElement>();
        for(CustomClass parent : upParents)
            for (NavigatorElement element : parent.relevantElements)
                if (securityPolicy.navigator.checkPermission(element))
                    result.add(element);
        for(CustomClass parent : upParents)
            result.add(parent.getBaseClassForm(BL));
        return result;
    }

    public NavigatorForm getClassForm(BusinessLogics<?> BL, SecurityPolicy securityPolicy) {
        for (NavigatorElement element : relevantElements)
            if (element instanceof NavigatorForm && securityPolicy.navigator.checkPermission(element))
                return (NavigatorForm) element;
        return getBaseClassForm(BL);
    }

    private NavigatorForm baseClassForm = null;
    public NavigatorForm getBaseClassForm(BusinessLogics<?> BL) {
        if(baseClassForm==null) {
            baseClassForm = new ClassNavigatorForm(BL,this);
            for(CustomClass child : children)
                baseClassForm.add(child.getBaseClassForm(BL));
        }
        return baseClassForm;
    }

    // проверяет находятся ли он и все верхние в OrObjectClassSet'е
    public boolean upInSet(UpClassSet upSet, ConcreteCustomClassSet set) {
        if(upSet.has(this)) return true; // по child'ам уже не идем они явно все тоже есть
        if(this instanceof ConcreteCustomClass && !set.contains((ConcreteCustomClass) this)) return false;
        for(CustomClass child : children)
            if(!child.upInSet(upSet, set)) return false;
        return true;
    }
    public boolean upInSet(CustomClass[] wheres, int numWheres, CustomClass[] proceeded, int numProceeded, CustomClass check) {
        if(isChild(check))
            return true;
        for(int i=0;i<numWheres;i++) if(wheres[i]!=null && isChild(wheres[i])) return true;
        for(int i=0;i<numProceeded;i++) if(isChild(proceeded[i])) return true;

        if(this instanceof ConcreteCustomClass) return false;
        for(CustomClass child : children)
            if(!child.upInSet(wheres,numWheres, proceeded, numProceeded, check)) return false;
        return true;
    }

    public abstract ConcreteCustomClass getSingleClass();

    public ObjectImplement newObject(int ID, String SID, String caption, CustomClassView classView, boolean addOnTransaction) {
        return new CustomObjectImplement(ID, SID, this, caption, classView, addOnTransaction);
    }

    public ValueExpr getActionExpr() {
        return new ValueExpr(0,getConcreteChildren().iterator().next());
    }
}
