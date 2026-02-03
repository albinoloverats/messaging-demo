package net.albinoloverats.messaging.demo.store;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.albinoloverats.messaging.demo.MessagingFramework;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class Event implements Comparable<Event>
{
	@Id
	private UUID id;
	private String data;
	private Integer hops;
	private Instant created;
	private Instant persisted;
	private MessagingFramework framework;

	@Override
	public int compareTo(Event event)
	{
		var order = created.compareTo(event.created);
		if (order == 0)
		{
			order += id.compareTo(event.id);
		}
		return order;
	}

	@Override
	public boolean equals(Object object)
	{
		if (object instanceof Event other)
		{
			return id.equals(other.id);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}
}
