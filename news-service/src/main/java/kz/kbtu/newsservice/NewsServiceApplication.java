package kz.kbtu.newsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"kz.kbtu.newsservice", "kz.kbtu.common"})
@EntityScan(basePackages = {"kz.kbtu.common.entity"})
@EnableJpaRepositories(basePackages = {"kz.kbtu.newsservice.repository"})
public class NewsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NewsServiceApplication.class, args);
	}

}
