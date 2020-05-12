package ru.nsu.protasov;

import java.io.*;
import java.util.Arrays;

public class Decoder extends ArithmeticZipping {
    private int bitsToSpit;
    private int bitContainer;
    private int garbage;
    private long messageCode;
    private long fileSize;

    public Decoder() {
        bitsToSpit = 0;
        bitContainer = 0;
        garbage = 0;
        messageCode = 0;
        byteFreq = new long[256];
    }

    /**
     * процедура получения входного бита из прочитанного байта (пока есть возможность, выжимаем биты
     * из байта, иначе прочитать следующий байт) с проверкой на переполнение поступивших битов и неверные входные файлы
     * @throws IOException
     */
    int getBit(BufferedInputStream in) throws IOException {
        if (bitsToSpit == 0) {
           bitContainer = in.read();
           if (bitContainer == -1) {
               garbage += 1;
               if (garbage > BITS - 2) {
                   throw new IOException("Wrong ouput");
               }
           }
           bitsToSpit = Byte.SIZE;
        }

        return (bitContainer >> --bitsToSpit) & 1;
    }

    /**
     * процедура декодирования: построение отрезков по данным из метафайла, пошаговые поиски нужного для
     * вывода байта по отрезкам, обновление границ отрезков с нормализацией
     * @throws IOException
     */
    public void decode(String input, String output, String metaPath) throws IOException {
        Tools.checkFileType(metaPath);

        Bound[] bounds;
        try (var meta = new DataInputStream(new BufferedInputStream(new FileInputStream(metaPath)))) {
            fileSize = meta.readLong();
            for (int i = 0; i < 256; ++i) {
                byteFreq[i] = meta.readLong();
            }
            bounds = createBounds();
        }

        try (var in = new BufferedInputStream(new FileInputStream(input));
             var out = new BufferedOutputStream(new FileOutputStream(output))) {
            for (int i = 1; i <= BITS; ++i) {
                messageCode = messageCode * 2 + getBit(in);
            }
            long lowerBound = 0;
            long upperBound = TOP;
            long cumulative = Arrays.stream(byteFreq).sum();

            long progressCounter = 0;
            for (long i = 0; i < fileSize; ++i) {
                progressCounter++;
                Tools.showProgress(progressCounter, fileSize);

                int datByte = 0;
                long range = upperBound - lowerBound + 1;
                long cumValue = (((messageCode - lowerBound) + 1) * cumulative - 1) / range;
                while (!(bounds[datByte].lower <= cumValue && cumValue < bounds[datByte].upper)) {
                    datByte++;
                }

                upperBound = lowerBound + (range * bounds[datByte].upper) / cumulative - 1;
                lowerBound = lowerBound + (range * bounds[datByte].lower) / cumulative;
                var result = normalizeNewBounds(lowerBound, upperBound, in);
                lowerBound = result.lower;
                upperBound = result.upper;
                out.write(datByte);
            }
        }
    }

    /**
     * нормализация границ отрезков с имитацией проверки одинаковых битов и отсечением общей части для
     * одинаковых первых битов и состояния underflow
     * @throws IOException
     */
    private Bound normalizeNewBounds(long lower, long upper, BufferedInputStream in) throws IOException {
        while (true) {
            if (upper < HALF) { } else if (HALF <= lower) {
                messageCode -= HALF;
                lower -= HALF;
                upper -= HALF;
            } else if (LQUARTER <= lower && upper < BQUARTER) {
                messageCode -= LQUARTER;
                lower -= LQUARTER;
                upper -= LQUARTER;
            } else {
                break;
            }
            lower *= 2;
            upper = upper * 2 + 1;
            messageCode = messageCode * 2 + getBit(in);
        }

        return new Bound(lower, upper);
    }
}
