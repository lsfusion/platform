/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import net.sf.jasperreports.engine.JRAlignment;

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
    static StringClass string;
    static IntegerClass integer;
    static LongClass longClass;
    static DoubleClass doubleClass;
    static DateClass date;
    static BitClass bit;

    static {
        base = new BaseClass(1000000, "Базовый класс");
        data = new BaseClass(1000001, "Данные");
        data.addParent(base);
        integral = new IntegralClass(1000002, "Число");
        integral.addParent(data);
        string = new StringClass(1000003, "Строка");
        string.addParent(data);
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
        if(Main.ActivateCaches) CacheChilds.get(ToCommon);
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
        return Type.Integer;
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
        return Type.Integer;
    }
}

// класс который можно суммировать
class IntegerClass extends IntegralClass {
    IntegerClass(Integer iID, String caption) {super(iID, caption);}
}

class LongClass extends IntegralClass {
    LongClass(Integer iID, String caption) {super(iID, caption);}

    Type getType() {
        return Type.Long;
    }
}

class DoubleClass extends IntegralClass {
    DoubleClass(Integer iID, String caption) {super(iID, caption);}

    Type getType() {
        return Type.Double;
    }
}

class DateClass extends IntegralClass {
    DateClass(Integer iID, String caption) {super(iID, caption);}
}

class BitClass extends IntegralClass {
    BitClass(Integer iID, String caption) {super(iID, caption);}

    Type getType() {
        return Type.Bit;
    }

    Object GetRandomObject(DataSession Session, TableFactory TableFactory, Random Randomizer, Integer Diap) throws SQLException {
        return Randomizer.nextBoolean();
    }

    Object getRandomObject(Map<Class, List<Integer>> Objects, Random Randomizer, Integer Diap) throws SQLException {
        return Randomizer.nextBoolean();
    }
}

class StringClass extends Class {

    StringClass(Integer iID, String caption) {super(iID, caption);}

    Type getType() {
        return Type.String;
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
        ArrayList<Map<KeyField,Integer>> Result = new ArrayList<Map<KeyField,Integer>>(TableFactory.ObjectTable.getClassJoin(this).executeSelect(Session).keySet());
        return Result.get(Randomizer.nextInt(Result.size())).get(TableFactory.ObjectTable.Key);
    }

    Object getRandomObject(Map<Class, List<Integer>> Objects, Random Randomizer, Integer Diap) throws SQLException {
        List<Integer> ClassObjects = Objects.get(this);
        return ClassObjects.get(Randomizer.nextInt(ClassObjects.size()));
    }

    Type getType() {
        return Type.Object;
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

    static StringType String = new StringType();
    static IntegerType Integer = new IntegerType();
    static LongType Long = new LongType();
    static DoubleType Double = new DoubleType();
    static BitType Bit = new BitType();
    static IntegerType Object;
    static Type System;

    static String NULL = "NULL";

    static List<Type> Enum = new ArrayList<Type>();

    static {
        Object = Integer;
        System = Integer;

        Enum.add(Integer);
        Enum.add(String);
        Enum.add(Long);
        Enum.add(Double);
        Enum.add(Bit);
    }

    abstract String getDB(SQLSyntax Syntax);

    abstract Object getMinValue();
    abstract String getEmptyString();
    abstract Object getEmptyValue();

    static Type getObjectType(Object Value) {
        if(Value==null)
            throw new RuntimeException();

        if(Value instanceof Integer)
            return Integer;
        else
            return String;
    }

    SourceExpr getMinValueExpr() {
        return new ValueSourceExpr(getMinValue(),this);
    }

    abstract public String getString(Object Value, SQLSyntax Syntax);
    abstract public java.lang.Class getJavaClass() ;

    public void fillReportDrawField(ReportDrawField reportField) {
        reportField.valueClass = getJavaClass();
        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_LEFT;
    };

    abstract T read(Object Value);
}

class StringType extends Type<String> {

    String getDB(SQLSyntax Syntax) {
        return Syntax.getStringType();
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

    public java.lang.Class getJavaClass() {
        return java.lang.String.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.width = 40;
    }

    String read(Object Value) {
        return (String)Value;
    }
}

abstract class IntegralType<T> extends Type<T> {

    String getEmptyString() {
        return "0";
    }

    Object getEmptyValue() {
        return 0;
    }

    public String getString(Object Value, SQLSyntax Syntax) {
        return Value.toString();
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }
}

class IntegerType extends IntegralType<Integer> {

    String getDB(SQLSyntax Syntax) {
        return Syntax.getIntegerType();
    }

    Object getMinValue() {
        return java.lang.Integer.MIN_VALUE;
    }

    public java.lang.Class getJavaClass() {
        return java.lang.Integer.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.width = 7;
   }

    Integer read(Object Value) {
        if(Value instanceof BigDecimal)
            return ((BigDecimal)Value).intValue();
        else
        if(Value instanceof Double)
            return ((Double)Value).intValue();
        else
        if(Value instanceof Float)
            return ((Float)Value).intValue();
        else
        if(Value instanceof Long)
            return ((Long)Value).intValue();
        else
            return (Integer)Value;
    }
}

class LongType extends IntegralType<Long> {

    String getDB(SQLSyntax Syntax) {
        return Syntax.getLongType();
    }

    Object getMinValue() {
        return java.lang.Long.MIN_VALUE;
    }

    public java.lang.Class getJavaClass() {
        return java.lang.Long.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.width = 13;
    }

    Long read(Object Value) {
        if(Value instanceof BigDecimal)
            return ((BigDecimal)Value).longValue();
        else
        if(Value instanceof Double)
            return ((Double)Value).longValue();
        else
        if(Value instanceof Float)
            return ((Float)Value).longValue();
        else
        if(Value instanceof Integer)
            return ((Integer)Value).longValue();
        else
            return (Long)Value;
    }
}

class DoubleType extends IntegralType<Double> {

    String getDB(SQLSyntax Syntax) {
        return Syntax.getDoubleType();
    }

    Object getMinValue() {
        return java.lang.Double.MIN_VALUE;
    }

    public java.lang.Class getJavaClass() {
        return java.lang.Double.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.width = 13;
    }

    Double read(Object Value) {
        if(Value instanceof BigDecimal)
            return ((BigDecimal)Value).doubleValue();
        else
        if(Value instanceof Float)
            return ((Float)Value).doubleValue();
        else
        if(Value instanceof Long)
            return ((Long)Value).doubleValue();
        if(Value instanceof Integer)
            return ((Integer)Value).doubleValue();
        else
            return (Double)Value;
    }
}

class BitType extends IntegralType<Boolean> {

    String getDB(SQLSyntax Syntax) {
        return Syntax.getBitType();
    }

    Object getMinValue() {
        return false;
    }

    Object getEmptyValue() {
        return false;
    }

    Boolean read(Object Value) {
        if(Value instanceof BigDecimal)
            return ((BigDecimal)Value).byteValue()!=0;
        else
        if(Value instanceof Double)
            return ((Double)Value).byteValue()!=0;
        if(Value instanceof Float)
            return ((Float)Value).byteValue()!=0;
        if(Value instanceof Long)
            return ((Long)Value).byteValue()!=0;
        if(Value instanceof Integer)
            return ((Integer)Value).byteValue()!=0;
        else
            return (Boolean)Value;
    }

    public String getString(Object Value, SQLSyntax Syntax) {
        return Syntax.getBitString((Boolean)Value);
    }

    public java.lang.Class getJavaClass() {
        return java.lang.Boolean.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);    //To change body of overridden methods use File | Settings | File Templates.

        reportField.width = 3;
    }
}

