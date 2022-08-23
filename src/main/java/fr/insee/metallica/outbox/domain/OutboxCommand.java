package fr.insee.metallica.outbox.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;

/**
 * @author jhaderer
 *
 */
@Entity
public class OutboxCommand {
	@Id
	@Column(columnDefinition = "UUID")
	private UUID id;
	
	private LocalDateTime lastHeartBeat;
	
	private boolean completed;

	@Type(type = "text")
	private String response;
	
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public LocalDateTime getLastHeartBeat() {
		return lastHeartBeat;
	}

	public void setLastHeartBeat(LocalDateTime lastHeartBeat) {
		this.lastHeartBeat = lastHeartBeat;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
}
