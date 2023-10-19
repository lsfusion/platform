package lsfusion.interop.session;

public enum ExternalHttpMethod {
    GET, DELETE, PATCH, POST, PUT;

    public boolean hasBody() {
        return this.equals(DELETE) || this.equals(PATCH) || this.equals(POST) || this.equals(PUT);
    }
}