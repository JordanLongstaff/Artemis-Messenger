package artemis.messenger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

/**
 * A wrapper class for a method that parses Comms messages.
 * @author Jordan Longstaff
 */
public class ParseMethod {
	private final Method method;
	private final Object receiver;
	
	/**
	 * The parameters types that the parser method must have.
	 */
	private static final Class<?>[] PARAM_TYPES = new Class<?>[] { String.class, String.class };

	/**
	 * Private constructor.
	 */
	private ParseMethod(Object object, Method met) {
		receiver = object;
		method = met;
	}
	
	/**
	 * Generates a list of parser methods found in the object's class. These methods must declare
	 * the {@code Message} annotation, return a {@code boolean} value and accept exactly two
	 * {@code String} arguments. Returns only the methods that have a protocol label in the given
	 * {@code Collection} as the argument to the {@code Message} annotation.
	 */
	public static List<ParseMethod> search(Object object, Collection<ParseProtocol> protocols) {
		// Get all declared methods
		Method[] methods = object.getClass().getDeclaredMethods();
		
		// Set up list of methods sorted by parse protocol
		EnumMap<ParseProtocol, List<ParseMethod>> dictionary = new EnumMap<ParseProtocol, List<ParseMethod>>(ParseProtocol.class);
		int numMethods = 0;
		
		// Search for methods that have a protocol
		for (Method method: methods) {
			// Method must return a boolean
			if (!method.getReturnType().isAssignableFrom(Boolean.TYPE)) continue;
			
			// Method must have the correct parameter types
			Class<?>[] paramTypes = method.getParameterTypes();
			if (paramTypes.length != PARAM_TYPES.length) continue;
			if (!PARAM_TYPES[0].isAssignableFrom(paramTypes[0])) continue;
			if (!PARAM_TYPES[1].isAssignableFrom(paramTypes[1])) continue;
			
			// Method must conform to one of the desired parse protocols
			Message parse = method.getAnnotation(Message.class);
			if (parse == null) continue;
			ParseProtocol protocol = parse.value();
			if (!protocols.contains(protocol)) continue;
			
			// Add parse method
			if (!dictionary.containsKey(protocol)) dictionary.put(protocol, new ArrayList<ParseMethod>());
			dictionary.get(protocol).add(new ParseMethod(object, method));
			numMethods++;
		}
		
		// Flatten the list and return it
		List<ParseMethod> list = new ArrayList<ParseMethod>(numMethods);
		for (ParseProtocol protocol: protocols) list.addAll(dictionary.get(protocol));
		return list;
	}
	
	/**
	 * Invokes the parser method.
	 */
	public boolean parse(String sender, String message) {
		try {
			Object ret = method.invoke(receiver, sender, message);
			return ret != null && Boolean.valueOf(ret.toString());
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}
}