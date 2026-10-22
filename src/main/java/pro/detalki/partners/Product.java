/*
 * detalkipro-partners — парсер карточек товаров проверенных партнёров
 * маркетплейса «ДеталькиПро».
 * Создатель и автор: Падерин Павел Владимирович
 */
package pro.detalki.partners;

import java.util.List;

/**
 * Карточка товара, собранная с сайта партнёра.
 * @author Падерин Павел Владимирович
 */
public record Product(
        String partner,
        String sku,
        String title,
        String category,
        double price,
        String description,
        String shortDescription,
        int stock,
        List<String> imageUrls) {}
