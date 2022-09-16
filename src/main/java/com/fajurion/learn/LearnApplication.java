package com.fajurion.learn;

import com.fajurion.learn.repository.account.Account;
import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.ranks.Rank;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.util.AccountUtil;
import com.fajurion.learn.util.Configuration;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import reactor.core.publisher.Mono;

import java.awt.image.BufferedImage;
import java.util.UUID;

@SpringBootApplication
public class LearnApplication {

	/**
	 * Main start function
	 *
	 * @param args Start arguments
	 */
	public static void main(String[] args) {
		Configuration.init();
		SpringApplication.run(LearnApplication.class, args);
	}

	/**
	 * Creates the tables in the database
	 * Credit to someone on StackOverflow
	 * ^ if you read this and this code belongs to you, please contact me, and I'll credit you here.
	 *
	 * @param connectionFactory Connection Factory
	 * @return Connection Factory Initializer with Database Populator attached
	 */
	@Bean
	public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {

		ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
		initializer.setConnectionFactory(connectionFactory);

		CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
		populator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));
		initializer.setDatabasePopulator(populator);

		return initializer;
	}

	// Rank repository for storing all the ranks
	private final RankRepository rankRepository;

	// Account repository that stores all the user accounts
	private final AccountRepository accountRepository;

	@Autowired
	public LearnApplication(RankRepository rankRepository, AccountRepository accountRepository) {
		this.rankRepository = rankRepository;
		this.accountRepository = accountRepository;
	}

	@Bean
	public HttpMessageConverter<BufferedImage> bufferedImageHttpMessageConverter() {
		return new BufferedImageHttpMessageConverter();
	}

	/**
	 * Create ranks on application startup
	 *
	 * @param event Startup event
	 */
	@EventListener
	public void handleAppStart(ApplicationStartedEvent event) {

		// Create default admin and user rank
		System.out.println("[Learn] Creating ranks..");
		createRank("Admin", 100);
		createRank("User", 0);
		System.out.println("[Learn] Ranks created.");

		// Create default admin account if not exists
		createDefaultAccount("Administrator", "Admin", UUID.randomUUID().toString());

	}

	/**
	 * Creates a default user account
	 *
	 * @param name Name of the user
	 * @param rank Name of the rank
	 */
	private void createDefaultAccount(String name, String rank, String password) {
		accountRepository.getAccountsByUsername(name).hasElements().flatMap(exists -> {

			// Check if account exists
			if(exists) {
				return Mono.error(new RuntimeException(name + " already exists."));
			}

			return accountRepository.save(new Account(name, "", rank, AccountUtil.getHash(name, password), "", 0));
		}).map(account -> {
			System.out.println(account.getUsername() + " account created.");

			System.out.println(" ");
			System.out.println("ADMIN PASSWORD: " + password);
			System.out.println("IMPORTANT: PLEASE SAVE!");
			System.out.println(" ");

			return account;
		}).onErrorResume(e -> {
			System.out.println(e.getMessage());
			return Mono.empty();
		}).block();
	}

	/**
	 * Creates a rank with a permission level
	 *
	 * @param name Name of the rank
	 * @param level Permission level of the rank
	 */
	private void createRank(String name, int level) {
		rankRepository.getRankByName(name).hasElement().flatMap(exists -> {

			if(exists) {
				return Mono.error(new RuntimeException(name + " already exists."));
			}

			return rankRepository.save(new Rank(name, level));
		}).map(rank -> {
			System.out.println(name + " created.");

			return rank;
		}).onErrorResume(e -> {
			System.out.println(e.getMessage());
			return Mono.empty();
		}).block();
	}

}
