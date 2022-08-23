package fr.insee.metallica.outbox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.insee.metallica.outbox.domain.OutboxCommandRepository;

@Service
public class OutboxCommandService {
	private Set<UUID> currentlyProcessed = new HashSet<>();
	
	private Map<UUID, List<CompletableFuture<String>>> awaitingResult = new HashMap<>();
	
	@Autowired
	private TransactionTemplate transactionTemplate;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private OutboxCommandRepository repository;
	
	@Autowired
	private HttpServletRequest request;
	
	@Scheduled(fixedDelay = 5000)
	public void updateHeartBeat() {
		List<UUID> toUpdate;
		synchronized (currentlyProcessed) {
			toUpdate = List.copyOf(currentlyProcessed);
		}
		toUpdate.forEach(repository::heartBeat);
	}

	@Scheduled(fixedDelay = 5000)
	public void removeLost() {
		repository.removeLost();
	}
	
	@Scheduled(fixedDelay = 5000)
	public void findCompleted() {
		Map<UUID, List<CompletableFuture<String>>> awaitingResult;
		synchronized (this.awaitingResult) {
			awaitingResult = Map.copyOf(this.awaitingResult);
		}
		awaitingResult.forEach((id, futures) -> {
			var command = repository.findById(id);
			if (!command.isPresent()) {
				futures.forEach(f -> f.completeExceptionally(new RuntimeException("Command was aborted")));
			}
			if (!command.get().isCompleted()) {
				return;
			}
			futures.forEach(f -> f.complete(command.get().getResponse()));
			
			synchronized (this.awaitingResult) {
				this.awaitingResult.remove(id);
			}
		});
	}
	
	public <T> Future<T> execute(TransactionCallback<T> transactionCallback, Class<T> result) {
		var commandIdString = request.getHeader("commandId");
		if (commandIdString == null) {
			throw new RuntimeException("No command Id available in request header");
		}
		try {
			var commandId = UUID.fromString(commandIdString);
			var commandProcessed = repository.findById(commandId);
			if (commandProcessed.isPresent()) {
				if (commandProcessed.get().isCompleted()) {
					return new AsyncResult<T>(mapper.readValue(commandProcessed.get().getResponse(), result));
				}
				return waitForResult(commandId, result);
			} else {
				if (!repository.takeOwnership(commandId)) {
					return waitForResult(commandId, result);
				}
			}

			return new AsyncResult<T>(transactionTemplate.execute((status) -> {
				try {
					synchronized (currentlyProcessed) {
						currentlyProcessed.add(commandId);
					}
					var response = transactionCallback.doInTransaction(status);
					var command = repository.findById(commandId).orElseThrow();
					command.setCompleted(true);
					command.setResponse(mapper.writeValueAsString(response));
					repository.save(command);
					return response;
				} catch (JsonProcessingException e) {
					repository.removeError(commandId);
					throw new RuntimeException("Could not serialize response");
				} catch (RuntimeException e) {
					repository.removeError(commandId);
					throw e;
				}
				finally {
					synchronized (currentlyProcessed) {
						currentlyProcessed.remove(commandId);
					}
				}
			}));
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Badly formatted command Id in request header");
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Could not deserialize previous response");
		}
	}

	private <T> Future<T> waitForResult(UUID commandId, Class<T> result) {
		var future = new CompletableFuture<String>();
		synchronized (awaitingResult) {
			// ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getResponse()
			var futures = awaitingResult.computeIfAbsent(commandId, (uuid) -> new LinkedList<>());
			futures.add(future);
		}
		return future.thenApply(response -> {
			try {
				return mapper.readValue(response, result);
			} catch (JsonProcessingException e) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot deserialize response");
			}
		});
	}
}
