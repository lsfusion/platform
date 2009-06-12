package platform.server.data.classes.where;

import platform.base.DNFWhere;
import platform.base.QuickMap;


// !!! equals'ы и hashCode должны только в meanWheres вызываться
public abstract class AbstractClassWhere<K,ThisAnd extends AbstractAndClassWhere<K,ThisAnd>,This extends AbstractClassWhere<K,ThisAnd,This>> extends DNFWhere<ThisAnd,This> {

    protected AbstractClassWhere() {
    }

    public AbstractClassWhere(ThisAnd[] iWheres) {
        super(iWheres);
    }
    public AbstractClassWhere(ThisAnd where) {
        super(where);
    }

    @Override
    public boolean isTrue() {
        return wheres.length==1 && wheres[0].isEmpty();
    }

    public boolean means(ThisAnd from, ThisAnd to) {
        return from.means(to);
    }

    public ThisAnd[] and(ThisAnd where1, ThisAnd where2) {
        ThisAnd andWhere = where1.merge(where2);
        if(andWhere==null)
            return newArray(0);
        else
            return toArray(andWhere);
    }

    public ClassSet getSingleWhere(K key) {
        assert (wheres.length == 1);
        return wheres[0].get(key);
    }

    protected ClassSet[] newValues(int size) {
        return new ClassSet[size];
    }

    class OrAnd extends QuickMap<K,OrClassSet, OrAnd> {

        OrAnd() { }
        OrAnd(OrAnd set) { super(set); }

        protected OrClassSet[] newValues(int size) {
            return new OrClassSet[size];
        }

        protected OrAnd getThis() {
            return this;
        }

        protected OrAnd copy() {
            return new OrAnd(this);
        }

        protected OrClassSet addValue(OrClassSet prevValue, OrClassSet newValue) {
            return prevValue.or(newValue);
        }

        protected boolean containsAll(OrClassSet who, OrClassSet what) {
            return who.containsAll(what);
        }

        public boolean meansFrom(ThisAnd where) {
            for(int i=0;i<size;i++) { // так как элементы не зависимы проверим каждый в отдельности
                ClassSet inSet = where.getPartial((K) table[indexes[i]]);
                if(inSet!=null && containsAll(vtable[indexes[i]],inSet.getOr())) return true;
            }
            return false;
        }
    }

    public boolean means(This where) {
        // берем все перестановки из means, and'им их и проверяем на means всех элементов, сделаем в лоб потому как Combinations слишком громоздкий
        if(where.isFalse()) return isFalse();
        if(where.isTrue() || isFalse()) return true;
        if(isTrue()) return false;

        for(ThisAnd andWhere : wheres)
            if(!where.meansFrom(andWhere)) return false;
        return true;
    }

    private boolean meansFrom(ThisAnd andFrom) {
        Object[][] mwheres = new Object[wheres.length][]; ClassSet[][] msets=new ClassSet[wheres.length][]; int[] mnums=new int[wheres.length]; int mnum=0;
        // берем все перестановки из means, and'им их и проверяем на means всех элементов, сделаем в лоб потому как Combinations слишком громоздкий
        for(ThisAnd where : wheres) { // бежим по всем операндам
            Object[] keys = new Object[where.size]; ClassSet[] sets = new ClassSet[where.size]; int num=0;
            for(int i=0;i<where.size;i++) { // бежим по всем элементам
                K key = where.getKey(i); ClassSet set = where.getValue(i); ClassSet fromSet;
                if((fromSet=andFrom.getPartial(key))==null || fromSet.and(set).isEmpty()) { // если в from'е нету или не пересекаются не интересует
                    num = -1;
                    break;
                } else
                    if(!set.containsAll(fromSet)) { // если не следует
                        keys[num]=key; sets[num++]=set;
                    }
            }
            if(num==0)
                return true;
            if(num>0) {
                mwheres[mnum] = keys;
                msets[mnum] = sets;
                mnums[mnum++] = num;
            }
        }

        if(mnum==0)
            return false;
        // погнали перестановки
        int[] nums = new int[mnum];
        while(true) {
//            ThisAnd next = createThisAnd();
            OrAnd orAnd = new OrAnd();
            for(int j=0;j<mnum;j++)
                orAnd.add((K)mwheres[j][nums[j]],msets[j][nums[j]].getOr());
            // здесь не так здесь надо проверять каждый на хоть один из этого
            if(!orAnd.meansFrom(andFrom))
                return false;

            int i = 0; // переходим к следующей комбинации
            while(i<mnum && nums[i]==mnums[i]-1) {
                nums[i] = 0;
                i++;
            }
            if(i==mnum)
                return true;
            else
                nums[i]++;
        }
    }


}
