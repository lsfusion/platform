package platform.interop.form.layout;

import java.io.Serializable;

public class SimplexComponentDirections implements Serializable {

    public double T;
    public double L;
    public double B;
    public double R;

    public SimplexComponentDirections(double iT, double iL, double iB, double iR) {

        T = iT;
        L = iL;
        B = iB;
        R = iR;

    }

}
