package com.messenger;

import com.messenger.api.Router;
import com.messenger.services.MessageService;
import com.messenger.storage.Database;

/**
 * Точка входу в застосунок.
 *
 * Запуск:
 *   java -cp out com.messenger.Main
 *   java -cp out com.messenger.Main 8080          (власний порт)
 *   java -cp out com.messenger.Main 8080 ./data   (власний порт і тека даних)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        int    port    = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String dataDir = args.length > 1 ? args[1] : "./data";

        Database       db      = new Database(dataDir);
        MessageService service = new MessageService(db);
        Router         router  = new Router(service, port);

        router.start();
        System.out.println("Дані зберігаються у: " + dataDir);
        System.out.println("Натисніть Ctrl+C для зупинки.");
    }
}
