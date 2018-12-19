package com.kuaidan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MicroKuaidanBackSsoServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroKuaidanBackSsoServerApplication.class, args);
	}

}

