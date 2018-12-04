package jasperapi;

public class ReportPropertyData {
    public Integer type;
    public Integer id;
    public String propertyType;
    public String toDraw;
    public Integer length;
    public Integer precision;

    public ReportPropertyData(Integer type, Integer id, String propertyType, String toDraw, Integer length, Integer precision) {
        this.type = type;
        this.id = id;
        this.propertyType = propertyType;
        this.toDraw = toDraw;
        this.length = length;
        this.precision = precision;
    }
}