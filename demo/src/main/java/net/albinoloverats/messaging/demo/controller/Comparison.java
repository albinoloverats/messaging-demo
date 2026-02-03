package net.albinoloverats.messaging.demo.controller;

import com.jcabi.aspects.Loggable;
import lombok.val;
import net.albinoloverats.messaging.demo.MessagingFramework;
import net.albinoloverats.messaging.demo.controller.exceptions.SubsystemUnavailableException;
import net.albinoloverats.messaging.demo.service.AmaService;
import net.albinoloverats.messaging.demo.service.AxonService;
import net.albinoloverats.messaging.demo.service.KafkaService;
import net.albinoloverats.messaging.demo.store.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@RestController
@RequestMapping("/demo")
public class Comparison
{
	private final ExecutorService eventProcessor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	@Autowired
	private AmaService amaService;
	@Autowired(required = false)
	private AxonService axonService;
	@Autowired(required = false)
	private KafkaService kafkaService;

	@PostMapping
	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<ResponseEntity<Set<UUID>>> event(@RequestParam Optional<MessagingFramework> framework,
	                                                          @RequestParam Optional<Integer> quantity,
	                                                          @RequestParam Optional<Integer> hops,
	                                                          @RequestBody String body)
	{
		val q = quantity.orElse(1);
		val h = hops.orElse(1);
		return CompletableFuture.supplyAsync(() ->
						switch (framework.orElse(MessagingFramework.AMA))
						{
							case AMA -> amaService.publish(q, h, body);
							case AXON ->
									ifComponentEnabled(axonService, MessagingFramework.AXON, () -> axonService.publish(q, h, body));
							case KAFKA ->
									ifComponentEnabled(kafkaService, MessagingFramework.KAFKA, () -> kafkaService.publish(q, h, body));
						}, eventProcessor)
				.thenApply(ResponseEntity::ok);
	}


	@GetMapping
	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<ResponseEntity<Set<Event>>> query(@RequestParam(required = false) Optional<MessagingFramework> framework,
	                                                           @RequestBody(required = false) Optional<List<UUID>> ids)
	{
		val set = new HashSet<>(ids.orElse(List.of()));
		return switch (framework.orElse(MessagingFramework.AMA))
		{
			case AMA -> amaService.query(set).thenApply(ResponseEntity::ok);
			case AXON ->
					ifComponentEnabled(axonService, MessagingFramework.AXON, () -> axonService.query(set).thenApply(ResponseEntity::ok));
			case KAFKA -> throw new UnsupportedOperationException("KAFKA does not support queries");
		};
	}

	@GetMapping("/{id}")
	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<ResponseEntity<Event>> query(@RequestParam(required = false) Optional<MessagingFramework> framework,
	                                                      @PathVariable("id") UUID id)
	{
		return (switch (framework.orElse(MessagingFramework.AMA))
		{
			case AMA -> amaService.query(id);
			case AXON -> ifComponentEnabled(axonService, MessagingFramework.AXON, () -> axonService.query(id));
			case KAFKA -> throw new UnsupportedOperationException("KAFKA does not support queries");
		}).thenApply(e ->
				e.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()));
	}

	private static <R> R ifComponentEnabled(Object object, MessagingFramework framework, Supplier<R> task)
	{
		if (object == null)
		{
			throw new SubsystemUnavailableException(framework.name());
		}
		return task.get();
	}
}
