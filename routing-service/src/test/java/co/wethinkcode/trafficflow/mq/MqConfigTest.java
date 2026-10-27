package co.wethinkcode.trafficflow.mq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MqConfigTest {

    @Test
    void exposesTheCongestionMessagingContract() {
        assertEquals("tcp://localhost:61616", MqConfig.BROKER_URL);
        assertEquals("congestion-topic", MqConfig.TOPIC);
    }
}
