package ru.nsu.protasov;

/**
 * класс для хранения отрезков
 */
public class Bound {
    public long lower;
    public long upper;

    public Bound() {
        this.lower = -1;
        this.upper = -1;
    }

    public Bound(long lower, long upper) {
        this.lower = lower;
        this.upper = upper;
    }
}
