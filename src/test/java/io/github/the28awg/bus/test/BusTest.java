package io.github.the28awg.bus.test;

import io.github.the28awg.bus.Bus;
import io.github.the28awg.bus.Event;
import io.github.the28awg.bus.Subscribe;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.logging.LogManager;

public class BusTest {

    private static final Logger logger = LoggerFactory.getLogger(Bus.class.getName());

    @Before
    public void setLogger() throws IOException {
        LogManager.getLogManager().readConfiguration(BusTest.class.getResourceAsStream("/logging.properties"));
    }

    @Test
    public void test_bus() {
        Bus bus = new Bus();
        bus.enable(this);
        PrivateStaticEvent event = new PrivateStaticEvent();
        bus.post(event);
        bus.post("test");
        bus.post("test_2", "beep");
        bus.disable(this);
        bus.post("test");
    }

    @Subscribe
    public void event_receiver(PrivateStaticEvent event) {
        logger.debug("event!");
    }

    @Subscribe("test")
    public void random_name() {
        logger.debug("event!");
    }

    @Subscribe("test_2")
    public void random_name_2(String test) {
        logger.debug("event! value = {}", test);
    }

    @Event
    private static class PrivateStaticEvent {

    }
}
