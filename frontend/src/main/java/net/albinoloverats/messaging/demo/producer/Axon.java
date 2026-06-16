package net.albinoloverats.messaging.demo.producer;

import com.jcabi.aspects.Loggable;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.albinoloverats.messaging.client.MessagingGateway;
import net.albinoloverats.messaging.demo.MessagingFramework;
import net.albinoloverats.messaging.demo.dto.Event;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import net.albinoloverats.messaging.demo.messages.ExampleQuery;
import net.albinoloverats.messaging.demo.messages.ExampleResponse;
import net.albinoloverats.messaging.demo.utils.Counters;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.unitofwork.ProcessingContext;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

//@ProcessingGroup("demoProcessor")
@Component
@Profile("axon")
@Slf4j
public class Axon
{
	private final Counters counters;
	private final EventGateway eventGateway;
	private final QueryGateway queryGateway;
	private final ProcessingContext processingContext;

	public Axon(MessagingGateway messagingGateway,
	            MeterRegistry meterRegistry,
	            EventGateway eventGateway,
	            QueryGateway queryGateway,
	            ProcessingContext processingContext)
	{
		counters = new Counters(messagingGateway.getClientId(),
				MessagingFramework.AXON,
				meterRegistry);
		this.eventGateway = eventGateway;
		this.queryGateway = queryGateway;
		this.processingContext = processingContext;
	}

	/*
	 * Event/Query Publishing
	 */

	@Loggable(value = Loggable.TRACE, prepend = true)
	public Set<UUID> publish(int quantity, int hops, String body)
	{
		val ids = new HashSet<UUID>();
		IntStream.range(0, quantity)
				.parallel()
				.forEach(i ->
				{
					val event = new ExampleEvent(body, hops);
					eventGateway.publish(processingContext, List.of(event));
					counters.eventSent(event);
					log.debug("Published AXON event {}", event);
					ids.add(event.eventId());
				});
		return ids;
	}

	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<Set<Event>> query(Set<UUID> ids)
	{
		val query = new ExampleQuery(ids);
		counters.querySent(query);
		log.debug("Issuing query {}", query);
		return queryGateway.query(query, ExampleResponse.class)
				.thenApply(ExampleResponse::events);
	}

	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<Optional<Event>> query(UUID ids)
	{
		val query = new ExampleQuery(Set.of(ids));
		counters.querySent(query);
		log.debug("Issuing query {}", query);
		return queryGateway.query(query, ExampleResponse.class)
				.thenApply(response -> response.events().stream().findFirst());
	}
}
