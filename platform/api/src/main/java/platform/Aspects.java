package platform;

class Aspects {
}

/*
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import platform.server.where.Where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Aspect
class Aspects {

//    @AfterReturning(pointcut="call(Where and(Where))",returning="f")
//    public void returnAnd(Where f) {
//        System.out.println("return " + f);
//    }

    // КЭШИРОВАНИЕ NOT'а

    public interface Method1Interface {
        Object getResult(int method);
        void setResult(int method,Object result);

        static int METHOD_NOT = 0;
        static int METHOD_GET_OBJECTS = 1;
        static int METHOD_HASH = 2;
        static int METHOD_COUNT = 3;
    }
    public static class Method1InterfaceCache implements Method1Interface {
        Object[] results = new Object[METHOD_COUNT];

        public Object getResult(int method) {
            return results[method];
        }

        public void setResult(int method,Object result) {
            results[method] = result;
        }
    }
    @DeclareParents(value="Where",defaultImpl=Method1InterfaceCache.class)
    private Method1Interface method1Interface;

    public Object callMethod(ProceedingJoinPoint thisJoinPoint,Method1Interface object,int method) {
        Object result = object.getResult(method);
        if(result!=null) return result;
        try {
            result = thisJoinPoint.proceed();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        object.setResult(method,result);
        return result;
    }

    @Around("call(Where not()) && target(object)")
    public Object callNot(ProceedingJoinPoint thisJoinPoint, Method1Interface object) {
        return callMethod(thisJoinPoint, object, Method1Interface.METHOD_NOT);
    }
    @Around("call(* getObjects()) && target(object)")
    public Object callGetObjects(ProceedingJoinPoint thisJoinPoint, FormulaWhere object) {
        return callMethod(thisJoinPoint, (Method1Interface) object, Method1Interface.METHOD_GET_OBJECTS);
    }
    @Around("call(* hashCode()) && target(object)")
    public Object callHashCode(ProceedingJoinPoint thisJoinPoint, FormulaWhere object) {
        return callMethod(thisJoinPoint, (Method1Interface) object, Method1Interface.METHOD_HASH);
    }

    // КЭШИРОВАНИЕ 2-местных операторов
    public interface Method2Interface {
        Object getResult(Object param,int method);
        void setResult(Object param,int method,Object result);

        static int METHOD_OR = 0;
        static int METHOD_AND = 1;
        static int METHOD_MEAN = 2;
        static int METHOD_FOLLOW_FALSE = 3;
        static int METHOD_COUNT = 4;
    }
    public static class Method2InterfaceCache implements Method2Interface {

        List<Map<Object,Object>> results = new ArrayList<Map<Object,Object>>();

        public Method2InterfaceCache() {
            for(int i=0;i<METHOD_COUNT;i++)
                results.add(new HashMap<Object, Object>());
        }

        public Object getResult(Object param,int method) {
            return results.get(method).get(param);
        }
        public void setResult(Object param,int method,Object result) {
            results.get(method).put(param,result);
        }
    }
    @DeclareParents(value="Where",defaultImpl=Method2InterfaceCache.class)
    private Method2Interface method2Interface;
    public Object callMethod(ProceedingJoinPoint thisJoinPoint,Method2Interface object,Object param,int method) {
        Object result = object.getResult(param,method);
        if(result!=null) return result;
        try {
            result = thisJoinPoint.proceed();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        object.setResult(param,method,result);
        return result;
    }

    @Around("call(Where or(Where)) && target(where1) && args(where2)")
    public Object callOr(ProceedingJoinPoint thisJoinPoint,Method2Interface where1,Where where2) {
        return callMethod(thisJoinPoint,where1,where2,Method2Interface.METHOD_OR);
    }
    @Around("call(Where and(Where)) && target(where1) && args(where2)")
    public Object callAnd(ProceedingJoinPoint thisJoinPoint,Method2Interface where1,Where where2) {
        return callMethod(thisJoinPoint,where1,where2,Method2Interface.METHOD_AND);
    }
    @Around("call(boolean means(Where)) && target(where1) && args(where2)")
    public Object callMean(ProceedingJoinPoint thisJoinPoint,FormulaWhere where1,FormulaWhere where2) {
        return callMethod(thisJoinPoint, (Method2Interface) where1,where2,Method2Interface.METHOD_MEAN);
    }
    @Around("call(Where followFalse(Where)) && target(where1) && args(where2)")
    public Object callFollowFalse(ProceedingJoinPoint thisJoinPoint,FormulaWhere where1,FormulaWhere where2) {
        return callMethod(thisJoinPoint, (Method2Interface) where1,where2,Method2Interface.METHOD_FOLLOW_FALSE);
    }

    static Map<Where, Where> cachedWheres = new HashMap<Where,Where>();
    @Around("call(* newThis(*))")
    public Object callOr(ProceedingJoinPoint thisJoinPoint) {
        Where where = null;
        try {
            where = (Where) thisJoinPoint.proceed();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        Where cachedWhere = cachedWheres.get(where);
        if(cachedWhere==null) {
            cachedWheres.put(where,where);
            return where;
        } else
            return cachedWhere;
    }
}
  */