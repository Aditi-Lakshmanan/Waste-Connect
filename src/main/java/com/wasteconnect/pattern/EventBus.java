package com.wasteconnect.pattern;

import com.wasteconnect.model.SegregationQuality;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class EventBus {
    public interface DomainEvent {}

    public record HouseholdCollected(String householdId, String collectorId, String routeId,
                                     int stopSequence, double weightKg, SegregationQuality quality) implements DomainEvent {}
    public record ComplaintRaised(String complaintId, String citizenId, String zoneId) implements DomainEvent {}
    public record ComplaintAssigned(String complaintId, String collectorId) implements DomainEvent {}
    public record ComplaintResolved(String complaintId, String citizenId) implements DomainEvent {}
    public record ComplaintEscalated(String complaintId, String zoneId) implements DomainEvent {}
    public record PointsAwarded(String citizenId, int pointsDelta, int balance) implements DomainEvent {}
    public record BinOverflow(String binId, String zoneId, double fillPercentage) implements DomainEvent {}
    public record BinFillChanged(String binId, String zoneId, double fillPercentage, String status) implements DomainEvent {}
    public record RouteGenerated(String routeId, String zoneId, String collectorId) implements DomainEvent {}
    public record RouteReplanned(String routeId, int remainingStops) implements DomainEvent {}
    public record PickupRequested(String pickupId, String citizenId, LocalDate preferredDate) implements DomainEvent {}
    public record ListingCreated(String listingId, String citizenId) implements DomainEvent {}
    public record ListingAccepted(String listingId, String citizenId, String pickerId) implements DomainEvent {}
    public record ListingCompleted(String listingId, String citizenId, String pickerId, double actualWeightKg) implements DomainEvent {}
    public record PickerVerified(String pickerId, boolean approved) implements DomainEvent {}
    public record SettingsChanged(String settingKey, String value) implements DomainEvent {}
    public record NotificationCreated(String notificationId, String userId) implements DomainEvent {}

    private static final EventBus INSTANCE = new EventBus();
    private final ConcurrentHashMap<Class<? extends DomainEvent>, CopyOnWriteArrayList<Consumer<DomainEvent>>> subscribers = new ConcurrentHashMap<>();

    private EventBus() {}

    public static EventBus getInstance() {
        return INSTANCE;
    }

    public <E extends DomainEvent> AutoCloseable subscribe(Class<E> eventType, Consumer<E> listener) {
        Consumer<DomainEvent> adapter = event -> listener.accept(eventType.cast(event));
        subscribers.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(adapter);
        return () -> {
            CopyOnWriteArrayList<Consumer<DomainEvent>> listeners = subscribers.get(eventType);
            if (listeners != null) {
                listeners.remove(adapter);
                if (listeners.isEmpty()) subscribers.remove(eventType, listeners);
            }
        };
    }

    public void publish(DomainEvent event) {
        CopyOnWriteArrayList<Consumer<DomainEvent>> listeners = subscribers.get(event.getClass());
        if (listeners != null) {
            for (Consumer<DomainEvent> listener : listeners) {
                listener.accept(event);
            }
        }
    }
}
