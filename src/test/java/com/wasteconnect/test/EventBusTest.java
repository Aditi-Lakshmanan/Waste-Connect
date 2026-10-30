package com.wasteconnect.test;

import com.wasteconnect.model.SegregationQuality;
import com.wasteconnect.pattern.EventBus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBusTest {
    @Test
    void deliversTypedEventsAndStopsAfterUnsubscribe() throws Exception {
        EventBus bus = EventBus.getInstance();
        List<EventBus.PickupRequested> received = new ArrayList<>();
        AutoCloseable subscription = bus.subscribe(EventBus.PickupRequested.class, received::add);
        EventBus.PickupRequested requested = new EventBus.PickupRequested("PCK-1", "CIT-1", LocalDate.now());

        bus.publish(requested);
        subscription.close();
        bus.publish(new EventBus.PickupRequested("PCK-2", "CIT-1", LocalDate.now()));

        assertEquals(List.of(requested), received);
    }

    @Test
    void collectionEventsCarryTheActualQuality() {
        EventBus.HouseholdCollected event = new EventBus.HouseholdCollected(
                "CIT-1", "COL-1", "RT-1", 3, 12.0, SegregationQuality.GOOD);

        assertEquals("CIT-1", event.householdId());
        assertEquals(12.0, event.weightKg());
        assertEquals(SegregationQuality.GOOD, event.quality());
    }
}
