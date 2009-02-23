package platform.server.logics.classes;

import java.util.*;
import java.sql.SQLException;

import platform.server.view.navigator.NavigatorElement;
import platform.server.logics.properties.groups.AbstractNode;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataSession;
import platform.server.data.types.Type;
import platform.Main;

/**
 *
 * @author ME
 */
abstract public class DataClass extends AbstractNode {

    public static BaseClass base;
    public static BaseClass data;
    public static IntegralClass integral;
    public static IntegerClass integer;
    public static LongClass longClass;
    public static DoubleClass doubleClass;
    public static DateClass date;
    public static BitClass bit;

    private static Collection<StringClass> strings = new ArrayList<StringClass>();
    public static StringClass string(int length) {
        for(StringClass string : strings)
            if(string.length==length)
                return string;
        StringClass string = new StringClass(1000300+strings.size(),"Строка "+length,length);
        string.addParent(data);
        strings.add(string);
        return string;
    }
    private static Collection<NumericClass> numerics = new ArrayList<NumericClass>();
    public static NumericClass numeric(int length,int precision) {
        for(NumericClass numeric : numerics)
            if(numeric.length==length && numeric.precision==precision)
                return numeric;
        NumericClass numeric = new NumericClass(1000700+numerics.size(),"Число "+length+","+precision,length,precision);
        numeric.addParent(data);
        numerics.add(numeric);
        return numeric;
    }

    static {
        base = new BaseClass(1000000, "Базовый класс");
        data = new BaseClass(1000001, "Данные");
        data.addParent(base);
        integral = new IntegralClass(1000002, "Число");
        integral.addParent(data);
        integer = new IntegerClass(1000004, "Кол-во");
        integer.addParent(integral);
        longClass = new LongClass(1000005, "Кол-во");
        longClass.addParent(integral);
        doubleClass = new DoubleClass(1000006, "Кол-во");
        doubleClass.addParent(integral);
        date = new DateClass(1000007, "Дата");
        date.addParent(integral);
        bit = new BitClass(1000008, "Бит");
        bit.addParent(integral);
    }

    Collection<DataClass> parents;
    public List<DataClass> childs;

    public Integer ID;
    public String caption;
    DataClass(Integer iID, String icaption, DataClass... iParents) {
        ID=iID;
        caption = icaption;
        parents = new ArrayList<DataClass>();
        childs = new ArrayList<DataClass>();

        for (DataClass parent : iParents) addParent(parent);
    }

    public String toString() {
        return ID + " " + caption;
    }

    public void addParent(DataClass parentClass) {
        // проверим что в Parent'ах нету этого класса
        for(DataClass parent : parents)
            if(parent.isParent(parentClass)) return;

        Iterator<DataClass> i = parents.iterator();
        while(i.hasNext())
            if(parentClass.isParent(i.next())) i.remove();

        parents.add(parentClass);
        parentClass.childs.add(this);
    }

    public boolean isParent(DataClass parentClass) {
        if(parentClass==this) return true;

        for(DataClass parent : parents)
            if (parent.isParent(parentClass)) return true;

        return false;
    }

    public DataClass findClassID(Integer idClass) {
        if(ID.equals(idClass)) return this;

        for(DataClass child : childs) {
            DataClass findClass = child.findClassID(idClass);
            if(findClass!=null) return findClass;
        }

        return null;
    }

    public Set<DataClass> commonParents(DataClass toCommon) {
        commonClassSet1(true);
        toCommon.commonClassSet2(false,null,true);

        Set<DataClass> result = new HashSet<DataClass>();
        commonClassSet3(result,null,true);
        return result;
    }

    void fillSetID(Collection<Integer> setID) {
        if (setID.contains(ID))
            return;

        setID.add(ID);

        for(DataClass child : childs)
            child.fillSetID(setID);
    }

    public Collection<DataClass> getChildren(boolean recursive) {

        if (!recursive) return new ArrayList<DataClass>(childs);

        Collection<DataClass> result = new ArrayList<DataClass>();
        fillChilds(result);
        return result;
    }

    // заполняет список классов
    public void fillChilds(Collection<DataClass> classSet) {
        if (classSet.contains(this))
            return;

        classSet.add(this);

        for(DataClass Child : childs)
            Child.fillChilds(classSet);
    }

    // заполняет список классов
    public void fillParents(Collection<ObjectClass> parentSet) {
    }

    Map<DataClass,Set<DataClass>> cacheChilds = new HashMap<DataClass,Set<DataClass>>();

    // получает классы у которого есть оба интерфейса
    public Set<DataClass> commonChilds(DataClass toCommon) {
        Set<DataClass> result = null;
        if(Main.activateCaches) result = cacheChilds.get(toCommon);
        if(result!=null) return result;
        result = new HashSet<DataClass>();
        commonClassSet1(false);
        toCommon.commonClassSet2(false,null,false);

        commonClassSet3(result,null,false);
        if(Main.activateCaches) cacheChilds.put(toCommon,result);
        return result;
    }

    int check = 0;
    // 1-й шаг расставляем пометки 1
    private void commonClassSet1(boolean up) {
        if(check ==1) return;
        check = 1;
        for(DataClass child : (up? parents : childs))
            child.commonClassSet1(up);
    }

    // 2-й шаг пометки
    // 2 - верхний общий класс
    // 3 - просто общий класс
    private void commonClassSet2(boolean set,Collection<DataClass> free,boolean up) {
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

        for(DataClass child : (up? parents : childs))
            child.commonClassSet2(set,free,up);
    }

    // 3-й шаг выводит в Set, и сбрасывает пометки
    private void commonClassSet3(Collection<DataClass> common,Collection<DataClass> free,boolean up) {
        if(check ==0) return;
        if(common!=null && check ==2) common.add(this);
        if(free!=null && check ==1) free.add(this);

        check = 0;

        for(DataClass child : (up? parents : childs))
            child.commonClassSet3(common,free,up);
    }

    public void getDiffSet(DataClass diffClass,Collection<DataClass> addClasses,Collection<DataClass> removeClasses) {
        commonClassSet1(true);
        if(diffClass!=null) diffClass.commonClassSet2(false,removeClasses,true);

        commonClassSet3(null,addClasses,true);
    }

    public abstract Type getType();

    // получает рандомный объект
    public abstract Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException;
    public abstract Object getRandomObject(Map<DataClass, List<Integer>> objects,Random randomizer,Integer diap) throws SQLException;

    public DataProperty externalID;
    public DataProperty getExternalID() {

        if (externalID != null) return externalID;
        for (DataClass parent : parents) {
            DataProperty parentID = parent.getExternalID();
            if (parentID != null) return parentID;
        }

        return null;
    }

    public ArrayList<NavigatorElement> relevantElements = new ArrayList<NavigatorElement>();
    public void addRelevantElement(NavigatorElement relevantElement) {
        relevantElements.add(relevantElement);
    }

}
