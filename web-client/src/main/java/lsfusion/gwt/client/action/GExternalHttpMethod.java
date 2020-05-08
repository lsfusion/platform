package lsfusion.gwt.client.action;

public enum GExternalHttpMethod {
    GET, DELETE, POST, PUT;

    public boolean hasBody() {
        return this.equals(POST) || this.equals(PUT);
    }
}