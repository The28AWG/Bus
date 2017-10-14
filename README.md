# Bus
Usage:
```java
public class BusTest {

    private static final Logger logger = LoggerFactory.getLogger(Bus.class.getName());

    @Before
    public void setLogger() throws IOException {
        LogManager.getLogManager().readConfiguration(BusTest.class.getResourceAsStream("/logging.properties"));
    }

    @Test
    public void test_bus() {
        // initialize
        Bus bus = new Bus();
        //subscribe
        bus.enable(this);
        PrivateStaticEvent event = new PrivateStaticEvent();
        //post events
        bus.post(event);
        bus.post("test");
        bus.post("test_2", "beep");
        // unsubscribe
        bus.disable(this);
        bus.post("test");
    }

    // mark @Subscribe receiver method
    @Subscribe
    public void event_receiver(PrivateStaticEvent event) {
        logger.debug("event!");
    }

    // custom receiver name
    @Subscribe("test")
    public void random_name() {
        logger.debug("event!");
    }

    // and send value
    @Subscribe("test_2")
    public void random_name_2(String test) {
        logger.debug("event! value = {}", test);
    }

    @Event
    private static class PrivateStaticEvent {

    }
}
```