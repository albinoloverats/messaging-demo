package net.albinoloverats.messaging.demo.service;

import com.jcabi.aspects.Loggable;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.albinoloverats.messaging.client.MessagingGateway;
import net.albinoloverats.messaging.common.annotations.EventHandler;
import net.albinoloverats.messaging.common.annotations.QueryHandler;
import net.albinoloverats.messaging.demo.MessagingFramework;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import net.albinoloverats.messaging.demo.messages.ExampleQuery;
import net.albinoloverats.messaging.demo.messages.ExampleResponse;
import net.albinoloverats.messaging.demo.messages.ExampleUnhandledEvent;
import net.albinoloverats.messaging.demo.messages.ExampleUnhandledQuery;
import net.albinoloverats.messaging.demo.store.Event;
import net.albinoloverats.messaging.demo.store.EventMapper;
import net.albinoloverats.messaging.demo.store.EventRepository;
import net.albinoloverats.messaging.demo.utils.Counters;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Component
@Slf4j
public class AmaService
{
	private final EventRepository eventRepository;
	private final EventMapper eventMapper;
	private final Counters counters;
	private final MessagingGateway messagingGateway;

	public AmaService(EventRepository eventRepository,
	                  EventMapper eventMapper,
	                  MessagingGateway messagingGateway,
	                  MeterRegistry meterRegistry)
	{
		this.eventRepository = eventRepository;
		this.eventMapper = eventMapper;
		this.messagingGateway = messagingGateway;
		counters = new Counters(messagingGateway.getClientId(),
				MessagingFramework.AMA,
				meterRegistry);
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
					messagingGateway.publish(event);
					counters.eventSent(event);
					log.debug("Published event {}", event);
					ids.add(event.eventId());
				});
		return ids;
	}

	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<Void> publish(String body)
	{
		val unhandled = new ExampleUnhandledEvent(body);
		log.debug("Publishing event {}", unhandled);
		counters.eventSent(unhandled);
		return messagingGateway.publish(unhandled);
	}

	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<Set<Event>> query(Set<UUID> ids)
	{
		val query = new ExampleQuery(ids);
		counters.querySent(query);
		log.debug("Issuing query {}", query);
		return messagingGateway.<ExampleResponse>query(query)
				.thenApply(ExampleResponse::events);
	}

	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<Optional<Event>> query(UUID ids)
	{
		val query = new ExampleQuery(Set.of(ids));
		counters.querySent(query);
		log.debug("Issuing query {}", query);
		return messagingGateway.<ExampleResponse>query(query)
				.thenApply(response -> response.events().stream().findFirst());
	}

	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<Set<Event>> query()
	{
		val query = new ExampleUnhandledQuery(MessagingFramework.AMA.name());
		counters.querySent(query);
		log.debug("Attempting query {}", query);
		return messagingGateway.<ExampleResponse>query(query)
				.thenApply(ExampleResponse::events);
	}

	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<Set<Event>> exceptionalQuery()
	{
		val query = ExampleQuery.exceptional();
		counters.querySent(query);
		log.debug("Attempting query {}", query);
		return messagingGateway.<ExampleResponse>query(query)
				.thenApply(ExampleResponse::events);
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
			var entity = eventMapper.toEntity(event, MessagingFramework.AMA);
			entity = eventRepository.save(entity);
			log.debug("Event persisted {}", entity);
		}
		else
		{
			val updated = event.withRemaining(remaining);
			messagingGateway.publish(updated);
			counters.eventSent(updated);
			log.debug("Forwarded event {}", updated);
		}
	}

	@QueryHandler
	@Loggable(value = Loggable.TRACE, prepend = true)
	public ExampleResponse handle(ExampleQuery query)
	{
		counters.queryReceived(query);
		if (query.isExceptional())
		{
			throw new RuntimeException("Query throwing exception.");
		}
		val ids = query.ids();
		List<Event> events;
		if (ids.isEmpty())
		{
			events = eventRepository.findAllByFramework(MessagingFramework.AMA);
		}
		else
		{
			events = eventRepository.findAllById(query.ids());
		}
		log.debug("Found {} entities", events.size());
		return new ExampleResponse(new TreeSet<>(events));
	}
}
