### Как пользовать progu.
На вход подаются либо 4, либо 2 ключа.
1) 4 ключа: режим (`-e` - кодирование, `-d` - декодирование), далее входной файл, выходной файл, файл метаданных
с расширением `.inf` (для первого режима входной файл - оригинал, выходной - закодированный, для второго
входной - закодированный, выходной - декодированный оригинального формата)
2) 2 ключа: режим (всегда `-m`), файл метаданных для вывода содержания в формате `.inf`


### Арифметическое кодирование.
Арифметическое кодирование и декодирование основано на отображении текста (или байтов) в вещественное число, которое
уменьшается по мере кодирования. Однако компьютеры не способны производить расчёты вещественных чисел с
бесконечной точностью.

Один из выходов - использовать целочисленную арифметику ограниченной точности и представление дробной части в виде целого числа.
Причём верхняя и нижняя граница моделируют бесконечные дроби, в конце нижней границы бесконечные нули,
верхней - бесконечные единицы. Это позволит реализовать идею выделения одинаковых битов в границах.

Пример: $0.111111_{2}... = 1$

Такая реализация сталкивается со следующими проблемами:

1) **Underflow** (отрицательное переполнение) - может произойти, если обе границы приблизились очень близко к
середине рассматриваемого масштабированного интервала, но их ведущие биты остаются различными. Если произойдёт переполнение,
то алгоритм не сможет вывести и одного бита до самого `EOF`. Для предотвращения необходимо проверять, что новый
отрезок на очередном шаге попал между первой и третьей четвертью рассматриваемого отрезка и гарантировать, что
максимальное количество рассматриваемых символов (байтов) не превышает $2^{N - 2} - 1$.

2) **Обычное переполнение** - при масштабировании отрезка может произойти переполнение при умножении расстояния
на одну из границ частотного отрезка. В силу ограничений на границы частот $2^{N - 2} - 1$ и расстояния $2^{N}$
необходимо, чтобы переменные, в которых они хранятся, вмещали в себя не менее $2N - 2$ бит.

### Особенности работы на разных типах данных.
Тестирование на различных типах данных показало, что алгоритм неплохо сжимает текстовые файлы и изображения формата BMP,
однако изображения других форматов, бинарные и видео файлы практически не поддаются сжатию, выдавая закодированный файл приблизительного того же
размера (сжатие видео файлов размера более 300 MB дало выигрыш не более половины MB).

Связано это с теоретическим пределом и с тем, как распределена частота битов. Функция 

$$-p log_{2}(p) - (1 - p) log_{2}(1 - p),$$

где $p$ - частота, с которой появляются биты $0$, принимает максимальное значение, равное 1, при $p = \frac{1}{2}$
Это означает, что каждый закодированный бит мы сможем сжать (*нет*) до 1 бита.

Это подтверждается, если проанализировать таблицу частот из файла метаданных: при пренебрежительно малом сжатии
байты в файле (а значит и биты) встречаются с почти одинаковой частотой.
Это справедливо для большинства изображений, бинарных и видео данных (биты встречаются хаотически и равновероятно),
но не для текстов и BMP-изображений (для таких файлов тот или иной бит встречается гораздо чаще, чем другой).