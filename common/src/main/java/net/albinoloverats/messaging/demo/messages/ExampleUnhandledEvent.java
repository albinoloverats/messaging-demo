package net.albinoloverats.messaging.demo.messages;

import net.albinoloverats.messaging.common.annotations.Event;

@Event
public record ExampleUnhandledEvent(String data)
{
}
