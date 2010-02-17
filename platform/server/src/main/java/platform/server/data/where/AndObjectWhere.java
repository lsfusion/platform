package platform.server.data.where;

interface AndObjectWhere extends Where {

    OrObjectWhere not();

    Where pairs(AndObjectWhere pair, boolean plainFollow);
}
