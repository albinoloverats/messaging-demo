package net.albinoloverats.messaging.demo.utils;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.albinoloverats.messaging.demo.MessagingFramework;

import java.util.UUID;

@RequiredArgsConstructor
public class Counters
{
	private final UUID clientId;
	private final MessagingFramework framework;
	private final MeterRegistry meterRegistry;

	public void eventSent(Object event)
	{
		increment("sent", "event", event.getClass().getName());
	}

	public void eventReceived(Object event)
	{
		increment("received", "event", event.getClass().getName());
	}

	public void querySent(Object query)
	{
		increment("sent", "query", query.getClass().getName());
	}

	public void queryReceived(Object query)
	{
		increment("received", "query", query.getClass().getName());
	}

	private void increment(String direction, String type, String message)
	{
		val tags = Tags.of(
				Tag.of("id", clientId.toString()),
				Tag.of("framework", framework.name()),
				Tag.of(type, message));
		val counter = meterRegistry.counter("demo.%s.%s".formatted(type, direction), tags);
		counter.increment();
	}
}
