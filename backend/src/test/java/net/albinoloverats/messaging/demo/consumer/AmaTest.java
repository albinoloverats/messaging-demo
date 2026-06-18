package net.albinoloverats.messaging.demo.consumer;

import lombok.val;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import net.albinoloverats.messaging.demo.messages.ExampleQuery;
import net.albinoloverats.messaging.demo.messages.ExampleResponse;
import net.albinoloverats.messaging.demo.store.EventEntity;
import net.albinoloverats.messaging.demo.store.EventRepository;
import net.albinoloverats.messaging.test.TestMessagingHarness;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static net.albinoloverats.messaging.test.TestMessagingHarness.publishEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class AmaTest
{
	@MockitoBean
	private EventRepository eventRepository;
	@Autowired
	private Ama service;

	@Test
	void testEventHandler()
	{
		publishEvent(new ExampleEvent("some text"));

		verify(eventRepository).save(any(EventEntity.class));
	}

	@Test
	void testQueryHandler()
	{
		val id = UUID.randomUUID();
		val event = mock(EventEntity.class);
		when(event.getId()).thenReturn(id);
		when(eventRepository.findAllById(any())).thenReturn(List.of(event));

		TestMessagingHarness.<ExampleResponse>raiseQuery(new ExampleQuery(Set.of(id)))
				.thenAccept(response ->
				{
					assertNotNull(response);
					assertEquals(1, response.events().size());
					val e = response.events()
							.stream()
							.findFirst()
							.orElseThrow();
					assertEquals(id, e.id());
				});
	}
}
