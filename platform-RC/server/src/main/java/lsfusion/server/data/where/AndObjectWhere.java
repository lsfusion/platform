package lsfusion.server.data.where;

interface AndObjectWhere<Not extends OrObjectWhere> extends Where {

    Not not();

    Where pairs(AndObjectWhere pair);
}
