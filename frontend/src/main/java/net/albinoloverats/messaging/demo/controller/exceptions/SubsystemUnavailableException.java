package net.albinoloverats.messaging.demo.controller.exceptions;

public class SubsystemUnavailableException extends RuntimeException
{
	public SubsystemUnavailableException(String subsystem)
	{
		super(subsystem + " is not available");
	}
}
