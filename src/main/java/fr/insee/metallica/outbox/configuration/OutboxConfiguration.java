package fr.insee.metallica.outbox.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.insee.metallica.outbox.OutboxCommandService;
import fr.insee.metallica.outbox.aspect.TransactionalCommandAspect;
import fr.insee.metallica.outbox.domain.OutboxCommand;
import fr.insee.metallica.outbox.domain.OutboxCommandRepository;

@EnableTransactionManagement
@EntityScan(basePackageClasses = OutboxCommand.class)
@EnableJpaRepositories(basePackageClasses = OutboxCommandRepository.class)
public class OutboxConfiguration {
	@Bean
	@ConditionalOnMissingBean
	public ObjectMapper mapper() {
		return new ObjectMapper();
	}
	
	@Bean
	public TransactionalCommandAspect transactionalCommandAspect() {
		return new TransactionalCommandAspect();
	}
	
	@Bean
	public OutboxCommandService outboxCommandService() {
		return new OutboxCommandService();
	}
}
