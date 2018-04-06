package artemis.messenger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
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
		Method[] methods = object.getClass().getDeclaredMethods();
		List<ParseMethod> list = new ArrayList<ParseMethod>();
		
		for (Method method: methods) {
			if (!method.getReturnType().isAssignableFrom(boolean.class)) continue;
			
			Class<?>[] paramTypes = method.getParameterTypes();
			if (paramTypes.length != PARAM_TYPES.length) continue;
			if (!PARAM_TYPES[0].isAssignableFrom(paramTypes[0])) continue;
			if (!PARAM_TYPES[1].isAssignableFrom(paramTypes[1])) continue;
			
			Message parse = method.getAnnotation(Message.class);
			if (parse != null && protocols.contains(parse.value()))
				list.add(new ParseMethod(object, method));
		}
		
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