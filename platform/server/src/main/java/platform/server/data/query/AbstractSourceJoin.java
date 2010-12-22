package platform.server.data.query;

import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.ManualLazy;
import platform.server.caches.OuterContext;
import platform.server.data.Table;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.OrderExpr;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.logics.BusinessLogics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

abstract public class AbstractSourceJoin<T extends OuterContext<T>> extends AbstractOuterContext<T> implements SourceJoin {

    public SourceJoin[] getEnum() {
        return new SourceJoin[]{this};
    }
   
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj!=null && getClass() == obj.getClass() && twins((AbstractSourceJoin) obj);
    }

    public abstract boolean twins(AbstractSourceJoin obj);

    protected static class ToString extends CompileSource  {
        public ToString(Set<Value> values, Set<KeyExpr> keys) {
            super(new KeyType() {
                public Type getKeyType(KeyExpr expr) {
                    return ObjectType.instance;
                }
            }, BaseUtils.mapString(values), BusinessLogics.debugSyntax);
            keySelect.putAll(BaseUtils.mapString(keys));
        }

        public String getSource(KeyExpr expr) {
            return expr.toString();
        }

        public String getSource(Table.Join.Expr expr) {
            return expr.toString();
        }

        public String getSource(Table.Join.IsIn where) {
            return where.toString();
        }

        public String getSource(GroupExpr groupExpr) {
            return groupExpr.toString();
        }

        public String getSource(OrderExpr orderExpr) {
            return orderExpr.toString();
        }
    }

    @Override
    public String toString() {
        return getSource(new ToString(enumValues(this),enumKeys(this)));
    }

    public void enumKeys(final Set<KeyExpr> keys) {
        enumerate(new ExprEnumerator() {
            public boolean enumerate(SourceJoin join) {
                if(join instanceof KeyExpr)
                    keys.add((KeyExpr) join);
                return true;
            }
        });
    }

    public void enumValues(final Set<Value> values) {
        enumerate(new ExprEnumerator() {
            public boolean enumerate(SourceJoin join) {
                join.enumInnerValues(values);
                return true;
            }
        });
    }

    public void enumInnerValues(Set<Value> values) {
    }

    public final static ArrayInstancer<SourceJoin> instancer = new ArrayInstancer<SourceJoin>() {
        public SourceJoin[] newArray(int size) {
            return new SourceJoin[size];
        }
    };

    public static SourceJoin[] merge(Collection<? extends SourceJoin> set,SourceJoin... array) {
        return BaseUtils.add(set.toArray(new SourceJoin[set.size()]),array,instancer);
    }
    
    public static Set<KeyExpr> enumKeys(Collection<? extends SourceJoin> set, SourceJoin... array) {
        return enumKeys(merge(set,array));
    }

    public static Set<KeyExpr> enumKeys(SourceJoin... array) {
        Set<KeyExpr> result = new HashSet<KeyExpr>();
        for(SourceJoin element : array)
            element.enumKeys(result);
        return result;
    }

    public static Set<Value> enumValues(Collection<? extends SourceJoin> set, SourceJoin... array) {
        return enumValues(merge(set,array));
    }

    public static Set<Value> enumValues(SourceJoin... array) {
        Set<Value> result = new HashSet<Value>();
        for(SourceJoin element : array)
            element.enumValues(result);
        return result;
    }

    public static long getComplexity(Collection<? extends SourceJoin> elements) {
        long complexity = 0;
        for(SourceJoin element : elements)
            complexity += element.getComplexity();
        return complexity;
    }

    public void enumerate(ExprEnumerator enumerator) {
        if(enumerator.enumerate(this))
            enumDepends(enumerator);
    }

    protected abstract long calculateComplexity();
    
    private Long complexity;    
    @ManualLazy
    public long getComplexity() {
        if(complexity==null)
            complexity = calculateComplexity();
        return complexity;
    }
}
