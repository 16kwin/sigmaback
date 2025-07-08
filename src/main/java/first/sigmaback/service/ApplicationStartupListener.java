package first.sigmaback.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupListener {

    @Autowired
    private DataCacheService dataCacheService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent() {
        dataCacheService.loadCache(); // Запускаем первоначальную загрузку кэша
    }
}