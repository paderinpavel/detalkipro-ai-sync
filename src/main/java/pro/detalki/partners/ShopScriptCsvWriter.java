/*
 * detalkipro-partners — парсер карточек товаров проверенных партнёров
 * маркетплейса «ДеталькиПро».
 * Создатель и автор: Падерин Павел Владимирович
 */
package pro.detalki.partners;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Выгрузка собранных товаров в CSV формата импорта WebAsyst Shop-Script:
 * разделитель «;», кодировка UTF-8, изображения — ссылками.
 * @author Падерин Павел Владимирович
 */
public class ShopScriptCsvWriter {

    private static final String HEADER = "Тип;Наименование;Категория;Описание;"
            + "Краткое описание;Артикул;Цена;В наличии;Статус;Изображение";

    public Path write(List<Product> products, String partner) throws IOException {
        Files.createDirectories(Paths.get("import"));
        Path file = Paths.get("import", partner + "-" + LocalDate.now() + ".csv");
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(HEADER + "\n");
            for (Product p : products) {
                w.write(String.join(";",
                        "product",
                        esc(p.title()),
                        esc(p.category()),
                        esc(p.description()),
                        esc(p.shortDescription()),
                        esc(p.sku()),
                        String.valueOf(p.price()).replace('.', ','),
                        String.valueOf(p.stock()),
                        "1",
                        esc(p.imageUrls().isEmpty() ? "" : p.imageUrls().get(0))
                ) + "\n");
            }
        }
        return file;
    }

    /** Кавычки удваиваются, точки с запятой заменяются — иначе сломаются колонки. */
    private String esc(String s) {
        return s == null ? "" : s.replace("\"", "\"\"").replace(";", ",");
    }
}
