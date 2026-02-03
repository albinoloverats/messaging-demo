package net.albinoloverats.messaging.demo.service;

import com.jcabi.aspects.Loggable;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.albinoloverats.messaging.client.MessagingGateway;
import net.albinoloverats.messaging.demo.MessagingFramework;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import net.albinoloverats.messaging.demo.messages.ExampleQuery;
import net.albinoloverats.messaging.demo.messages.ExampleResponse;
import net.albinoloverats.messaging.demo.store.Event;
import net.albinoloverats.messaging.demo.store.EventMapper;
import net.albinoloverats.messaging.demo.store.EventRepository;
import net.albinoloverats.messaging.demo.utils.Counters;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@ProcessingGroup("demoProcessor")
@Component
@Profile("axon")
@Slf4j
public class AxonService
{
	private final EventRepository eventRepository;
	private final EventMapper eventMapper;
	private final Counters counters;
	private final EventGateway eventGateway;
	private final QueryGateway queryGateway;

	public AxonService(EventRepository eventRepository,
	                   EventMapper eventMapper,
	                   MessagingGateway messagingGateway,
	                   MeterRegistry meterRegistry,
	                   EventGateway eventGateway,
	                   QueryGateway queryGateway)
	{
		this.eventRepository = eventRepository;
		this.eventMapper = eventMapper;
		counters = new Counters(messagingGateway.getClientId(),
				MessagingFramework.AXON,
				meterRegistry);
		this.eventGateway = eventGateway;
		this.queryGateway = queryGateway;
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
					eventGateway.publish(event);
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
		return queryGateway.query(query, ResponseTypes.instanceOf(ExampleResponse.class))
				.thenApply(ExampleResponse::events);
	}

	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<Optional<Event>> query(UUID ids)
	{
		val query = new ExampleQuery(Set.of(ids));
		counters.querySent(query);
		log.debug("Issuing query {}", query);
		return queryGateway.query(query, ResponseTypes.instanceOf(ExampleResponse.class))
				.thenApply(response -> response.events().stream().findFirst());
	}

	/*
	 * Event/Query Handling
	 */

	@EventHandler
	@Loggable(value = Loggable.TRACE, prepend = true)
	public void handle(ExampleEvent event)
	{
		counters.eventReceived(event);
		val remaining = event.remaining() - 1;
		if (remaining <= 0)
		{
			var entity = eventMapper.toEntity(event, MessagingFramework.AXON);
			entity = eventRepository.save(entity);
			log.debug("AXON event persisted {}", entity);
		}
		else
		{
			val updated = event.withRemaining(remaining);
			eventGateway.publish(updated);
			counters.eventSent(updated);
			log.debug("Forwarded AXON event {}", updated);
		}
	}

	@QueryHandler
	@Loggable(value = Loggable.TRACE, prepend = true)
	public ExampleResponse handle(ExampleQuery query)
	{
		counters.queryReceived(query);
		val ids = query.ids();
		List<Event> events;
		if (ids.isEmpty())
		{
			events = eventRepository.findAllByFramework(MessagingFramework.AXON);
		}
		else
		{
			events = eventRepository.findAllById(query.ids());
		}
		log.debug("Found {} entities", events.size());
		return new ExampleResponse(new TreeSet<>(events));
	}
}
