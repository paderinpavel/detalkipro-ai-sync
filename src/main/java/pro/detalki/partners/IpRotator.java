/*
 * detalkipro-partners — парсер карточек товаров проверенных партнёров
 * маркетплейса «ДеталькиПро».
 * Создатель и автор: Падерин Павел Владимирович
 */
package pro.detalki.partners;

import java.time.Instant;

/**
 * Ротация IP: переподключает модем, если сайт забанил по частоте запросов.
 * Команда переподключения задаётся в partners.json (поле ip.rotateCommand)
 * и подбирается под модель модема.
 * @author Падерин Павел Владимирович
 */
public class IpRotator {

    private static final long MIN_INTERVAL_MS = 45_000; // не чаще раза в 45 секунд
    private Instant lastRotate = Instant.EPOCH;
    private final String command;

    public IpRotator(String command) {
        this.command = command;
    }

    public synchronized void rotate() {
        long since = Instant.now().toEpochMilli() - lastRotate.toEpochMilli();
        if (since < MIN_INTERVAL_MS) {
            System.out.println("rotator: cooling down, skipped");
            return;
        }
        try {
            System.out.println("rotator: switching IP...");
            new ProcessBuilder("/bin/bash", "-c", command)
                    .inheritIO().start().waitFor();
            lastRotate = Instant.now();
            Thread.sleep(10_000); // дать сети подняться
            System.out.println("rotator: new IP assumed");
        } catch (Exception e) {
            System.err.println("rotator failed: " + e.getMessage());
        }
    }
}
