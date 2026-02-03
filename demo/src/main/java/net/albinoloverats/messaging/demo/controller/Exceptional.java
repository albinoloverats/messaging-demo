package net.albinoloverats.messaging.demo.controller;

import com.jcabi.aspects.Loggable;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.albinoloverats.messaging.demo.service.AmaService;
import net.albinoloverats.messaging.demo.store.Event;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/demo/exceptional")
@RequiredArgsConstructor
public class Exceptional
{
	private final AmaService amaService;

	/*
	 * Events and queries that are either not handled or complete exceptionally.
	 */

	@PostMapping("/unhandled")
	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<ResponseEntity<String>> unhandledEvent(@RequestBody String body)
	{
		return amaService.publish(body)
				.thenApply(v -> ResponseEntity.ok(body))
				.exceptionally(this::internalServerError);
	}

	@GetMapping("/unhandled")
	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<ResponseEntity<Set<Event>>> unhandledQuery()
	{
		return amaService.query()
				.thenApply(ResponseEntity::ok)
				.exceptionally(this::internalServerError);
	}

	@GetMapping
	@Loggable(value = Loggable.TRACE, prepend = true)
	public CompletableFuture<ResponseEntity<Set<Event>>> exceptionalQuery()
	{
		return amaService.exceptionalQuery()
				.thenApply(ResponseEntity::ok)
				.exceptionally(this::internalServerError);
	}

	private <R> ResponseEntity<R> internalServerError(Throwable throwable)
	{
		val body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, throwable.getMessage());
		return ResponseEntity.of(body)
				.build();
	}
}
