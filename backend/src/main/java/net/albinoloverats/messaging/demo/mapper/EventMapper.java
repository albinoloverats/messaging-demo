package net.albinoloverats.messaging.demo.mapper;

import net.albinoloverats.messaging.demo.MessagingFramework;
import net.albinoloverats.messaging.demo.dto.Event;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import net.albinoloverats.messaging.demo.store.EventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper
{
	@Mapping(target = "id", source = "event.eventId")
	@Mapping(target = "persisted", expression = "java(java.time.Instant.now())")
	EventEntity toEntity(ExampleEvent event, MessagingFramework framework);

	Event toDto(EventEntity entity);
}
