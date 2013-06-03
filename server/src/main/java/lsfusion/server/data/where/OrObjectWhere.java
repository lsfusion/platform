package lsfusion.server.data.where;

// generics Not чисто чтобы обойти компилятор
public interface OrObjectWhere<Not extends AndObjectWhere> extends Where {

    Not not();
}
