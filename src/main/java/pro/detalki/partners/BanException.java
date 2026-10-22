/*
 * detalkipro-partners — парсер карточек товаров проверенных партнёров
 * маркетплейса «ДеталькиПро».
 * Создатель и автор: Падерин Павел Владимирович
 */
package pro.detalki.partners;

/**
 * Сигнал ротатору IP: сайт ответил 403/429.
 * @author Падерин Павел Владимирович
 */
public class BanException extends Exception {
    public BanException() {
        super("site banned us: 403/429");
    }
}
