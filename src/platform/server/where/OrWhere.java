package platform.server.where;

import platform.server.data.query.JoinData;
import platform.server.data.query.JoinWheres;
import platform.server.data.query.Translator;
import platform.server.data.query.wheres.MapWhere;

import java.util.Collection;

class OrWhere extends FormulaWhere<AndWhere,AndObjectWhere> implements OrObjectWhere<AndWhere> {

    // вообще надо противоположные + Object, но это наследованием не сделаешь
    OrWhere(AndObjectWhere[] iWheres) {
        super(iWheres);
    }
    OrWhere() {
        super(new AndObjectWhere[0]);
    }

    public int getSize() {
        int size = 1;
        for(Where where : wheres)
            size += where.getSize();
        return size;
    }

    public static Where op(Where where1,Where where2,boolean plainFollow) {

        if(where1.isFalse()) return where2;
        if(where2.isFalse()) return where1;
        if(where1.isTrue()) return where1;
        if(where2.isTrue()) return where2;

        AndObjectWhere[] wheres1 = where1.getAnd();
        AndObjectWhere[] wheres2 = where2.getAnd();

        // пытаем "сливать" элементы
        Where[] pairedWheres = new Where[wheres1.length]; int pairs = 0;
        AndObjectWhere[] rawWheres1 = new AndObjectWhere[wheres1.length]; int num1 = 0;
        AndObjectWhere[] rawWheres2 = wheres2.clone();
        for(AndObjectWhere andWhere1 : wheres1) {
            boolean paired = false;
            for(int i=0;i<rawWheres2.length;i++)
                if(rawWheres2[i]!=null) {
                    Where pairedWhere = rawWheres2[i].pairs(andWhere1, plainFollow);
                    if(pairedWhere!=null) {
                        pairedWheres[pairs++] = pairedWhere;
                        rawWheres2[i] = null;
                        paired = true;
                        break;
                    }
                }
            if(!paired) rawWheres1[num1++] = andWhere1;
        }

        Where unpairedWhere1,unpairedWhere2;
        if(pairs>0) {
            AndObjectWhere[] unpairedWheres2 = new AndObjectWhere[wheres2.length-pairs]; int num2 = 0;
            for(AndObjectWhere andWhere2 : rawWheres2)
                if(andWhere2!=null) unpairedWheres2[num2++] = andWhere2;
            unpairedWhere2 = toWhere(unpairedWheres2);
            unpairedWhere1 = toWhere(rawWheres1,num1);
        } else {
            unpairedWhere1 = where1;
            unpairedWhere2 = where2;
        }

        if(unpairedWhere1.getHeight()<unpairedWhere2.getHeight()) { // поменяем местами чтобы поменьше высота была
            Where t = unpairedWhere1; unpairedWhere1 = unpairedWhere2; unpairedWhere2 = t; }

        // делаем followFalse друг друга
        Where resultWhere;

        Where followWhere1 = followFalse(unpairedWhere1,unpairedWhere2,plainFollow);
        Where followWhere2 = followFalse(unpairedWhere2,followWhere1,plainFollow);
        if(followWhere2.isTrue())
            resultWhere = followWhere2;
        else
            if(plainFollow || (followWhere1==unpairedWhere1 && followWhere2==unpairedWhere2)) { // если совпали follow'ы то все отлично
                AndObjectWhere[] followWheres1 = followWhere1.getAnd(); AndObjectWhere[] followWheres2 = followWhere2.getAnd();
                AndObjectWhere[] resultWheres = new AndObjectWhere[followWheres1.length+followWheres2.length];
                System.arraycopy(followWheres1,0,resultWheres,0,followWheres1.length);
                System.arraycopy(followWheres2,0,resultWheres,followWheres1.length,followWheres2.length);
                resultWhere = toWhere(resultWheres);
            } else // иначе погнали еще раз or может новые скобки появились
                resultWhere = op(followWhere1,followWhere2,false);

        for(int i=0;i<pairs;i++)
            resultWhere = op(resultWhere,pairedWheres[i],plainFollow);

        return resultWhere;
    }

    public boolean directMeansFrom(AndObjectWhere where) {
        for(AndObjectWhere meanWhere : wheres)
            if(meanWhere.directMeansFrom(where))
                return true;
        return false;
    }

    public AndObjectWhere[] getAnd() {
        return wheres;
    }

    public OrObjectWhere[] getOr() {
        return new OrObjectWhere[]{this};
    }

    static int followed = 0;
    static int decomposed = 0;

    static Where followFalse(Where followWhere, Where falseWhere, boolean plainFollow) {

        if(falseWhere.isTrue()) return FALSE;
        if(falseWhere.isFalse()) return followWhere;
//        if(!decomposed(followWhere,falseWhere)) return followWhere;

        AndObjectWhere[] wheres = followWhere.getAnd();

        // поищем "элементарные" directMeans
        // для getAnd() - вырезаем not(), которые directMeans хоть один из OrFalse - если осталось 0 элементов, возвращаем True
        //              - если из какого-нить ObjectWhere directMeans что-нить из OrFalse - то вырезаем элемент
        //              - если остался один элемент и он OrWhere для него рекурсивно повторяем операцию
        // для элементов - проверяем может есть directMeans в OrFalse - если есть вырезаем элемент

        Where[] changedWheres = new Where[wheres.length]; int changed = 0;
        AndObjectWhere[] staticWheres = new AndObjectWhere[wheres.length]; int statics = 0;
        for(AndObjectWhere where : wheres) {
            if(where instanceof ObjectWhere) {
                if(falseWhere.directMeansFrom(((ObjectWhere<?>)where).not())) // проверяем если not возвращаем true
                    return TRUE;
                staticWheres[statics++] = where;
            } else {
                boolean isFalse = false;
                OrObjectWhere[] followWheres = new OrObjectWhere[((AndWhere)where).wheres.length]; int follows = 0;
                for(OrObjectWhere<?> orWhere : ((AndWhere)where).wheres) {
                    if(orWhere instanceof ObjectWhere && falseWhere.directMeansFrom((ObjectWhere<?>)orWhere)) { // если из операнда следует false drop'аем весь orWhere
                        isFalse = true;
                        break;
                    }
                    if(!falseWhere.directMeansFrom(orWhere.not())) // если из not следует один из false'ов
                        followWheres[follows++] = orWhere;
                }
                if(!isFalse) {
                    if(follows==0) // остался True значит и результат true
                        return TRUE;

                    if(follows==((AndWhere)where).wheres.length)
                        staticWheres[statics++] = where;
                    else
                        changedWheres[changed++] = toWhere(followWheres,follows);
                }
            }
        }

        if(changed>0) { // по кругу погнали result
            Where result = toWhere(staticWheres,statics);
            for(int i=0;i<changed;i++)
                result = op(result,changedWheres[i],plainFollow);
            return followFalse(result,falseWhere,plainFollow);
        } else {
            AndObjectWhere[] resultWheres = new AndObjectWhere[wheres.length]; int results = 0;
            for(int i=0;i<statics;i++)
                if(!falseWhere.directMeansFrom(staticWheres[i])) // вычистим из wheres элементы которые заведомо false
                    resultWheres[results++] = staticWheres[i];
            Where result = (results<wheres.length?toWhere(resultWheres,results):followWhere); // чтобы сохранить ссылку
            if(plainFollow || result.isFalse()) // если глубже не надо выходим
                return result;
            else // иначе погнали основной цикл + по sibling'ам
                return result.siblingsFollow(falseWhere);
        }
    }

    public Where decompose(ObjectWhereSet decompose, ObjectWhereSet objects) {
        AndObjectWhere[] staticWheres = new AndObjectWhere[wheres.length]; int stat = 0;
        Where[] decomposedWheres = new Where[wheres.length]; int decomp = 0;
        AndObjectWhere[] maxWheres = wheres.clone();
        for(int i=0;i<maxWheres.length;i++) { // будем бежать с высот поменьше - своего рода пузырьком
            for(int j=maxWheres.length-1;j>i;j--)
                if(maxWheres[j].getHeight()<maxWheres[j-1].getHeight()) {
                    AndObjectWhere t = maxWheres[j];
                    maxWheres[j] = maxWheres[j-1];
                    maxWheres[j-1] = t;
                }
            Where decomposedWhere = maxWheres[i].decompose(decompose,objects);
            if(decomposedWhere == maxWheres[i])
                staticWheres[stat++] = maxWheres[i];
            else {
                if(decomposedWhere.isTrue()) return decomposedWhere;
                decomposedWheres[decomp++] = decomposedWhere;
            }
        }

        if(stat < wheres.length) {
            Where result = toWhere(staticWheres,stat);
            for(int i=0;i<decomp;i++)
                result = op(result,decomposedWheres[i],true);
            return result;
        } else
            return this;
    }

    // вычищает те Or'ы которые заведомо не будут false
    static Where decomposeFalse(Where where,Where toFalse) {

        AndObjectWhere[] wheres = where.getAnd();

        // toFalse может менять знаки
        ObjectWhereSet decompose = new ObjectWhereSet(toFalse.getObjects());
        decompose.addAll(toFalse.not().getObjects());

        // для всех or'ов делаем decompose, получаем доп. objects
        Where[] decomposedWheres = new Where[wheres.length];
        ObjectWhereSet[] objectWheres = new ObjectWhereSet[wheres.length];
        for(int i=0;i<wheres.length;i++) {
            objectWheres[i] = new ObjectWhereSet();
            decomposedWheres[i] = wheres[i].decompose(decompose,objectWheres[i]);
        }

        AndObjectWhere[] rawResult = new AndObjectWhere[wheres.length]; int resnum = 0;

        // разбиваем на компоненты по правилу (доп. objects linked or.getObjects)
        int queue = 0; int first = 0;
        int[] component = new int[wheres.length]; int numWheres = 1; component[0] = 0;
        boolean[] checked = new boolean[wheres.length];
        while(true) {
            for(int i=first+1;i<wheres.length;i++)
                if(!checked[i] && (objectWheres[component[queue]].linked(wheres[i].getObjects()) || objectWheres[i].linked(wheres[component[queue]].getObjects()))) {
                    component[numWheres++] = i;
                    checked[i] = true;
                }
            queue++;
            if(queue >= numWheres) { // в очереди никого нету
                // проверяем or всей компоненты на checkTrue
                Where toCheck = FALSE;
                AndObjectWhere[] staticWheres = new AndObjectWhere[numWheres]; int stats = 0;
                for(int i=0;i<numWheres;i++)
                    if(decomposedWheres[component[i]]==wheres[component[i]])
                        staticWheres[stats++] = wheres[component[i]];
                    else
                        toCheck = op(toCheck,decomposedWheres[component[i]],true);
                if(stats < numWheres && op(toCheck,toWhere(staticWheres,stats),true).checkTrue()) // если не true - drop'аем
                    for(int i=0;i<numWheres;i++)
                        rawResult[resnum++] = wheres[component[i]];

                first++; while(first<wheres.length && checked[first]) first++; // ищем первый необработанный элемент
                if(first>=wheres.length) break; // больше нету

                component = new int[wheres.length]; numWheres = 1;
                component[0] = first; queue = 0;
            }
        }

        if(resnum==wheres.length)
            return where;
        else
            return toWhere(rawResult,resnum);
    }

    public Where siblingsFollow(Where falseWhere) {

        if(op(this,falseWhere,true).checkTrue())
            return TRUE;

        falseWhere = decomposeFalse(falseWhere,this);
        if(falseWhere.isFalse())
            return this;

        Where followWhere = FALSE; //false
        AndObjectWhere[] staticWheres = wheres;
        int current = 0;
        for(AndObjectWhere where : wheres) { // после упрощения важно использовать именно этот элемент, иначе неправильно работать будет
            AndObjectWhere[] siblingWheres = siblings(staticWheres, current);
            Where followAndWhere = followFalse(where,op(op(toWhere(siblingWheres),followWhere,true),falseWhere,true),false);
            if(followAndWhere!=where) { // если изменился static "перекидываем" в result
                followWhere = op(followWhere,followAndWhere,false);
                staticWheres = siblingWheres;
            } else
                current++;
        }

        if(staticWheres.length<wheres.length) // чтобы сохранить ссылку
            return op(toWhere(staticWheres),followWhere,false);
        else
            return this;
    }

    static boolean checkTrue(AndObjectWhere[] wheres, int numWheres) {
        // ищем максимальную по высоте вершину and
        int maxWhere = -1;
        for(int i=0;i<numWheres;i++)
            if(wheres[i] instanceof AndWhere && (maxWhere<0 || wheres[i].getHeight()>wheres[maxWhere].getHeight()))
                maxWhere = i;
        if(maxWhere<0)
            return false;

        Where siblingWhere = toWhere(siblings(wheres, maxWhere, numWheres));
        OrObjectWhere[] maxWheres = ((AndWhere)wheres[maxWhere]).wheres.clone();
        for(int i=0;i<maxWheres.length;i++) { // будем бежать с высот поменьше - своего рода пузырьком
            for(int j=maxWheres.length-1;j>i;j--)
                if(maxWheres[j].getHeight()<maxWheres[j-1].getHeight()) {
                    OrObjectWhere t = maxWheres[j];
                    maxWheres[j] = maxWheres[j-1];
                    maxWheres[j-1] = t;
                }
            if(!op(maxWheres[i],siblingWhere,true).checkTrue())
                return false;
        }
        return true;
    }

    public boolean checkTrue() {
        if(wheres.length==0) return false;

//        if(1==1) return checkTrue(wheres,wheres.length);

        // разбиваем на компоненты
        // компонента определяется просто (OrObjectWhere) A связан с B если есть oA V oB = TRUE - data intersect followNot или followData intersect not
        // будем вытаскивать компоненты по очереди, пока не закончатся все элементы
        AndObjectWhere[][] components = new AndObjectWhere[wheres.length][]; int numComp = 0;
        int queue = 0;
        int[] nums = new int[wheres.length]; int[] heights = new int[wheres.length];
        AndObjectWhere[] wherePool = wheres.clone();
        int first = 0;
        AndObjectWhere[] component = new AndObjectWhere[wheres.length]; int numWheres = 1;
        component[0] = wheres[0]; int heightComponent = wheres[0].getHeight();
        while(true) {
            for(int i=first+1;i<wherePool.length;i++)
                if(wherePool[i]!=null && component[queue].getObjects().linked(wherePool[i].getObjects())) {
                    component[numWheres++] = wherePool[i];
                    int heightWhere = wherePool[i].getHeight();
                    if(heightWhere > heightComponent) heightComponent = heightWhere;
                    wherePool[i] = null;
                }
            queue++;
            if(queue >= numWheres) { // в очереди никого нету
                components[numComp] = component; nums[numComp] = numWheres; heights[numComp] = heightComponent; numComp++; // сохраняем
                first++; while(first<wherePool.length && wherePool[first]==null) first++; // ищем первый необработанный элемент
                if(first>=wherePool.length) break; // больше нету
                component = new AndObjectWhere[wheres.length]; numWheres = 1;
                component[0] = wherePool[first]; heightComponent = wherePool[first].getHeight();
                queue = 0;
            }
        }

        for(int i=0;i<numComp;i++) { // бежим по высотам поменьше
            for(int j=numComp-1;j>i;j--)
                if(heights[j]<heights[j-1]) {
                    AndObjectWhere[] tc = components[j]; components[j] = components[j-1]; components[j-1] = tc;
                    int th = heights[j]; heights[j] = heights[j-1]; heights[j-1] = th;
                    int tn = nums[j]; nums[j] = nums[j-1]; nums[j-1] = tn;
                }
            if(checkTrue(components[i],nums[i]))
                return true;
        }

        // return checkTrue(wheres, wheres.length);
        return false;
    }

    AndWhere getNot() {
        return new AndWhere(not(wheres));
    }

    AndObjectWhere[] newArray(int length) {
        return new AndObjectWhere[length];  //To change body of implemented methods use File | Settings | File Templates.
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public Where translate(Translator translator) {

        AndObjectWhere[] staticWheres = new AndObjectWhere[wheres.length]; int statics = 0;
        Where[] transWheres = new Where[wheres.length]; int trans = 0;
        for(AndObjectWhere where : wheres) {
            Where transWhere = where.translate(translator);
            if(transWhere==where)
                staticWheres[statics++] = where;
            else
                transWheres[trans++] = transWhere;
        }

        if(transWheres.length==0)
            return this;


        if(translator.direct()) {
            AndObjectWhere[] resultWheres = new AndObjectWhere[wheres.length];
            System.arraycopy(staticWheres,0,resultWheres,0,statics);
            System.arraycopy(transWheres,0,resultWheres,statics,trans); // должен быть тоже AndObjectWhere
            return toWhere(resultWheres);
        } else {
            Where result = toWhere(staticWheres,statics);
            for(int i=0;i<trans;i++)
                result = result.or(transWheres[i]);
            return result;
        }
    }

    // разобъем чисто для оптимизации
    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(int i=0;i<wheres.length;i++)
            wheres[i].fillJoinWheres(joins,andWhere.and(toWhere(siblings(wheres,i)).not()));
    }

    String getOp() {
        return "OR";
    }

    public boolean isTrue() {
        return false;
    }

    public boolean isFalse() {
        return wheres.length==0;
    }

    public boolean evaluate(Collection<DataWhere> data) {
        boolean result = false;
        for(Where where : wheres)
            result = result || where.evaluate(data);
        return result;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof OrWhere && equalWheres(((OrWhere)o).wheres);
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public JoinWheres getInnerJoins() {
        JoinWheres result = new JoinWheres();
        for(Where<?> where : wheres)
            result.or(where.getInnerJoins());
        return result;
    }

    int hashCoeff() {
        return 5;
    }
}
