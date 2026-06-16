package net.albinoloverats.messaging.demo.consumer;

import com.jcabi.aspects.Loggable;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.albinoloverats.messaging.client.MessagingGateway;
import net.albinoloverats.messaging.demo.MessagingFramework;
import net.albinoloverats.messaging.demo.mapper.EventMapper;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import net.albinoloverats.messaging.demo.messages.ExampleQuery;
import net.albinoloverats.messaging.demo.messages.ExampleResponse;
import net.albinoloverats.messaging.demo.store.EventEntity;
import net.albinoloverats.messaging.demo.store.EventRepository;
import net.albinoloverats.messaging.demo.utils.Counters;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.unitofwork.ProcessingContext;
import org.axonframework.queryhandling.annotation.QueryHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

//@ProcessingGroup("demoProcessor")
@Component
@Profile("axon")
@Slf4j
public class Axon
{
	private final EventRepository eventRepository;
	private final EventMapper eventMapper;
	private final Counters counters;
	private final EventGateway eventGateway;
	private final ProcessingContext processingContext;

	public Axon(EventRepository eventRepository,
	            EventMapper eventMapper,
	            MessagingGateway messagingGateway,
	            MeterRegistry meterRegistry,
	            EventGateway eventGateway,
	            ProcessingContext processingContext)
	{
		this.eventRepository = eventRepository;
		this.eventMapper = eventMapper;
		counters = new Counters(messagingGateway.getClientId(),
				MessagingFramework.AXON,
				meterRegistry);
		this.eventGateway = eventGateway;
		this.processingContext = processingContext;
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
			eventGateway.publish(processingContext, List.of(updated));
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
		List<EventEntity> events;
		if (ids.isEmpty())
		{
			events = eventRepository.findAllByFramework(MessagingFramework.AXON);
		}
		else
		{
			events = eventRepository.findAllById(query.ids());
		}
		log.debug("Found {} entities", events.size());
		return new ExampleResponse(events.stream().map(eventMapper::toDto).collect(Collectors.toSet()));
	}
}
