/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.*;
import java.math.BigDecimal;

/**
 *
 * @author ME
 */
abstract class Class extends AbstractNode {

    static BaseClass base;
    static BaseClass data;
    static IntegralClass integral;
    static IntegerClass integer;
    static LongClass longClass;
    static DoubleClass doubleClass;
    static DateClass date;
    static BitClass bit;

    private static Collection<StringClass> strings = new ArrayList<StringClass>();
    static StringClass string(int length) {
        for(StringClass string : strings)
            if(string.length==length)
                return string;
        StringClass string = new StringClass(1000300+strings.size(),"Строка "+length,length);
        string.addParent(data);
        strings.add(string);
        return string;
    }
    private static Collection<NumericClass> numerics = new ArrayList<NumericClass>();
    static NumericClass numeric(int length,int precision) {
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

    Collection<Class> parents;
    List<Class> childs;
    
    Integer ID;
    String caption;
    Class(Integer iID, String icaption, Class... iParents) {
        ID=iID;
        caption = icaption;
        parents = new ArrayList<Class>();
        childs = new ArrayList<Class>();

        for (Class parent : iParents) addParent(parent);
    }

    public String toString() {
        return ID + " " + caption;
    }

    void addParent(Class parentClass) {
        // проверим что в Parent'ах нету этого класса
        for(Class parent : parents)
            if(parent.isParent(parentClass)) return;
        
        Iterator<Class> i = parents.iterator();
        while(i.hasNext())
            if(parentClass.isParent(i.next())) i.remove();
            
        parents.add(parentClass);
        parentClass.childs.add(this);
    }

    boolean isParent(Class parentClass) {
        if(parentClass==this) return true;

        for(Class parent : parents)
            if (parent.isParent(parentClass)) return true;
        
        return false;
    }
    
    Class findClassID(Integer idClass) {
        if(ID.equals(idClass)) return this;

        for(Class child : childs) {
            Class findClass = child.findClassID(idClass);
            if(findClass!=null) return findClass;
        }
        
        return null;
    }
    
    Set<Class> commonParents(Class toCommon) {
        commonClassSet1(true);
        toCommon.commonClassSet2(false,null,true);

        Set<Class> result = new HashSet<Class>();
        commonClassSet3(result,null,true);
        return result;
    }
    
    void fillSetID(Collection<Integer> setID) {
        if (setID.contains(ID))
            return;
        
        setID.add(ID);
        
        for(Class child : childs)
            child.fillSetID(setID);
    }

    Collection<Class> getChildren(boolean recursive) {

        if (!recursive) return new ArrayList<Class>(childs);

        Collection<Class> result = new ArrayList<Class>();
        fillChilds(result);
        return result;
    }

    // заполняет список классов
    void fillChilds(Collection<Class> classSet) {
        if (classSet.contains(this))
            return;
        
        classSet.add(this);
        
        for(Class Child : childs)
            Child.fillChilds(classSet);
    }

    // заполняет список классов
    void fillParents(Collection<ObjectClass> parentSet) {
    }

    Map<Class,Set<Class>> cacheChilds = new HashMap<Class,Set<Class>>();

    // получает классы у которого есть оба интерфейса
    Set<Class> commonChilds(Class toCommon) {
        Set<Class> result = null;
        if(Main.activateCaches) result = cacheChilds.get(toCommon);
        if(result!=null) return result;
        result = new HashSet<Class>();
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
        for(Class child : (up? parents : childs))
            child.commonClassSet1(up);
    }
    
    // 2-й шаг пометки 
    // 2 - верхний общий класс
    // 3 - просто общий класс
    private void commonClassSet2(boolean set,Collection<Class> free,boolean up) {
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
            
        for(Class child : (up? parents : childs))
            child.commonClassSet2(set,free,up);
    }
    
    // 3-й шаг выводит в Set, и сбрасывает пометки
    private void commonClassSet3(Collection<Class> common,Collection<Class> free,boolean up) {
        if(check ==0) return;
        if(common!=null && check ==2) common.add(this);
        if(free!=null && check ==1) free.add(this);
               
        check = 0;

        for(Class child : (up? parents : childs))
            child.commonClassSet3(common,free,up);
    }
    
    void getDiffSet(Class diffClass,Collection<Class> addClasses,Collection<Class> removeClasses) {
        commonClassSet1(true);
        if(diffClass!=null) diffClass.commonClassSet2(false,removeClasses,true);

        commonClassSet3(null,addClasses,true);
    }

    abstract Type getType();

    // получает рандомный объект
    abstract Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException;
    abstract Object getRandomObject(Map<Class, List<Integer>> objects,Random randomizer,Integer diap) throws SQLException;

    DataProperty externalID;
    DataProperty getExternalID() {

        if (externalID != null) return externalID;
        for (Class parent : parents) {
            DataProperty parentID = parent.getExternalID();
            if (parentID != null) return parentID;
        }

        return null;
    }

    ArrayList<NavigatorElement> relevantElements = new ArrayList<NavigatorElement>();
    void addRelevantElement(NavigatorElement relevantElement) {
        relevantElements.add(relevantElement);
    }

}

class BaseClass extends Class {

    BaseClass(Integer iID, String iCaption) {
        super(iID, iCaption);
    }

    Type getType() {
        return Type.integer;
    }// получает рандомный объект

    Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return null;
    }

    Object getRandomObject(Map<Class, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return null;
    }
}

// класс который можно сравнивать
class IntegralClass extends Class {
    
    IntegralClass(Integer iID, String caption) {super(iID, caption);}

    Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return randomizer.nextInt(diap * diap +1);
    }

    Object getRandomObject(Map<Class, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return randomizer.nextInt(diap);
    }

    Type getType() {
        return Type.integer;
    }
}

// класс который можно суммировать
class IntegerClass extends IntegralClass {
    IntegerClass(Integer iID, String caption) {super(iID, caption);}
}

class LongClass extends IntegralClass {
    LongClass(Integer iID, String caption) {super(iID, caption);}

    Type getType() {
        return Type.longType;
    }
}

class DoubleClass extends IntegralClass {
    DoubleClass(Integer iID, String caption) {super(iID, caption);}

    Type getType() {
        return Type.doubleType;
    }
}

class DateClass extends IntegralClass {
    DateClass(Integer iID, String caption) {super(iID, caption);}
}

class BitClass extends IntegralClass {
    BitClass(Integer iID, String caption) {super(iID, caption);}

    Type getType() {
        return Type.bit;
    }

    Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return randomizer.nextBoolean();
    }

    Object getRandomObject(Map<Class, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return randomizer.nextBoolean();
    }
}

class NumericClass extends DoubleClass {

    int length;
    int precision;

    NumericClass(Integer iID, String caption, int iLength, int iPrecision) {
        super(iID, caption);
        length = iLength;
        precision = iPrecision;
    }

    Type getType() {
        return Type.numeric(length,precision);
    }
}

class StringClass extends Class {

    StringClass(Integer iID, String caption, int iLength) {
        super(iID, caption);
        length = iLength;
    }

    int length;

    Type getType() {
        return Type.string(length);
    }
    
    Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        return "NAME "+ randomizer.nextInt(50);
    }

    Object getRandomObject(Map<Class, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        return "NAME "+ randomizer.nextInt(diap);
    }

}

class ObjectClass extends Class {

    ObjectClass(Integer iID, String caption, Class... parents) {super(iID, caption, parents); }

    Object getRandomObject(DataSession session, TableFactory tableFactory, Integer diap, Random randomizer) throws SQLException {
        ArrayList<Map<KeyField,Integer>> Result = new ArrayList<Map<KeyField,Integer>>(tableFactory.objectTable.getClassJoin(this).executeSelect(session).keySet());
        return Result.get(randomizer.nextInt(Result.size())).get(tableFactory.objectTable.key);
    }

    Object getRandomObject(Map<Class, List<Integer>> objects, Random randomizer, Integer diap) throws SQLException {
        List<Integer> classObjects = objects.get(this);
        return classObjects.get(randomizer.nextInt(classObjects.size()));
    }

    Type getType() {
        return Type.object;
    }

    void fillParents(Collection<ObjectClass> parentSet) {
        if (parentSet.contains(this))
            return;

        parentSet.add(this);

        for(Class parent : parents)
            parent.fillParents(parentSet);
    }
}

abstract class Type<T> {

    String ID;
    Type(String iID) {
        ID = iID;
    }

    static IntegerType integer = new IntegerType();
    static LongType longType = new LongType();
    static DoubleType doubleType = new DoubleType();
    static BitType bit = new BitType();
    static IntegerType object;
    static Type system;

    static String NULL = "NULL";

    static StringType string(int length) {
        StringType result = new StringType(length);
        types.add(result);
        return result;
    }
    static NumericType numeric(int length,int precision) {
        NumericType result = new NumericType(length,precision);
        types.add(result);
        return result;
    }
    static Set<Type> types = new HashSet<Type>();

    static {
        object = integer;
        system = integer;

        types.add(integer);
        types.add(longType);
        types.add(doubleType);
        types.add(bit);
    }

    abstract String getDB(SQLSyntax syntax);

    abstract Object getMinValue();
    abstract String getEmptyString();
    abstract Object getEmptyValue();
    ValueExpr getEmptyValueExpr() {
        return new ValueExpr(0,this);
    }

    AndExpr getExpr(Object value) {
        if(value==null)
            return new NullExpr(this);
        else
            return new ValueExpr(value,this);
    }

    ValueExpr getMinValueExpr() {
        return new ValueExpr(getMinValue(),this);
    }

    abstract public String getString(Object value, SQLSyntax syntax);

    abstract T read(Object value);

    abstract boolean greater(Object value1,Object value2);
}

class StringType extends Type<String> {

    int length;
    StringType(int iLength) {
        super("S"+iLength);
        length = iLength;
    }

    public boolean equals(Object obj) {
        return this==obj || obj instanceof StringType && length==((StringType)obj).length;
    }

    public int hashCode() {
        return length;
    }

    String getDB(SQLSyntax syntax) {
        return syntax.getStringType(length);
    }

    Object getMinValue() {
        return "";
    }

    String getEmptyString() {
        return "''";
    }

    Object getEmptyValue() {
        return "";
    }

    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    String read(Object value) {
        return (String) value;
    }

    boolean greater(Object value1, Object value2) {
        throw new RuntimeException("Java не умеет сравнивать строки");
    }
}

abstract class IntegralType<T> extends Type<T> {

    protected IntegralType(String iID) {
        super(iID);
    }

    String getEmptyString() {
        return "0";
    }

    Object getEmptyValue() {
        return 0;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }

}

class IntegerType extends IntegralType<Integer> {

    IntegerType() {
        super("I");
    }

    String getDB(SQLSyntax syntax) {
        return syntax.getIntegerType();
    }

    Object getMinValue() {
        return java.lang.Integer.MIN_VALUE;
    }

    Integer read(Object value) {
        if(value instanceof BigDecimal)
            return ((BigDecimal) value).intValue();
        else
        if(value instanceof Double)
            return ((Double) value).intValue();
        else
        if(value instanceof Float)
            return ((Float) value).intValue();
        else
        if(value instanceof Long)
            return ((Long) value).intValue();
        else
            return (Integer) value;
    }

    boolean greater(Object value1, Object value2) {
        return read(value1)>read(value2);
    }
}

class LongType extends IntegralType<Long> {

    LongType() {
        super("L");
    }

    String getDB(SQLSyntax syntax) {
        return syntax.getLongType();
    }

    Object getMinValue() {
        return java.lang.Long.MIN_VALUE;
    }

    Long read(Object value) {
        if(value instanceof BigDecimal)
            return ((BigDecimal) value).longValue();
        else
        if(value instanceof Double)
            return ((Double) value).longValue();
        else
        if(value instanceof Float)
            return ((Float) value).longValue();
        else
        if(value instanceof Integer)
            return ((Integer) value).longValue();
        else
            return (Long) value;
    }

    boolean greater(Object value1, Object value2) {
        return read(value1)>read(value2);
    }
}

class DoubleType extends IntegralType<Double> {

    DoubleType() {
        super("D");
    }
    DoubleType(String iID) {
        super(iID);
    }

    String getDB(SQLSyntax syntax) {
        return syntax.getDoubleType();
    }

    Object getMinValue() {
        return java.lang.Double.MIN_VALUE;
    }

    Double read(Object value) {
        if(value instanceof BigDecimal)
            return ((BigDecimal) value).doubleValue();
        else
        if(value instanceof Float)
            return ((Float) value).doubleValue();
        else
        if(value instanceof Long)
            return ((Long) value).doubleValue();
        if(value instanceof Integer)
            return ((Integer) value).doubleValue();
        else
            return (Double) value;
    }

    boolean greater(Object value1, Object value2) {
        return read(value1)>read(value2);
    }
}

class NumericType extends DoubleType {

    int length;
    int precision;
    NumericType(int iLength,int iPrecision) {
        super("N"+iLength+"P"+iPrecision);
        length = iLength;
        precision = iPrecision;
    }

    String getDB(SQLSyntax syntax) {
        return syntax.getNumericType(length,precision);
    }
}

class BitType extends IntegralType<Boolean> {

    BitType() {
        super("B");
    }

    String getDB(SQLSyntax syntax) {
        return syntax.getBitType();
    }

    Object getMinValue() {
        return false;
    }

    Object getEmptyValue() {
        return false;
    }

    Boolean read(Object value) {
        if(value instanceof BigDecimal)
            return ((BigDecimal) value).byteValue()!=0;
        else
        if(value instanceof Double)
            return ((Double) value).byteValue()!=0;
        if(value instanceof Float)
            return ((Float) value).byteValue()!=0;
        if(value instanceof Long)
            return ((Long) value).byteValue()!=0;
        if(value instanceof Integer)
            return ((Integer) value).byteValue()!=0;
        else
            return (Boolean) value;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return syntax.getBitString((Boolean) value);
    }

    boolean greater(Object value1, Object value2) {
        return read(value1) && !read(value2);
    }
}

