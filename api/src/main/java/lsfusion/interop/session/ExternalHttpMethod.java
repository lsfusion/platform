package lsfusion.interop.session;

public enum ExternalHttpMethod {
    GET, DELETE, POST, PUT;

    public boolean hasBody() {
        return this.equals(DELETE) || this.equals(POST) || this.equals(PUT);
    }
}