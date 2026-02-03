package net.albinoloverats.messaging.demo.messages;

import net.albinoloverats.messaging.common.annotations.Query;

@Query
public record ExampleUnhandledQuery(String data)
{
}
