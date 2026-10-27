package co.wethinkcode.trafficflow;

public record Congestion(int level) {

    public Congestion {
        if (level < 0 || level > 8) {
            throw new IllegalArgumentException(
                    "Congestion level must be between 0 and 8"
            );
        }
    }
}
