
package co.wethinkcode.trafficflow;

import java.util.concurrent.atomic.AtomicInteger;

public class CongestionController {
    private final AtomicInteger currentLevel = new AtomicInteger(0);
    private final CongestionMessagePublisher publisher;

    public CongestionController(CongestionMessagePublisher publisher) {
        this.publisher = publisher;
    }

    public int getLevel() {
        return currentLevel.get();
    }

    public synchronized int updateLevel(int level) throws Exception {
        if (level < 0 || level > 8) {
            throw new IllegalArgumentException(
                    "Congestion level must be between 0 and 8"
            );
        }

        int previousLevel = currentLevel.get();

        if (previousLevel != level) {
            publisher.publish(level);
            currentLevel.set(level);
        }

        return currentLevel.get();
    }
}
