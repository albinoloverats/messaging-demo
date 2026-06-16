package net.albinoloverats.messaging.demo.producer;

import lombok.val;
import net.albinoloverats.messaging.demo.dto.Event;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import net.albinoloverats.messaging.demo.messages.ExampleQuery;
import net.albinoloverats.messaging.demo.messages.ExampleResponse;
import net.albinoloverats.messaging.test.matchers.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;
import java.util.UUID;

import static net.albinoloverats.messaging.test.TestMessagingHarness.verifyEvent;
import static net.albinoloverats.messaging.test.TestMessagingHarness.verifyQueries;
import static net.albinoloverats.messaging.test.TestMessagingHarness.withQueryHandler;
import static net.albinoloverats.messaging.test.matchers.Matchers.exactly;
import static net.albinoloverats.messaging.test.matchers.Matchers.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SpringBootTest
public class AmaTest
{
	@Autowired
	private Ama service;

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
}
