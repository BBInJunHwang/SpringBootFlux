package com.spring.fluxdemo;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import com.spring.fluxdemo.domain.Customer;
import com.spring.fluxdemo.domain.CustomerRepository;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

/**
 *org.springframework.r2dbc.data.. 이렇게 붙은건 지금 디플리케이트 됨 안붙은거 써야함
 *
 *
 * Configuration 통해서 서버실행시 schema.sql 실행, saveAll 통한 data save 넣고 select한다
 *
 **/

@Slf4j
@Configuration
public class DBinit {

	@Bean
    ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {

        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));

        return initializer;
    }

    @Bean
    public CommandLineRunner demo(CustomerRepository repository) {

        return (args) -> {
            // save a few customers
            repository.saveAll(Arrays.asList(new Customer("Jack", "Bauer"),
							                 new Customer("Chloe", "O'Brian"),
							                 new Customer("Kim", "Bauer"),
							                 new Customer("David", "Palmer"),
							                 new Customer("Michelle", "Dessler")))
							                 .blockLast(Duration.ofSeconds(10));

            // fetch all customers
            log.info("Customers found with findAll():");
            log.info("-------------------------------");
            repository.findAll().doOnNext(customer -> {
                log.info(customer.toString());
            }).blockLast(Duration.ofSeconds(10));

            log.info("");

            // fetch an individual customer by ID
			repository.findById(1L).doOnNext(customer -> {
				log.info("Customer found with findById(1L):");
				log.info("--------------------------------");
				log.info(customer.toString());
				log.info("");
			}).block(Duration.ofSeconds(10));


            // fetch customers by last name
            log.info("Customer found with findByLastName('Bauer'):");
            log.info("--------------------------------------------");
            repository.findByLastName("Bauer").doOnNext(bauer -> {
                log.info(bauer.toString());
            }).blockLast(Duration.ofSeconds(10));;
            log.info("");
        };
    }
}
