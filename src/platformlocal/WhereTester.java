package platformlocal;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collection;

public class WhereTester {

    private static int RANDOM_SEED = 1234;
    private static int DATAWHERE_COUNT = 16;
    private static float FOLLOW_PERCENT = 0.1f;

    private static int ITERATION_COUNT = 100000;

    private static int NOT = 0;
    private static int AND = 1;
    private static int OR = 2;
    private static int FOLLOW_FALSE = 3;
    private static int AND_NOT = 4;
    private static int MEANS = 5;
    private static int OPERATION_COUNT = 6;

    public void test() {

        Random rand = new Random(RANDOM_SEED);

        List<DataWhere> dataWheres = new ArrayList();
        for (int i = 0; i < DATAWHERE_COUNT; i++) {
            dataWheres.add(new TestDataWhere());
        }

        IntraWhere andWhere1 = new InnerWhere(dataWheres.get(0));
        andWhere1 = andWhere1.in(dataWheres.get(1));

        IntraWhere andWhere2 = new InnerWhere(dataWheres.get(1));
        andWhere2 = andWhere2.in(dataWheres.get(0));

        OuterWhere outerWhere1 = new OuterWhere((InnerWhere)andWhere1);
        OuterWhere outerWhere2 = new OuterWhere((InnerWhere)andWhere2);
        IntraWhere resWhere = outerWhere1.followFalse(outerWhere2);

        for (int i = 0; i < DATAWHERE_COUNT; i++) {

            TestDataWhere where = (TestDataWhere)dataWheres.get(i);
            for (int j = 0; j < ((Float)(FOLLOW_PERCENT * DATAWHERE_COUNT)).intValue(); j++)
                where.follows.add(dataWheres.get(rand.nextInt(DATAWHERE_COUNT)));
        }

        // пишем флойда на замыкание графа
        for (int k = 0; k < DATAWHERE_COUNT; k++) {
            TestDataWhere whereK = (TestDataWhere)dataWheres.get(k);
            for (int i = 0; i < DATAWHERE_COUNT; i++) {
                TestDataWhere whereI = (TestDataWhere)dataWheres.get(i);
                for (int j = 0; j < DATAWHERE_COUNT; j++) {
                    TestDataWhere whereJ = (TestDataWhere)dataWheres.get(j);
                    if (whereI.follow(whereK) && whereK.follow(whereJ))
                        whereI.follows.add(whereJ);
                }
            }
        }
               

        List<List<DataWhere>> allValues = SetBuilder.buildSubSetList(dataWheres);

        Collection<List<DataWhere>> values = new ArrayList();
        for (List<DataWhere> value : allValues) {
            if (correct(value, dataWheres))
                values.add(value);
        }

        Collection<List<DataWhere>> values2 = OuterWhere.buildAllDataWheres(dataWheres);
        if (values.size() != values2.size()) throw new RuntimeException("Error");

        values = values2;

        List<IntraWhere> wheres = new ArrayList(dataWheres);
        wheres.add(new OuterWhere());
        wheres.add(new InnerWhere());

        List<Integer> lengths = new ArrayList();

        for (int iteration = 0; iteration < ITERATION_COUNT; iteration++) {

            System.out.println(iteration);
            if (iteration == 232) {
                int a = 1;
//                break;
            }

            IntraWhere where1 = wheres.get(rand.nextInt(wheres.size()));

            int operation = rand.nextInt(OPERATION_COUNT);

            IntraWhere resultWhere;

            if (operation == NOT) {

                resultWhere = where1.not();

                for (List<DataWhere> value : values) {
                    boolean value1 = where1.getValue(value);
                    boolean resultValue = resultWhere.getValue(value);

                    if (value1 != !resultValue)
                        throw new RuntimeException("Error - Not");
                }

            } else {

                if (! (where1 instanceof OuterWhere) && (operation == OR || operation == FOLLOW_FALSE))
                    operation = AND;

                IntraWhere where2 = wheres.get(rand.nextInt(wheres.size()));

                if (operation == AND)
                    resultWhere = where1.in(where2);
                else {
                    if (operation == OR) {
                        resultWhere = new OuterWhere((OuterWhere)where1);
                        ((OuterWhere)resultWhere).out(where2);
                        ((OuterWhere)resultWhere).simplifyFull();
                    } else {
                        if (operation == FOLLOW_FALSE)
                            resultWhere = where2.followFalse((OuterWhere)where1);
                        else {
                            if (operation == AND_NOT) {
                                resultWhere = where1.inNot(where2);
                                if (resultWhere instanceof OuterWhere)
                                    ((OuterWhere)resultWhere).simplifyFull();
                            } else
                                resultWhere = where1;
                        }
                    }
                }

                boolean alwaysMeans = true;
                boolean means = where1.means(where2);

                for (List<DataWhere> value : values) {
                    boolean value1 = where1.getValue(value);
                    boolean value2 = where2.getValue(value);

                    if (operation == MEANS) {

                        if (means) {
                            if (value1 & (!value2))
                                throw new RuntimeException("Error - MEANS");
                        } else {
                            if (value1 & (!value2))
                                alwaysMeans = false;
                        }

                    } else {

                        boolean resultValue = resultWhere.getValue(value);

                        boolean correctValue;
                        if (operation == AND)
                            correctValue = resultValue == (value1 & value2);
                        else
                        if (operation == OR)
                            correctValue = resultValue == (value1 | value2);
                        else
                        if (operation == FOLLOW_FALSE)
                            correctValue = (value2 | value1) == (resultValue | value1);
                        else
                            correctValue = resultValue == (value1 & (!value2));

                        if (!correctValue)
                            throw new RuntimeException("Error - AND/OR/FF/ANDNOT");
                    }
                }
                if (operation == MEANS && !means && alwaysMeans)
                    throw new RuntimeException("Error - MEANS");
            }

            lengths.add(resultWhere.getOr().size());
            if (wheres.contains(resultWhere)) continue;
            if (resultWhere instanceof OuterWhere && ((OuterWhere)resultWhere).size() > 7)
                continue;
            if (resultWhere instanceof InnerWhere && ((InnerWhere)resultWhere).size() > 10)
                continue;

            wheres.add(resultWhere);

            if (wheres.size() > 100)
                wheres.remove(rand.nextInt(wheres.size()));
        }

        System.out.println(lengths.toString());

    }

    private boolean correct(List<DataWhere> value, List<DataWhere> dataWheres) {
        for (DataWhere where : value) {
            for (DataWhere whereFollows : dataWheres) {
                if (!value.contains(whereFollows) && where.follow(whereFollows))
                    return false;
            }
        }

        return true;
    }

}