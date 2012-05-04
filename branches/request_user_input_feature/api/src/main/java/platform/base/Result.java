package platform.base;

public class Result<R> {

    public R result = null;
    public void set(R result) {
        this.result = result;
    }

    public Result() {
    }
    public Result(R result) {
        this.result = result;
    }
}
