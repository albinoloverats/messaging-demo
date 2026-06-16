package net.albinoloverats.messaging.demo.store;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.albinoloverats.messaging.demo.MessagingFramework;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EventEntity implements Comparable<EventEntity>
{
	@Id
	@EqualsAndHashCode.Include
	private UUID id;
	private String data;
	private Integer hops;
	private Instant created;
	private Instant persisted;
	private MessagingFramework framework;

	@Override
	public int compareTo(EventEntity event)
	{
		var order = created.compareTo(event.created);
		if (order == 0)
		{
			order += id.compareTo(event.id);
		}
		return order;
	}
}
