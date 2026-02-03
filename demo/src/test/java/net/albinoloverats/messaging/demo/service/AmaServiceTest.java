package net.albinoloverats.messaging.demo.service;

import lombok.val;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import net.albinoloverats.messaging.demo.messages.ExampleQuery;
import net.albinoloverats.messaging.demo.messages.ExampleResponse;
import net.albinoloverats.messaging.demo.store.Event;
import net.albinoloverats.messaging.demo.store.EventRepository;
import net.albinoloverats.messaging.test.TestMessagingHarness;
import net.albinoloverats.messaging.test.annotations.MessagingDispatch;
import net.albinoloverats.messaging.test.matchers.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static net.albinoloverats.messaging.test.TestMessagingHarness.publishEvent;
import static net.albinoloverats.messaging.test.TestMessagingHarness.verifyEvent;
import static net.albinoloverats.messaging.test.TestMessagingHarness.verifyQueries;
import static net.albinoloverats.messaging.test.TestMessagingHarness.verifyQuery;
import static net.albinoloverats.messaging.test.TestMessagingHarness.withQueryHandler;
import static net.albinoloverats.messaging.test.matchers.Matchers.exactly;
import static net.albinoloverats.messaging.test.matchers.Matchers.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class AmaServiceTest
{
	@MockitoBean
	private EventRepository eventRepository;
	@Autowired
	private AmaService service;

	@Test
	void testEventProducer()
	{
		val ids = service.publish(1, 1, "some text");
		assertEquals(1, ids.size());
		val id = ids.stream()
				.findFirst()
				.orElseThrow();
		verifyEvent(Matchers.<ExampleEvent>matching(event ->
				event.eventId().equals(id) && event.data().equals("some text")));
	}

	@Test
	void testQueryProducer()
	{
		val id = UUID.randomUUID();
		val expected = new ExampleQuery(Set.of(id));

		val event = mock(Event.class);

		withQueryHandler(exactly(expected), query ->
				new ExampleResponse(Set.of(event)));

		withQueryHandler(of(ExampleQuery.class), ignored -> new ExampleResponse(Set.of()));

		service.query(Set.of(id)).thenAccept(response ->
		{
			assertNotNull(response);
			assertFalse(response.isEmpty());
			assertTrue(response.contains(event));
		});
		service.query(Set.of(UUID.randomUUID())).thenAccept(response ->
		{
			assertNotNull(response);
			assertTrue(response.isEmpty());
		});

		verifyQueries(exactly(expected), of(ExampleQuery.class));
	}

	@Test
	void testEventHandler()
	{
		publishEvent(new ExampleEvent("some text"));

		verify(eventRepository).save(any(Event.class));
	}

	@Test
	void testQueryHandler()
	{
		val id = UUID.randomUUID();
		val event = mock(Event.class);
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
					assertEquals(event, e);
				});
	}

	@Test
	@MessagingDispatch
	void testEventEnd2End()
	{
		val ids = service.publish(1, 1, "some text");
		assertEquals(1, ids.size());
		val id = ids.stream()
				.findFirst()
				.orElseThrow();

		verifyEvent(Matchers.<ExampleEvent>matching(event ->
				event.eventId().equals(id) && event.data().equals("some text")));

		verify(eventRepository).save(any(Event.class));
	}

	@Test
	@MessagingDispatch
	void testQueryEnd2End()
	{
		val id = UUID.randomUUID();
		val event = mock(Event.class);
		when(event.getId()).thenReturn(id);
		when(eventRepository.findAllById(Set.of(id))).thenReturn(List.of(event));

		service.query(Set.of(id)).thenAccept(response ->
		{
			assertNotNull(response);
			assertFalse(response.isEmpty());
			assertTrue(response.contains(event));
		});
		val expected = new ExampleQuery(Set.of(id));

		verifyQuery(expected);
	}
}
