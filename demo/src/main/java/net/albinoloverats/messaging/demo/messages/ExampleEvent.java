package net.albinoloverats.messaging.demo.messages;

import lombok.With;
import net.albinoloverats.messaging.common.annotations.Event;

import java.time.Instant;
import java.util.UUID;

@Event
public record ExampleEvent(UUID eventId, String data, Integer hops, @With Integer remaining, Instant created)
{
	public ExampleEvent(String data, Integer hops)
	{
		this(UUID.randomUUID(), data, hops, hops, Instant.now());
	}

	public ExampleEvent(String data)
	{
		this(data, 1);
	}
}
