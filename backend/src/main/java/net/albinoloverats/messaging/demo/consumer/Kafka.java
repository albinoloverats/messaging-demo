package net.albinoloverats.messaging.demo.consumer;

import com.jcabi.aspects.Loggable;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.albinoloverats.messaging.client.MessagingGateway;
import net.albinoloverats.messaging.demo.MessagingFramework;
import net.albinoloverats.messaging.demo.mapper.EventMapper;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import net.albinoloverats.messaging.demo.store.EventRepository;
import net.albinoloverats.messaging.demo.utils.Counters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Profile("kafka")
@Slf4j
public class Kafka
{
	private final EventRepository eventRepository;
	private final EventMapper eventMapper;
	private final Counters counters;
	private final KafkaTemplate<String, ExampleEvent> kafkaTemplate;

	@Value("${kafka-topic-name}")
	private String topicName;

	public Kafka(EventRepository eventRepository,
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
