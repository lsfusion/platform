package platform.server.where;

interface AndObjectWhere<Not extends OrObjectWhere> extends Where<Not> {

    Where pairs(AndObjectWhere pair, boolean plainFollow);

}
