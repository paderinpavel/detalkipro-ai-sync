/*
 * detalkipro-partners — парсер карточек товаров проверенных партнёров
 * маркетплейса «ДеталькиПро».
 * Создатель и автор: Падерин Павел Владимирович
 */
package pro.detalki.partners;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Map;

/**
 * Пайплайн: читает partners.json, краулит домены партнёров, пишет CSV
 * для импорта в WebAsyst Shop-Script.
 * @author Падерин Павел Владимирович
 */
public class Main {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        var mapper = new ObjectMapper();
        var cfg = mapper.readTree(new File("partners.json"));

        var ip = cfg.get("ip");
        var rotator = new IpRotator(ip.get("rotateCommand").asText());

        var writer = new ShopScriptCsvWriter();

        for (var node : cfg.get("partners")) {
            var partner = new PartnerConfig(
                    node.get("name").asText(),
                    node.get("startUrl").asText(),
                    mapper.convertValue(node.get("selectors"), Map.class));
            var products = new Crawler(partner, rotator).crawl();
            var file = writer.write(products, partner.name());
            System.out.printf("%s: %d товаров → %s%n",
                    partner.name(), products.size(), file);
        }
    }
}
