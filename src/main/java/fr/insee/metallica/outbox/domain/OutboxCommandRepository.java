package fr.insee.metallica.outbox.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxCommandRepository extends JpaRepository<OutboxCommand, UUID>{
	List<OutboxCommand> findByLastHeartBeatLessThanAndCompleted(LocalDateTime before, boolean completed);
	
	@Transactional(value = TxType.REQUIRES_NEW)
	default boolean takeOwnership(UUID commandId) {
		var command = new OutboxCommand();
		command.setId(commandId);
		command.setLastHeartBeat(LocalDateTime.now());
		command.setCompleted(false);
		try {
			save(command);
			return true;
		} catch(DuplicateKeyException e) {
			return false;
		}
	}
	
	@Transactional(value = TxType.REQUIRES_NEW)
	default void heartBeat(UUID commandId) {
		var command = getById(commandId);
		command.setLastHeartBeat(LocalDateTime.now());
		save(command);
	}

	@Transactional
	default void removeLost() {
		var commandsLost = findByLastHeartBeatLessThanAndCompleted(LocalDateTime.now().minusSeconds(10), false);
		deleteAll(commandsLost);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	default void removeError(UUID commandId) {
		deleteById(commandId);
	}

}
