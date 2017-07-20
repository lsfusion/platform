package lsfusion.server.form.instance;

public class PropertyType {
    public String type;
    public String toDraw;
    public Integer length;
    public Integer precision;

    public PropertyType(String type, String toDraw, Integer length, Integer precision) {
        this.type = type;
        this.toDraw = toDraw;
        this.length = length;
        this.precision = precision;
    }
}