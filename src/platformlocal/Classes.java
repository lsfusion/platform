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

    Collection<Class> Parents;
    List<Class> Childs;
    
    Integer ID;
    String caption;
    Class(Integer iID, String icaption, Class... parents) {
        ID=iID;
        caption = icaption;
        Parents = new ArrayList<Class>();
        Childs = new ArrayList<Class>();

        for (Class parent : parents) addParent(parent);
    }

    public String toString() {
        return ID + " " + caption;
    }

    void addParent(Class ParentClass) {
        // проверим что в Parent'ах нету этого класса
        for(Class Parent:Parents) 
            if(Parent.isParent(ParentClass)) return;
        
        Iterator<Class> i = Parents.iterator();
        while(i.hasNext())
            if(ParentClass.isParent(i.next())) i.remove();
            
        Parents.add(ParentClass);
        ParentClass.Childs.add(this);        
    }

    boolean isParent(Class Class) {
        if(Class==this) return true;

        for(Class Parent : Parents)
            if (Parent.isParent(Class)) return true;
        
        return false;
    }
    
    Class findClassID(Integer idClass) {
        if(ID.equals(idClass)) return this;

        for(Class Child : Childs) {
            Class FindClass = Child.findClassID(idClass);
            if(FindClass!=null) return FindClass;
        }
        
        return null;
    }
    
    Set<Class> commonParents(Class ToCommon) {
        CommonClassSet1(true);
        ToCommon.CommonClassSet2(false,null,true);

        Set<Class> Result = new HashSet<Class>();
        CommonClassSet3(Result,null,true);
        return Result;
    }
    
    void FillSetID(Collection<Integer> SetID) {
        if (SetID.contains(ID))
            return;
        
        SetID.add(ID);
        
        for(Class Child : Childs)
            Child.FillSetID(SetID);
    }

    Collection<Class> getChildren(boolean recursive) {

        if (!recursive) return new ArrayList(Childs);

        Collection<Class> result = new ArrayList();
        fillChilds(result);
        return result;
    }

    // заполняет список классов
    void fillChilds(Collection<Class> ClassSet) {
        if (ClassSet.contains(this))
            return;
        
        ClassSet.add(this);
        
        for(Class Child : Childs)
            Child.fillChilds(ClassSet);
    }

    // заполняет список классов
    void fillParents(Collection<ObjectClass> ParentSet) {
    }

    Map<Class,Set<Class>> CacheChilds = new HashMap<Class,Set<Class>>();

    // получает классы у которого есть оба интерфейса
    Set<Class> commonChilds(Class ToCommon) {
        Set<Class> Result = null;
        if(Main.ActivateCaches) Result = CacheChilds.get(ToCommon);
        if(Result!=null) return Result;
        Result = new HashSet<Class>();
        CommonClassSet1(false);
        ToCommon.CommonClassSet2(false,null,false);

        CommonClassSet3(Result,null,false);
        if(Main.ActivateCaches) CacheChilds.put(ToCommon,Result);
        return Result;
    }

    int Check = 0;
    // 1-й шаг расставляем пометки 1
    private void CommonClassSet1(boolean Up) {
        if(Check==1) return;
        Check = 1;
        for(Class Child : (Up?Parents:Childs))
            Child.CommonClassSet1(Up);
    }
    
    // 2-й шаг пометки 
    // 2 - верхний общий класс
    // 3 - просто общий класс
    private void CommonClassSet2(boolean Set,Collection<Class> Free,boolean Up) {
        if(!Set) {
            if(Check>0) {
                if(Check!=1) return;
                Check = 2;
                Set = true;
            } else
                if(Free!=null) Free.add(this);
        } else {
            if(Check==3 || Check==2) {
                Check = 3;
                return;
            }
            
            Check = 3;
        }
            
        for(Class Child : (Up?Parents:Childs))
            Child.CommonClassSet2(Set,Free,Up);
    }
    
    // 3-й шаг выводит в Set, и сбрасывает пометки
    private void CommonClassSet3(Collection<Class> Common,Collection<Class> Free,boolean Up) {
        if(Check==0) return;
        if(Common!=null && Check==2) Common.add(this);
        if(Free!=null && Check==1) Free.add(this);
               
        Check = 0;

        for(Class Child : (Up?Parents:Childs))
            Child.CommonClassSet3(Common,Free,Up);
    }
    
    void GetDiffSet(Class DiffClass,Collection<Class> AddClasses,Collection<Class> RemoveClasses) {
        CommonClassSet1(true);
        if(DiffClass!=null) DiffClass.CommonClassSet2(false,RemoveClasses,true);

        CommonClassSet3(null,AddClasses,true);
    }

    abstract Type getType();

    // получает рандомный объект
    abstract Object GetRandomObject(DataSession Session,TableFactory TableFactory,Random Randomizer,Integer Diap) throws SQLException;
    abstract Object getRandomObject(Map<Class, List<Integer>> Objects,Random Randomizer,Integer Diap) throws SQLException;

    DataProperty externalID;
    DataProperty getExternalID() {

        if (externalID != null) return externalID;
        for (Class parent : Parents) {
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

    Object GetRandomObject(DataSession Session, TableFactory TableFactory, Random Randomizer, Integer Diap) throws SQLException {
        return null;
    }

    Object getRandomObject(Map<Class, List<Integer>> Objects, Random Randomizer, Integer Diap) throws SQLException {
        return null;
    }
}

// класс который можно сравнивать
class IntegralClass extends Class {
    
    IntegralClass(Integer iID, String caption) {super(iID, caption);}

    Object GetRandomObject(DataSession Session,TableFactory TableFactory,Random Randomizer,Integer Diap) throws SQLException {
        return Randomizer.nextInt(Diap*Diap+1);
    }

    Object getRandomObject(Map<Class, List<Integer>> Objects, Random Randomizer, Integer Diap) throws SQLException {
        return Randomizer.nextInt(Diap);
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

    Object GetRandomObject(DataSession Session, TableFactory TableFactory, Random Randomizer, Integer Diap) throws SQLException {
        return Randomizer.nextBoolean();
    }

    Object getRandomObject(Map<Class, List<Integer>> Objects, Random Randomizer, Integer Diap) throws SQLException {
        return Randomizer.nextBoolean();
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
    
    Object GetRandomObject(DataSession Session,TableFactory TableFactory,Random Randomizer,Integer Diap) throws SQLException {
        return "NAME "+Randomizer.nextInt(50);
    }

    Object getRandomObject(Map<Class, List<Integer>> Objects, Random Randomizer, Integer Diap) throws SQLException {
        return "NAME "+Randomizer.nextInt(Diap);
    }

}

class ObjectClass extends Class {

    ObjectClass(Integer iID, String caption, Class... parents) {super(iID, caption, parents); }

    Object GetRandomObject(DataSession Session,TableFactory TableFactory,Random Randomizer,Integer Diap) throws SQLException {
        ArrayList<Map<KeyField,Integer>> Result = new ArrayList<Map<KeyField,Integer>>(TableFactory.objectTable.getClassJoin(this).executeSelect(Session).keySet());
        return Result.get(Randomizer.nextInt(Result.size())).get(TableFactory.objectTable.key);
    }

    Object getRandomObject(Map<Class, List<Integer>> Objects, Random Randomizer, Integer Diap) throws SQLException {
        List<Integer> ClassObjects = Objects.get(this);
        return ClassObjects.get(Randomizer.nextInt(ClassObjects.size()));
    }

    Type getType() {
        return Type.object;
    }

    void fillParents(Collection<ObjectClass> ParentSet) {
        if (ParentSet.contains(this))
            return;

        ParentSet.add(this);

        for(Class Parent : Parents)
            Parent.fillParents(ParentSet);
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

    AndExpr getExpr(Object Value) {
        if(Value==null)
            return new NullExpr(this);
        else
            return new ValueExpr(Value,this);
    }

    ValueExpr getMinValueExpr() {
        return new ValueExpr(getMinValue(),this);
    }

    abstract public String getString(Object Value, SQLSyntax Syntax);

    abstract T read(Object value);

    abstract boolean greater(Object Value1,Object Value2);
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

    public String getString(Object Value, SQLSyntax Syntax) {
        return "'" + Value + "'";
    }

    String read(Object value) {
        return (String) value;
    }

    boolean greater(Object Value1, Object Value2) {
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

    public String getString(Object Value, SQLSyntax Syntax) {
        return Value.toString();
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

    boolean greater(Object Value1, Object Value2) {
        return read(Value1)>read(Value2);
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

    boolean greater(Object Value1, Object Value2) {
        return read(Value1)>read(Value2);
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

    boolean greater(Object Value1, Object Value2) {
        return read(Value1)>read(Value2);
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

    public String getString(Object Value, SQLSyntax Syntax) {
        return Syntax.getBitString((Boolean)Value);
    }

    boolean greater(Object Value1, Object Value2) {
        return read(Value1) && !read(Value2);
    }
}

