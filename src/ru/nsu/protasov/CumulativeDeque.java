package ru.nsu.protasov;

import java.util.LinkedList;

/**
 * специальный дек для вывода байтов в случае возникновения состояния underflow
 */
public class CumulativeDeque extends LinkedList<Boolean> {
    private int cumulator;

    public CumulativeDeque() {
        cumulator = 0;
    }

    /**
     * запоминает количество выкинутых вторых битов в числе
     */
    public void cumulate() {
        cumulator++;
    }

    /**
     * вносит в очередь сначала поступивший бит, затем cumulator противоположных поступившему
     */
    @Override
    public void addLast(Boolean bool) {
        super.addLast(bool);

        if (cumulator != 0) {
            for (int i = 0; i < cumulator; ++i) {
                super.addLast(!bool);
            }
            cumulator = 0;
        }
    }
}
