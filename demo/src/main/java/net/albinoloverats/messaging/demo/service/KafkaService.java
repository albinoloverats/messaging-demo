package net.albinoloverats.messaging.demo.service;

import com.jcabi.aspects.Loggable;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.albinoloverats.messaging.client.MessagingGateway;
import net.albinoloverats.messaging.demo.MessagingFramework;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import net.albinoloverats.messaging.demo.store.EventMapper;
import net.albinoloverats.messaging.demo.store.EventRepository;
import net.albinoloverats.messaging.demo.utils.Counters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
@Profile("kafka")
@Slf4j
public class KafkaService
{
	private final EventRepository eventRepository;
	private final EventMapper eventMapper;
	private final Counters counters;
	private final KafkaTemplate<String, ExampleEvent> kafkaTemplate;

	@Value("${kafka-topic-name}")
	private String topicName;

	public KafkaService(EventRepository eventRepository,
	                    EventMapper eventMapper,
	                    MessagingGateway messagingGateway,
	                    MeterRegistry meterRegistry,
	                    KafkaTemplate<String, ExampleEvent> kafkaTemplate)
	{
		this.eventRepository = eventRepository;
		this.eventMapper = eventMapper;
		counters = new Counters(messagingGateway.getClientId(),
				MessagingFramework.KAFKA,
				meterRegistry);
		this.kafkaTemplate = kafkaTemplate;
	}

	/*
	 * Event Publishing
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
					kafkaTemplate.send(topicName, event);
					counters.eventSent(event);
					log.debug("Published Kafka event {}", event);
					ids.add(event.eventId());
				});
		return ids;
	}

	/*
	 * Event Handling
	 */

	@KafkaListener(
			topics = "${kafka-topic-name}",
			groupId = "${spring.kafka.consumer.group-id}",
			containerFactory = "exampleEventKafkaListenerContainerFactory"
	)
	@Loggable(value = Loggable.TRACE, prepend = true)
	public void handle(@Payload ExampleEvent event)
	{
		counters.eventReceived(event);
		val remaining = event.remaining() - 1;
		if (remaining <= 0)
		{
			var entity = eventMapper.toEntity(event, MessagingFramework.KAFKA);
			entity = eventRepository.save(entity);
			log.debug("Kafka event persisted {}", entity);
		}
		else
		{
			val updated = event.withRemaining(remaining);
			kafkaTemplate.send(topicName, updated);
			counters.eventSent(updated);
			log.debug("Forwarded Kafka event {}", updated);
		}
	}
}
