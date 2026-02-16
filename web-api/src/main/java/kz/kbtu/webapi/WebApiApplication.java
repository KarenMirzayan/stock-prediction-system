package kz.kbtu.webapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"kz.kbtu.webapi", "kz.kbtu.common"})
@EntityScan(basePackages = {"kz.kbtu.common.entity"})
@EnableJpaRepositories(basePackages = {"kz.kbtu.webapi.repository"})
public class WebApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApiApplication.class, args);
    }
}
