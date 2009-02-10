package platformlocal;

import java.util.*;

class TestDataWhere extends DataWhere {

    String caption;
    TestDataWhere(String iCaption) {
        caption = iCaption;
    }

    public String toString() {
        return caption;
    }

    Set<DataWhere> follows = new HashSet<DataWhere>();

    boolean follow(DataWhere dataWhere) {
        return equals(dataWhere) || follows.contains(dataWhere);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public JoinWheres getInnerJoins() {
        return new JoinWheres(Where.TRUE,this);
    }

    public boolean equals(Where where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
    
    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return caption;
    }

    public <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values) {
    }

    int getHash() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Where translate(Translator translator) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    DataWhereSet getExprFollows() {
        return new DataWhereSet();
    }
}

class WhereTester {

    private static int RANDOM_SEED = 1234;
    private static int DATAWHERE_COUNT = 4;
    private static float FOLLOW_PERCENT = 0.1f;

    private static int ITERATION_COUNT = 100000;

    private static int NOT = 0;
    private static int AND = 1;
    private static int OR = 2;
    private static int FOLLOW_FALSE = 3;
    private static int FOLLOW_TRUE = 4;
    private static int AND_NOT = 5;
    private static int MEANS = 6;
    private static int OPERATION_COUNT = 7;

    public void test() {

        Random rand = new Random(RANDOM_SEED);

        List<DataWhere> dataWheres = new ArrayList();
        for (int i = 0; i < DATAWHERE_COUNT; i++) {
            dataWheres.add(new TestDataWhere("t"+i));
        }

        for (int i = 0; i < DATAWHERE_COUNT; i++) {

            TestDataWhere where = (TestDataWhere)dataWheres.get(i);
            for (int j = 0; j < ((Float)(FOLLOW_PERCENT * DATAWHERE_COUNT)).intValue(); j++)
                where.follows.add(dataWheres.get(rand.nextInt(DATAWHERE_COUNT)));
        }

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

        List<Where> wheres = new ArrayList();
        wheres.add(Where.FALSE);
        wheres.add(Where.TRUE);

        for (DataWhere dataWhere : dataWheres)
            wheres.add(dataWhere);

        List<Integer> lengths = new ArrayList();

        for (int iteration = 0; iteration < ITERATION_COUNT; iteration++) {
            System.out.println(iteration);
            if (iteration == 5) {
            int a = 1;
//                break;
            }

            Where where1 = wheres.get(rand.nextInt(wheres.size()));

            int operation = rand.nextInt(OPERATION_COUNT);

            Where resultWhere;

            if (operation == NOT) {

                resultWhere = where1.not();

                for (List<DataWhere> value : values) {
                    boolean value1 = where1.evaluate(value);
                    boolean resultValue = resultWhere.evaluate(value);

                    if (value1 != !resultValue)
                        throw new RuntimeException("Error - Not");
                }

            } else {

    //                if (! (where1 instanceof OrWhere) && (operation == OR || operation == FOLLOW_FALSE))
    //                    operation = AND;

                Where where2 = wheres.get(rand.nextInt(wheres.size()));

                resultWhere = opiterate(values, where1, operation, where2);
            }

            if (wheres.contains(resultWhere)) continue;

            wheres.add(resultWhere);
            System.out.println(resultWhere);

            if (wheres.size() > 100)
            wheres.remove(rand.nextInt(wheres.size()));
        }

        System.out.println(lengths.toString());
    }

    private Where opiterate(Collection<List<DataWhere>> values, Where where1, int operation, Where where2) {
        Where resultWhere;
        Where resultWhere1;
        if (operation == AND)
            resultWhere1 = where1.and(where2);
        else {
            if (operation == OR) {
                resultWhere1 = where1.or(where2);
            } else {
                if (operation == FOLLOW_FALSE)
                    resultWhere1 = where2.followFalse(where1);
                else {
                    if (operation == FOLLOW_TRUE)
                        resultWhere1 = where2.followFalse(where1.not());
                    else {
                        if (operation == AND_NOT) {
                            resultWhere1 = where1.and(where2.not());
                        } else
                            resultWhere1 = where1;
                    }
                }
            }
        }

        resultWhere = resultWhere1;

        boolean alwaysMeans = true;
        boolean means = where1.means(where2);

        for (List<DataWhere> value : values) {
            boolean value1 = where1.evaluate(value);
            boolean value2 = where2.evaluate(value);

            if (operation == MEANS) {

                if (means) {
                    if (value1 & (!value2))
                        throw new RuntimeException("Error - MEANS");
                } else {
                    if (value1 & (!value2))
                        alwaysMeans = false;
                }

            } else {

                boolean resultValue = resultWhere.evaluate(value);

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
                if (operation == FOLLOW_TRUE)
                    correctValue = (value2 & value1) == (resultValue & value1);
                else
                    correctValue = resultValue == (value1 & (!value2));

                if (!correctValue)
                    throw new RuntimeException("Error - AND/OR/FF/ANDNOT");
            }
        }
        if (operation == MEANS && !means && alwaysMeans)
            throw new RuntimeException("Error - MEANS");
        return resultWhere;
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
