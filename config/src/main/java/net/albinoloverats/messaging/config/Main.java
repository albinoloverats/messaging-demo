package net.albinoloverats.messaging.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class Main
{
	public static void main(final String[] args)
	{
		SpringApplication.run(Main.class, args);
	}
}
