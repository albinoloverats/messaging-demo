package net.albinoloverats.messaging.demo.store;

import net.albinoloverats.messaging.demo.MessagingFramework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>
{
	List<Event> findAllByFramework(MessagingFramework framework);
}
