package platform.client.form.layout;

import java.io.Serializable;

public class SimplexComponentDirections implements Serializable {

    double T;
    double L;
    double B;
    double R;

    public SimplexComponentDirections(double iT, double iL, double iB, double iR) {

        T = iT;
        L = iL;
        B = iB;
        R = iR;

    }

}
