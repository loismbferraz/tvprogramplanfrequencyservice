package com.challenge.tvprogramplanfrequencyservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@OpenAPIDefinition(
        info = @Info(
                version = "0.0.1",
                title = "7.1 Tech Hub Challenge - TV Program Plan Frequency Service",
                description = "TBF",
                contact = @Contact(
                        name = "Luís Ferraz",
                        url = "TBF",
                        email = "loismbferraz@gmail.com")),
        servers = @Server(description = "TV Program Plan Frequency Service", url = "/tv-prog-plan-freq-serv"))

@SpringBootApplication
public class TvprogramplanfrequencyserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TvprogramplanfrequencyserviceApplication.class, args);
    }

}
