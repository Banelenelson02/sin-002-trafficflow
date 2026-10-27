package co.wethinkcode.trafficflow;

public record WatchdogStatus(int httpStatus, String status, String message) {

    public static WatchdogStatus from(HeartbeatListener heartbeatListener) {
        if (heartbeatListener.hasDeadLetteredHeartbeat()) {
            return new WatchdogStatus(
                    503,
                    "DOWN",
                    "A heartbeat was dead-lettered"
            );
        }

        if (heartbeatListener.isHeartbeatMissing()) {
            return new WatchdogStatus(
                    503,
                    "DOWN",
                    "No recent heartbeat from Intersection Service"
            );
        }

        return new WatchdogStatus(
                200,
                "UP",
                "Intersection Service heartbeat is being received"
        );
    }
}
