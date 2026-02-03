package net.albinoloverats.messaging.demo.messages;

import net.albinoloverats.messaging.common.annotations.Response;
import net.albinoloverats.messaging.demo.store.Event;

import java.util.Set;

@Response
public record ExampleResponse(Set<Event> events)
{
}
