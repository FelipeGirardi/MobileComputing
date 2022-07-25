package mobile.tracker.service;

public interface ITrackingServiceCallback {

    void onServiceStatusChange(TrackingServiceStatus status);

    void onNewLocationCount(int count);
}
