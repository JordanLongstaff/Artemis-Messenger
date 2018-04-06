package artemis.messenger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for methods that are used to parse Comms messages. Labels must be provided
 * for the parsing protocols employed by those methods by passing those labels in as the only
 * argument to this annotation.
 * @author Jordan Longstaff
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Message {
	ParseProtocol value();
}