/*
 * detalkipro-partners — парсер карточек товаров проверенных партнёров
 * маркетплейса «ДеталькиПро».
 * Создатель и автор: Падерин Павел Владимирович
 */
package pro.detalki.partners;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

/**
 * Краулер: обходит весь домен партнёра от стартовой страницы, находит
 * карточки товаров, при бане (403/429) дёргает ротатор IP и продолжается.
 * @author Падерин Павел Владимирович
 */
public class Crawler {

    private static final String UA =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";
    private static final int THREADS = 3; // вежливость: не более 3 потоков

    private final PartnerConfig partner;
    private final IpRotator rotator;
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final Queue<String> queue = new ConcurrentLinkedQueue<>();
    private final List<Product> products = Collections.synchronizedList(new ArrayList<>());

    public Crawler(PartnerConfig partner, IpRotator rotator) {
        this.partner = partner;
        this.rotator = rotator;
    }

    public List<Product> crawl() throws InterruptedException {
        queue.add(partner.startUrl());
        var pool = Executors.newFixedThreadPool(THREADS);
        for (int i = 0; i < THREADS; i++) pool.submit(this::loop);
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.HOURS);
        return List.copyOf(products);
    }

    private void loop() {
        while (!queue.isEmpty()) {
            String url = queue.poll();
            if (url == null || !visited.add(normalize(url))) continue;
            try {
                Thread.sleep(300 + new Random().nextInt(500));
                Document doc = fetch(url);
                if (doc == null) continue;

                if (isProductCard(doc)) {
                    products.add(toProduct(doc, url));
                } else {
                    for (Element a : doc.select("a[href]")) {
                        String next = a.absUrl("href");
                        if (inDomain(next) && looksLikeCatalog(next)) queue.add(next);
                    }
                }
            } catch (BanException e) {
                rotator.rotate();   // забанили — меняем IP и продолжаем
                queue.add(url);     // вернём страницу в очередь
            } catch (IOException e) {
                System.err.println("skip " + url + ": " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private Document fetch(String url) throws IOException, BanException {
        var resp = Jsoup.connect(url).userAgent(UA).timeout(15000).execute();
        if (resp.statusCode() == 403 || resp.statusCode() == 429) {
            throw new BanException();
        }
        return resp.parse();
    }

    private boolean inDomain(String url) {
        String host = URI.create(url).getHost();
        return host != null && host.equals(partnerDomain());
    }

    /** Страницы, которые не парсим: корзина, кабинет, админка, сессионные фильтры. */
    private boolean looksLikeCatalog(String url) {
        String u = url.toLowerCase();
        return !u.contains("cart") && !u.contains("login") && !u.contains("admin")
                && !u.contains("order") && !u.contains("checkout")
                && !u.contains("?sid=") && !u.contains("logout");
    }

    private boolean isProductCard(Document doc) {
        var sel = partner.selectors();
        // карточка — там, где есть заголовок товара И цена
        return !doc.select(sel.get("title")).isEmpty()
                && !doc.select(sel.get("price")).isEmpty();
    }

    private Product toProduct(Document doc, String url) {
        var sel = partner.selectors();
        String text(String key) {
            var q = sel.get(key);
            return q == null ? "" : doc.select(q).text().trim();
        }

        List<String> images = new ArrayList<>();
        var imgSel = sel.get("images");
        if (imgSel != null) {
            for (Element img : doc.select(imgSel)) {
                if (!img.absUrl("src").isBlank()) images.add(img.absUrl("src"));
            }
        }

        String desc = text("description");
        String shortDesc = desc.isBlank() ? ""
                : (desc.length() <= 300 ? desc : desc.substring(0, 300) + "…");
        String sku = text("sku").isBlank() ? partner.name() + "-" + url.hashCode() : text("sku");

        return new Product(partner.name(), sku, text("title"),
                doc.select(".breadcrumbs span:last-of-type").text().trim(),
                parsePrice(text("price")), desc, shortDesc,
                parseStock(text("stock")), images);
    }

    private double parsePrice(String raw) {
        String digits = raw.replaceAll("[^0-9,\.]", "").replace(",", ".");
        return digits.isBlank() ? 0.0 : Double.parseDouble(digits);
    }

    private int parseStock(String raw) {
        return raw.matches(".*[0-9]+.*")
                ? Integer.parseInt(raw.replaceAll("[^0-9]", "")) : 1;
    }

    private String partnerDomain() {
        String host = URI.create(partner.startUrl()).getHost();
        return host == null ? "" : host.replaceFirst("^www\\.", "");
    }

    private String normalize(String url) {
        return url.split("#")[0];
    }
}
