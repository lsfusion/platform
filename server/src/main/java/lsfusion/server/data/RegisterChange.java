package lsfusion.server.data;

public interface RegisterChange {

    RegisterChange VOID = new RegisterChange() {
        public void register(SQLSession sql, int result) {

        }
    };

    void register(SQLSession sql, int result);
}
