/*
 * detalkipro-partners — парсер карточек товаров проверенных партнёров
 * маркетплейса «ДеталькиПро».
 * Создатель и автор: Падерин Павел Владимирович
 */
package pro.detalki.partners;

import java.util.Map;

/**
 * Конфигурация проверенного партнёра: стартовый URL и CSS-селекторы карточек.
 * @author Падерин Павел Владимирович
 */
public record PartnerConfig(String name, String startUrl, Map<String, String> selectors) {}
