package ru.nsu.protasov;

/**
 * общий интерфейс кодера и декодера с нужными константами и общим методом сreateBounds
 */
public class ArithmeticZipping {
    protected final int BITS = Integer.SIZE - 2;
    protected final long SCALER = Tools.pow(2, BITS - 2);
    protected final long TOP = Tools.pow(2, BITS) - 1;
    protected final long LQUARTER = TOP / 4 + 1;
    protected final long HALF = LQUARTER * 2;
    protected final long BQUARTER = LQUARTER * 3;

    protected long[] byteFreq;

    /**
     * создаёт отрезки для каждого байта на основе таблицы частот
     */
    protected Bound[] createBounds() {
        var bounds = new Bound[256];
        for (int i = 0; i < 256; ++i) {
            bounds[i] = new Bound(0, 0);
        }
        bounds[0].lower = 0;
        bounds[0].upper = byteFreq[0];
        for (int i = 1; i < 256; ++i) {
            bounds[i].lower = bounds[i - 1].upper;
            bounds[i].upper = bounds[i].lower + byteFreq[i];
        }

        return bounds;
    }
}
