package ru.nsu.protasov;

import java.io.*;
import java.util.Arrays;

public class Coder extends ArithmeticZipping {
    private final CumulativeDeque bitsToOutput;
    private final long blockSize;
    private long fileSize;

    public Coder(int blockSize) {
        this.blockSize = Tools.pow(2, blockSize);
        byteFreq = new long[256];
        bitsToOutput = new CumulativeDeque();
        fileSize = 0;
    }

    /**
     * Считывает файл, строит таблицу частот и запоминает размер файла
     * @throws IOException если произошла ошибка при чтении файла
     * @throws OutOfMemoryError если подан слишком большой файл,
     *                             превышающий вместимость кодера
     */
    public void encode(String input, String output, String metaPath) throws IOException, OutOfMemoryError {
        Tools.checkFileType(metaPath);

        try (var in = new BufferedInputStream(new FileInputStream(input))) {
            freqClear();
            int datByte;
            while ((datByte = in.read()) != -1) {
                if (fileSize >= blockSize) {
                    throw new OutOfMemoryError("The file is too big");
                }
                byteFreq[datByte]++;
                fileSize++;
            }
        }

        try (var in = new BufferedInputStream(new FileInputStream(input));
             var out = new BufferedOutputStream(new FileOutputStream(output));
             var meta = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(metaPath)))) {
            buildCode(in, out, meta);
        }
    }

    /**
     * Конструирует отрезки частот, в которых лежат байты,
     * пишет метаданные в метафайл и преобразует поступающие биты при втором чтении
     * @throws IOException если произошла ошибка при чтении файла
     */
    private void buildCode(BufferedInputStream in, BufferedOutputStream out,
                           DataOutputStream meta) throws IOException {
        var bounds = buildBounds();
        createMeta(meta);
        encodeBits(in, out, bounds);
    }

    /**
     * заполняет массив частот
     */
    private void freqClear() {
        Arrays.fill(byteFreq, 0);
    }

    /**
     * отправляет байты в поток вывода, если есть что отправлять
     * @throws IOException
     */
    private void writeBits(BufferedOutputStream out) throws IOException {
        while (bitsToOutput.size() >= Byte.SIZE) {
            int result = 0, mask = 1;
            for (int i = Byte.SIZE - 1; i >= 0; --i) {
                result = result + ((bitsToOutput.getFirst()) ? mask << i : 0);
                bitsToOutput.removeFirst();
            }
            out.write(result);
        }
    }

    /**
     * записывает оставшиеся < 8 байт после завершения кодирования,
     * если такие есть
     * @throws IOException
     */
    private void writeRest(BufferedOutputStream out) throws IOException {
        int size = bitsToOutput.size();
        int freeBits = Byte.SIZE - size;

        int result = 0, mask = 1;
        for (int i = Byte.SIZE - 1; i >= freeBits; --i) {
            result = result + ((bitsToOutput.getFirst()) ? mask << i : 0);
            bitsToOutput.removeFirst();
        }
        out.write(result);
    }

    /**
     * записывает в файл метаданные: размер файла и таблицу частот
     * @throws IOException
     */
    private void createMeta (DataOutputStream meta) throws IOException {
        meta.writeLong(fileSize);
        for (int i = 0; i < 256; ++i) {
            meta.writeLong(byteFreq[i]);
        }
    }

    /**
     * процедура нормализации, используется целочисленная арифметика, для определения общих битов
     * и состояния underflow (когда отрезок сужается возле середины), используются сравнения с половиной
     * и четвертями отрезка и вычитанием общих частей явных выкидываний битов
     */
    private Bound normalizeNewBounds(long lower, long upper) {
        while (true) {
            if (upper < HALF) {
                bitsToOutput.addLast(false);
            } else if (lower >= HALF) {
                bitsToOutput.addLast(true);
                lower -= HALF;
                upper -= HALF;
            } else if (upper < BQUARTER && lower >= LQUARTER) {
                bitsToOutput.cumulate();
                lower -= LQUARTER;
                upper -= LQUARTER;
            } else {
                break;
            }

            lower *= 2;
            upper = 2 * upper + 1;
        }

        return new Bound(lower, upper);
    }

    /**
     * конструирует отрезки для байтов, при необходимости масштабируя
     * (в силу теоретических ограничений кумулятивная частота не должна превышать
     * 2 ^ (N - 2), где N - количество бит, использующееся для ограничения точности вычисления)
     */
    private Bound[] buildBounds() {
        long scale = (long) Math.ceil((double) fileSize / SCALER);

        for (int i = 0; i < 256; ++i) {
            if (byteFreq[i] != 0) {
                int newFreq = (int) (byteFreq[i] / scale);
                byteFreq[i] = (newFreq == 0) ? 1 : newFreq;
            }
        }

        return createBounds();
    }

    /**
     * для каждого байта входных данных обновляются левая и правая граница отрезка с последующей нормализацией
     * и попыткой отправить сжатые данные в поток вывода
     * @throws IOException
     */
    private void encodeBits(BufferedInputStream in, BufferedOutputStream out, Bound[] bounds) throws IOException {
        long cumulative = Arrays.stream(byteFreq).sum();
        long lowerBound = 0;
        long upperBound = TOP;
        long progressCounter = 0;
        int datByte;
        while ((datByte = in.read()) != -1) {
            progressCounter++;
            Tools.showProgress(progressCounter, fileSize);

            long currentRange = upperBound - lowerBound + 1;
            upperBound = (lowerBound + currentRange * bounds[datByte].upper / cumulative - 1);
            lowerBound = (lowerBound + currentRange * bounds[datByte].lower / cumulative);
            var result = normalizeNewBounds(lowerBound, upperBound);
            lowerBound = result.lower;
            upperBound = result.upper;
            writeBits(out);
        }

        bitsToOutput.cumulate();
        bitsToOutput.addLast(lowerBound >= LQUARTER);
        writeBits(out);
        writeRest(out);
    }
}