package io.github.the28awg.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Bus {

    private static final Logger logger = LoggerFactory.getLogger(Bus.class.getName());
    private static final AtomicInteger BUS_NUMBER = new AtomicInteger();
    private Map<Object, Map<String, Method>> subscribers;
    private String identifier;

    public Bus() {
        this.subscribers = new ConcurrentHashMap<>();
        this.identifier = String.format(Locale.ROOT, "Bus(#%05d)", BUS_NUMBER.getAndIncrement());
    }

    public Bus(String identifier) {
        this.subscribers = new ConcurrentHashMap<>();
        this.identifier = identifier;
    }

    private static String argumentTypesToString(Class<?>[] argTypes) {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        if (argTypes != null) {
            for (int i = 0; i < argTypes.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                Class<?> c = argTypes[i];
                if (c != null && !c.isAnnotationPresent(Event.class)) {
                    throw new RuntimeException(c.getName() + " is not an @Event");
                }
                buf.append((c == null) ? "null" : c.getName());
            }
        }
        buf.append(")");
        return buf.toString();
    }

    private static String argumentObjectToString(Object... o) {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        if (o == null) {
            buf.append("null");
        } else {
            for (int i = 0; i < o.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                if (o[i] == null) {
                    buf.append("null");
                } else {
                    Class<?> c = o[i].getClass();
                    if (c.isAnnotationPresent(Event.class)) {
                        Event event = c.getAnnotation(Event.class);
                        String identifier = event.value();
                        if (identifier.isEmpty()) {
                            identifier = c.getName();
                        }
                        buf.append(identifier);
                    } else {
                        if (c.equals(String.class)) {
                            buf.append(o[i].toString());
                        } else {
                            buf.append(c.getName());
                        }
                    }
                }
            }

        }
        buf.append(")");
        return buf.toString();
    }

    public String identifier() {
        return identifier;
    }

    public void enable(Object o) {
        boolean found = false;

        for (Map.Entry<Object, Map<String, Method>> entry : subscribers.entrySet()) {
            Object object = entry.getKey();
            if (object == null) {
                subscribers.remove(entry.getKey());
            }
            if (o == object) {
                found = true;
            }
        }
        if (!found) {
            Map<String, Method> tmp = new ConcurrentHashMap<>();
            Class clazz = o.getClass();
            while (clazz != null) {
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(Subscribe.class)) {
                        Subscribe subscribe = method.getAnnotation(Subscribe.class);
                        String identifier = subscribe.value();
                        if (identifier.isEmpty()) {
                            identifier = argumentTypesToString(method.getParameterTypes());
                        }
                        logger.debug("{} enable {} in {}#{}", this.identifier, identifier, clazz.getSimpleName(), method.getName());
                        tmp.put(identifier, method);

                    }
                }
                clazz = clazz.getSuperclass();
            }
            subscribers.put(o, tmp);
        }
    }

    public void disable(Object o) {
        for (Map.Entry<Object, Map<String, Method>> entry : subscribers.entrySet()) {
            Object object = entry.getKey();
            if (object == null || o == object) {
                logger.debug("{} disable {}", this.identifier, o.getClass().getSimpleName());
                subscribers.remove(entry.getKey());
            }
        }
    }

    public void post(Object... args) {
        if (args != null) {
            if (args[0] instanceof String){
                String identifier = (String) args[0];
                post(identifier, Arrays.copyOfRange(args, 1, args.length));
            } else {
                post(argumentObjectToString(args), args);
            }
        }
    }

    private void post(String identifier, Object... args) {
        for (Map.Entry<Object, Map<String, Method>> entry : subscribers.entrySet()) {
            Object object = entry.getKey();
            if (object == null) {
                subscribers.remove(entry.getKey());
            } else {
                for (Map.Entry<String, Method> tmp : entry.getValue().entrySet()) {
                    if (tmp.getKey().equals(identifier)) {
                        Method method = tmp.getValue();
                        method.setAccessible(true);
                        try {
                            logger.debug("{} post {} on {} from {}#{}", this.identifier, identifier, Arrays.toString(args), object.getClass().getSimpleName(), method.getName());
                            method.invoke(object, args);
                        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                            throw new RuntimeException("identifier: " + identifier + ", post: " + Arrays.toString(args) + ", object: " + object + ", method: " + method + ", key: " + tmp.getKey(), e);
                        }
                    }
                }
            }
        }
    }
}
