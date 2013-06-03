package lsfusion.interop.form.layout;

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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SimplexComponentDirections) {
            SimplexComponentDirections dirs = (SimplexComponentDirections) obj;
            return T == dirs.T && L == dirs.L && B == dirs.B && R == dirs.R; 
        }
        return false;
    }
}
