package net.albinoloverats.messaging.demo.dto;

import net.albinoloverats.messaging.demo.MessagingFramework;

import java.time.Instant;
import java.util.UUID;

public record Event(UUID id,
                    String data,
                    Integer hops,
                    Instant created,
                    Instant persisted,
                    MessagingFramework framework) implements Comparable<Event>
{
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
