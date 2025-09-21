package first.sigmaback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class SigmabackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SigmabackApplication.class, args);
	}
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://194.87.56.253:3000", "http://194.87.56.253:3001")); // Разрешите ваш фронтенд
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Разрешенные методы
        config.setAllowedHeaders(List.of("*")); // Разрешите все заголовки (или перечислите нужные)
        config.setAllowCredentials(true); // Если используете куки или авторизацию
        config.setMaxAge(3600L); // Время жизни preflight-ответа

        source.registerCorsConfiguration("/**", config); // Применить ко всем эндпоинтам
        return new CorsFilter(source);
    }
}