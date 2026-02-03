package net.albinoloverats.messaging.demo.messages;

import net.albinoloverats.messaging.common.annotations.Query;

import java.util.Set;
import java.util.UUID;

@Query
public record ExampleQuery(Set<UUID> ids, boolean isExceptional)
{
	public ExampleQuery(Set<UUID> ids)
	{
		this(ids, false);
	}

	public static ExampleQuery exceptional()
	{
		return new ExampleQuery(Set.of(), true);
	}
}
