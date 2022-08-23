package fr.insee.metallica.outbox;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insee.metallica.outbox.domain.OutboxCommand;
import fr.insee.metallica.outbox.domain.OutboxCommandRepository;

@RestController
@RequestMapping("/command")
public class OutboxCommandController {
	@Autowired
	private OutboxCommandRepository repository;
	
	@PostMapping(path = "/{id}")
	public OutboxCommand getCommand(@PathVariable("id") UUID id) throws InterruptedException {
		return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

}
