package co.wethinkcode.trafficflow;

public interface CongestionMessagePublisher {
    void publish(int level) throws Exception;
}
