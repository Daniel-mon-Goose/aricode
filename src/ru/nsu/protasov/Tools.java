package ru.nsu.protasov;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Paths;


/**
 * набор вспомогательных методов
 */
public class Tools {
    private static final long MARK = Tools.pow(2, 24);
    private static final long MB = Tools.pow(2, 20);

    /**
     * целочисленное возведение в степень
     */
    public static long pow(long a, int b) {
        if (b == 0) {
            return 1;
        }
        if (b == 1) {
            return a;
        }

        return (b % 2 == 0) ? pow(a * a, b / 2) : a * pow ( a * a, b/2);
    }

    /**
     * вывод прогресса кодирования и декодирования для бфайлов больше 16 MB
     */
    public static void showProgress(long progress, long counter) {
        if (progress % MARK == 0) {
            System.out.println(Long.toString(progress / MARK * 16) + '/' + counter / MB + " MB");
        }
    }

    /**
     * проверка формата метафайла
     * @throws FileSystemException если тип файла не совпадает с .inf
     */
    public static void checkFileType(String metaPath) throws FileSystemException {
        if (!Paths.get(metaPath).getFileName().toString().endsWith(".inf")) {
            throw new FileSystemException("Metafile type must be .inf");
        }
    }

    /**
     * вывод метаданных (размера закодированного файла и таблицы частот байтов)
     */
    public static void readMeta(String metaPath) throws IOException {
        checkFileType(metaPath);
        try (var meta = new DataInputStream(new BufferedInputStream(new FileInputStream(metaPath)))) {
            System.out.println(meta.readLong());
            for (int i = 0; i < 256; ++i) {
                System.out.print(i);
                System.out.print(": ");
                System.out.println(meta.readLong());
            }
        }
    }
}
