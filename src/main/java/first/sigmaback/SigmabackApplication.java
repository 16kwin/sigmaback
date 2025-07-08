package first.sigmaback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SigmabackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SigmabackApplication.class, args);
	}

}