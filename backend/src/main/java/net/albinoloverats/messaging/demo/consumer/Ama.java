package net.albinoloverats.messaging.demo.consumer;

import com.jcabi.aspects.Loggable;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.albinoloverats.messaging.client.MessagingGateway;
import net.albinoloverats.messaging.common.annotations.EventHandler;
import net.albinoloverats.messaging.common.annotations.QueryHandler;
import net.albinoloverats.messaging.demo.MessagingFramework;
import net.albinoloverats.messaging.demo.mapper.EventMapper;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import net.albinoloverats.messaging.demo.messages.ExampleQuery;
import net.albinoloverats.messaging.demo.messages.ExampleResponse;
import net.albinoloverats.messaging.demo.store.EventEntity;
import net.albinoloverats.messaging.demo.store.EventRepository;
import net.albinoloverats.messaging.demo.utils.Counters;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class Ama
{
	private final EventRepository eventRepository;
	private final EventMapper eventMapper;
	private final Counters counters;
	private final MessagingGateway messagingGateway;

	public Ama(EventRepository eventRepository,
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
		List<EventEntity> events;
		if (ids.isEmpty())
		{
			events = eventRepository.findAllByFramework(MessagingFramework.AMA);
		}
		else
		{
			events = eventRepository.findAllById(query.ids());
		}
		log.debug("Found {} entities", events.size());
		return new ExampleResponse(events.stream().map(eventMapper::toDto).collect(Collectors.toSet()));
	}
}
