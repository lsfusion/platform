package platform.server.logics.classes;

import java.util.*;
import java.sql.SQLException;
import java.text.Format;
import java.io.DataOutputStream;
import java.io.IOException;

import platform.server.view.navigator.NavigatorElement;
import platform.server.view.form.report.ReportDrawField;
import platform.server.logics.properties.groups.AbstractNode;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataSession;
import platform.server.data.types.Type;
import platform.Main;
import net.sf.jasperreports.engine.JRAlignment;

/**
 *
 * @author ME
 */
abstract public class RemoteClass extends AbstractNode {

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

    Collection<RemoteClass> parents;
    public List<RemoteClass> childs;

    public Integer ID;
    public String caption;
    RemoteClass(Integer iID, String icaption, RemoteClass... iParents) {
        ID=iID;
        caption = icaption;
        parents = new ArrayList<RemoteClass>();
        childs = new ArrayList<RemoteClass>();

        for (RemoteClass parent : iParents) addParent(parent);
    }

    public String toString() {
        return ID + " " + caption;
    }

    public void addParent(RemoteClass parentClass) {
        // проверим что в Parent'ах нету этого класса
        for(RemoteClass parent : parents)
            if(parent.isParent(parentClass)) return;

        Iterator<RemoteClass> i = parents.iterator();
        while(i.hasNext())
            if(parentClass.isParent(i.next())) i.remove();

        parents.add(parentClass);
        parentClass.childs.add(this);
    }

    public boolean isParent(RemoteClass parentClass) {
        if(parentClass==this) return true;

        for(RemoteClass parent : parents)
            if (parent.isParent(parentClass)) return true;

        return false;
    }

    public RemoteClass findClassID(Integer idClass) {
        if(ID.equals(idClass)) return this;

        for(RemoteClass child : childs) {
            RemoteClass findClass = child.findClassID(idClass);
            if(findClass!=null) return findClass;
        }

        return null;
    }

    public Set<RemoteClass> commonParents(RemoteClass toCommon) {
        commonClassSet1(true);
        toCommon.commonClassSet2(false,null,true);

        Set<RemoteClass> result = new HashSet<RemoteClass>();
        commonClassSet3(result,null,true);
        return result;
    }

    void fillSetID(Collection<Integer> setID) {
        if (setID.contains(ID))
            return;

        setID.add(ID);

        for(RemoteClass child : childs)
            child.fillSetID(setID);
    }

    public Collection<RemoteClass> getChildren(boolean recursive) {

        if (!recursive) return new ArrayList<RemoteClass>(childs);

        Collection<RemoteClass> result = new ArrayList<RemoteClass>();
        fillChilds(result);
        return result;
    }

    // заполняет список классов
    public void fillChilds(Collection<RemoteClass> classSet) {
        if (classSet.contains(this))
            return;

        classSet.add(this);

        for(RemoteClass Child : childs)
            Child.fillChilds(classSet);
    }

    // заполняет список классов
    public void fillParents(Collection<ObjectClass> parentSet) {
    }

    Map<RemoteClass,Set<RemoteClass>> cacheChilds = new HashMap<RemoteClass,Set<RemoteClass>>();

    // получает классы у которого есть оба интерфейса
    public Set<RemoteClass> commonChilds(RemoteClass toCommon) {
        Set<RemoteClass> result = null;
        if(Main.activateCaches) result = cacheChilds.get(toCommon);
        if(result!=null) return result;
        result = new HashSet<RemoteClass>();
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
        for(RemoteClass child : (up? parents : childs))
            child.commonClassSet1(up);
    }

    // 2-й шаг пометки
    // 2 - верхний общий класс
    // 3 - просто общий класс
    private void commonClassSet2(boolean set,Collection<RemoteClass> free,boolean up) {
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

        for(RemoteClass child : (up? parents : childs))
            child.commonClassSet2(set,free,up);
    }

    // 3-й шаг выводит в Set, и сбрасывает пометки
    private void commonClassSet3(Collection<RemoteClass> common,Collection<RemoteClass> free,boolean up) {
        if(check ==0) return;
        if(common!=null && check ==2) common.add(this);
        if(free!=null && check ==1) free.add(this);

        check = 0;

        for(RemoteClass child : (up? parents : childs))
            child.commonClassSet3(common,free,up);
    }

    public void getDiffSet(RemoteClass diffClass,Collection<RemoteClass> addClasses,Collection<RemoteClass> removeClasses) {
        commonClassSet1(true);
        if(diffClass!=null) diffClass.commonClassSet2(false,removeClasses,true);

        commonClassSet3(null,addClasses,true);
    }

    public abstract Type getType();

    // получает рандомный объект
    public abstract Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException;
    public abstract Object getRandomObject(Map<RemoteClass, List<Integer>> objects,Random randomizer,Integer diap) throws SQLException;

    public DataProperty externalID;
    public DataProperty getExternalID() {

        if (externalID != null) return externalID;
        for (RemoteClass parent : parents) {
            DataProperty parentID = parent.getExternalID();
            if (parentID != null) return parentID;
        }

        return null;
    }

    public ArrayList<NavigatorElement> relevantElements = new ArrayList<NavigatorElement>();
    public void addRelevantElement(NavigatorElement relevantElement) {
        relevantElements.add(relevantElement);
    }

    abstract public Class getJavaClass();
    public abstract Format getDefaultFormat();
    
    public int getMinimumWidth() {
        return getPreferredWidth();
    }
    public int getPreferredWidth() {
        return 50;
    }
    public int getMaximumWidth() {
        return Integer.MAX_VALUE;
    }
    public void fillReportDrawField(ReportDrawField reportField) {
        reportField.valueClass = getJavaClass();
        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_LEFT;
    }

    public abstract byte getTypeID();
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());

        outStream.writeInt(ID);
        outStream.writeUTF(caption);
    }
}
