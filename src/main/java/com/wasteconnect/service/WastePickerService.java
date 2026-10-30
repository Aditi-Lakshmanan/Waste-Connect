package com.wasteconnect.service;

import com.wasteconnect.dao.RecyclableListingDao;
import com.wasteconnect.dao.RecyclableListingDaoImpl;
import com.wasteconnect.dao.CitizenDao;
import com.wasteconnect.dao.CitizenDaoImpl;
import com.wasteconnect.dao.RewardTransactionDao;
import com.wasteconnect.dao.RewardTransactionDaoImpl;
import com.wasteconnect.dao.DatabaseConnection;
import com.wasteconnect.dao.WastePickerDao;
import com.wasteconnect.dao.WastePickerDaoImpl;
import com.wasteconnect.exception.ValidationException;
import com.wasteconnect.model.ListingStatus;
import com.wasteconnect.model.Location;
import com.wasteconnect.model.RecyclableListing;
import com.wasteconnect.model.WastePicker;
import com.wasteconnect.pattern.EventBus;
import com.wasteconnect.model.RewardTransaction;
import com.wasteconnect.util.AppLogger;
import com.wasteconnect.util.Validators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Service managing Waste Picker informal recycling partner workflows.
 */
public class WastePickerService {
    private final RecyclableListingDao listingDao;
    private final WastePickerDao pickerDao;
    private final NotificationService notificationService;
    private final CitizenDao citizenDao;
    private final RewardTransactionDao rewardTransactionDao;

    public WastePickerService() {
        this(new RecyclableListingDaoImpl(), new WastePickerDaoImpl(), new CitizenDaoImpl(),
                new RewardTransactionDaoImpl(), new NotificationService());
    }

    public WastePickerService(RecyclableListingDao listingDao, WastePickerDao pickerDao, NotificationService notificationService) {
        this(listingDao, pickerDao, new CitizenDaoImpl(), new RewardTransactionDaoImpl(), notificationService);
    }

    public WastePickerService(RecyclableListingDao listingDao, WastePickerDao pickerDao,
                              CitizenDao citizenDao, RewardTransactionDao rewardTransactionDao,
                              NotificationService notificationService) {
        this.listingDao = listingDao;
        this.pickerDao = pickerDao;
        this.citizenDao = citizenDao;
        this.rewardTransactionDao = rewardTransactionDao;
        this.notificationService = notificationService;
    }

    public Optional<WastePicker> getPicker(String pickerId) {
        return pickerDao.findById(pickerId);
    }

    /**
     * Finds all available recyclable listings sorted by distance from the picker's current location.
     */
    public List<RecyclableListing> getNearbyAvailableListings(Location pickerLocation) {
        List<RecyclableListing> available = listingDao.findByStatus(ListingStatus.AVAILABLE);
        if (pickerLocation == null) {
            return available;
        }
        List<RecyclableListing> sorted = new ArrayList<>(available);
        sorted.sort(Comparator.comparingDouble(l -> pickerLocation.distanceTo(l.getLocation())));
        return sorted;
    }

    public List<RecyclableListing> getPickerActiveListings(String pickerId) {
        return listingDao.findByPicker(pickerId);
    }

    /**
     * Claims a listed recyclable commodity batch.
     */
    public boolean claimListing(String listingId, String pickerId) {
        WastePicker picker = pickerDao.findById(pickerId)
                .orElseThrow(() -> new ValidationException("Waste Picker not found: " + pickerId));

        if (!picker.isVerified()) {
            throw new ValidationException("Your account is pending municipal verification. You cannot claim listings yet.");
        }

        boolean success = listingDao.claimListing(listingId, pickerId);
        if (success) {
            listingDao.findById(listingId).ifPresent(listing -> EventBus.getInstance().publish(
                    new EventBus.ListingAccepted(listingId, listing.getCitizenId(), pickerId)));
            listingDao.findById(listingId).ifPresent(l -> {
                notificationService.sendNotification(l.getCitizenId(), "Recyclables Claimed",
                        "Waste picker " + picker.getName() + " has accepted your recyclable pickup request.");
            });
            AppLogger.info("Waste picker " + pickerId + " claimed listing " + listingId);
        }
        return success;
    }

    /**
     * Completes collection with actual measured weight, updating earnings.
     */
    public boolean completeCollection(String listingId, double actualWeightKg, String pickerId) {
        Validators.validatePositive(actualWeightKg, "Actual Weight");

        RecyclableListing listing = listingDao.findById(listingId)
                .orElseThrow(() -> new ValidationException("Listing not found: " + listingId));

        if (listing.getStatus() != ListingStatus.CLAIMED || !pickerId.equals(listing.getClaimedPickerId())) {
            throw new ValidationException("This listing is not claimed by your account.");
        }

        double ratePerKg = (listing.getWasteType() == com.wasteconnect.model.WasteType.E_WASTE) ? 30.0 : 12.0;
        double earnings = Math.round(actualWeightKg * ratePerKg * 100.0) / 100.0;

        int pointsAwarded = (int) Math.floor(actualWeightKg * listing.getWasteType().getBasePointsPerKg());
        Connection connection = DatabaseConnection.getInstance().getConnection();
        try {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (!listingDao.completeCollection(listingId, pickerId, actualWeightKg, earnings)) {
                    throw new ValidationException("This listing is no longer claimed by your account.");
                }
                if (!pickerDao.addEarnings(pickerId, earnings, actualWeightKg)) {
                    throw new ValidationException("Picker earnings could not be updated.");
                }
                int newCitizenBalance = -1;
                if (pointsAwarded > 0) {
                    var citizen = citizenDao.findById(listing.getCitizenId()).orElse(null);
                    if (citizen != null) {
                        newCitizenBalance = citizen.getPointsBalance() + pointsAwarded;
                        if (!citizenDao.updatePointsOnly(citizen.getId(), newCitizenBalance)) {
                            throw new ValidationException("Household points could not be updated.");
                        }
                        RewardTransaction transaction = new RewardTransaction(
                                "TXN-" + UUID.randomUUID().toString().substring(0, 8), citizen.getId(),
                                RewardTransaction.Type.EARNED, pointsAwarded,
                                "Recyclable pickup completed: " + listingId);
                        rewardTransactionDao.save(transaction);
                    }
                }
                connection.commit();
                connection.setAutoCommit(autoCommit);

                if (newCitizenBalance >= 0) {
                    EventBus.getInstance().publish(new EventBus.PointsAwarded(
                            listing.getCitizenId(), pointsAwarded, newCitizenBalance));
                }
                EventBus.getInstance().publish(new EventBus.ListingCompleted(
                        listingId, listing.getCitizenId(), pickerId, actualWeightKg));
                notificationService.sendNotification(listing.getCitizenId(), "Recyclable Pickup Completed",
                        String.format("Eco collector collected %.1f kg of %s. You earned %d points.",
                                actualWeightKg, listing.getWasteType().getDisplayName(), pointsAwarded));
                AppLogger.info("Waste picker " + pickerId + " completed collection for listing " + listingId + " (₹" + earnings + ")");
                return true;
            } catch (Exception exception) {
                connection.rollback();
                connection.setAutoCommit(autoCommit);
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new RuntimeException("Failed to complete recyclable pickup.", exception);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Database error while completing recyclable pickup.", exception);
        }
    }
}
