package lsfusion.gwt.base.client;

public class Dimension {
    public int width;
    public int height;

    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return "Dimension{" +
               "width=" + width +
               ", height=" + height +
               '}';
    }
}
