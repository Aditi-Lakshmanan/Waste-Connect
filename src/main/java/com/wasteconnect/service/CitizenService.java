package com.wasteconnect.service;

import com.wasteconnect.dao.*;
import com.wasteconnect.exception.ValidationException;
import com.wasteconnect.model.*;
import com.wasteconnect.pattern.EventBus;
import com.wasteconnect.util.AppLogger;
import com.wasteconnect.util.Validators;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service orchestrating Citizen residential operations, transactions, and environmental rewards.
 */
public class CitizenService {
    private final CitizenDao citizenDao;
    private final ComplaintDao complaintDao;
    private final PickupRequestDao pickupRequestDao;
    private final RecyclableListingDao recyclableListingDao;
    private final SegregationLogDao segregationLogDao;
    private final RewardTransactionDao rewardTransactionDao;
    private final NotificationService notificationService;

    public CitizenService() {
        this(new CitizenDaoImpl(), new ComplaintDaoImpl(), new PickupRequestDaoImpl(),
                new RecyclableListingDaoImpl(), new SegregationLogDaoImpl(),
                new RewardTransactionDaoImpl(), new NotificationService());
    }

    public CitizenService(CitizenDao citizenDao, ComplaintDao complaintDao,
                          PickupRequestDao pickupRequestDao, RecyclableListingDao recyclableListingDao,
                          SegregationLogDao segregationLogDao, RewardTransactionDao rewardTransactionDao,
                          NotificationService notificationService) {
        this.citizenDao = citizenDao;
        this.complaintDao = complaintDao;
        this.pickupRequestDao = pickupRequestDao;
        this.recyclableListingDao = recyclableListingDao;
        this.segregationLogDao = segregationLogDao;
        this.rewardTransactionDao = rewardTransactionDao;
        this.notificationService = notificationService;
    }

    public Optional<Citizen> getCitizen(String citizenId) {
        return citizenDao.findById(citizenId);
    }

    /**
     * Raises a municipal waste complaint and logs the opening event.
     */
    public Complaint raiseComplaint(String citizenId, ComplaintType type, String description, double lat, double lng, String zoneId) {
        Validators.validateNonBlank(description, "Complaint Description");
        Validators.validateCoordinates(lat, lng);
        if (type == null) throw new ValidationException("Complaint Type must be selected.");

        String id = "CMP-" + UUID.randomUUID().toString().substring(0, 8);
        Location loc = new Location(lat, lng, "Reported issue site");
        Complaint complaint = new Complaint(id, citizenId, type, description, loc, zoneId != null ? zoneId : "Z1");
        complaintDao.save(complaint);

        // Audit event
        ComplaintEvent event = new ComplaintEvent("EVT-" + UUID.randomUUID().toString().substring(0, 8),
                id, null, ComplaintStatus.OPEN, "Complaint lodged by citizen");
        complaintDao.logEvent(event);
        EventBus.getInstance().publish(new EventBus.ComplaintRaised(id, citizenId, complaint.getZoneId()));

        List<Collector> zoneCollectors = new CollectorDaoImpl().findByZone(complaint.getZoneId());
        if (!zoneCollectors.isEmpty()) {
            String collectorId = zoneCollectors.get(0).getId();
            if (complaintDao.assignCollector(id, collectorId)) {
                complaint.setAssignedCollectorId(collectorId);
                EventBus.getInstance().publish(new EventBus.ComplaintAssigned(id, collectorId));
                notificationService.sendNotification(collectorId, "Complaint Assigned",
                        "Complaint " + id + " was automatically added to your zone queue.");
            }
        }

        notificationService.sendNotification(citizenId, "Complaint Received",
                "Your " + type.getLabel() + " report (#" + id + ") has been queued with a " + type.getSlaHours() + "h resolution SLA.");
        AppLogger.info("Complaint raised by citizen " + citizenId + ": " + id);
        return complaint;
    }

    public List<Complaint> getCitizenComplaints(String citizenId) {
        return complaintDao.findByCitizen(citizenId);
    }

    public List<ComplaintEvent> getComplaintTimeline(String complaintId) {
        return complaintDao.findEventsForComplaint(complaintId);
    }

    /**
     * Books an on-demand domestic pickup slot.
     */
    public PickupRequest requestPickup(String citizenId, WasteType wasteType, double weightKg, LocalDate preferredSlot, String address) {
        Validators.validatePositive(weightKg, "Estimated Weight");
        Validators.validateNonBlank(address, "Pickup Address");
        if (preferredSlot == null || preferredSlot.isBefore(LocalDate.now())) {
            throw new ValidationException("Preferred slot must be today or a future date.");
        }

        String id = "PCK-" + UUID.randomUUID().toString().substring(0, 8);
        PickupRequest req = new PickupRequest(id, citizenId, wasteType, weightKg, preferredSlot, address);
        pickupRequestDao.save(req);
        EventBus.getInstance().publish(new EventBus.PickupRequested(id, citizenId, preferredSlot));

        notificationService.sendNotification(citizenId, "Pickup Scheduled",
                "Pickup request for " + weightKg + " kg of " + wasteType.getDisplayName() + " confirmed for " + preferredSlot);
        return req;
    }

    public List<PickupRequest> getCitizenPickups(String citizenId) {
        return pickupRequestDao.findByCitizen(citizenId);
    }

    /**
     * Posts a dry recyclable listing for local waste pickers.
     */
    public RecyclableListing listRecyclables(String citizenId, WasteType wasteType, double weightKg, String address, double lat, double lng) {
        Validators.validatePositive(weightKg, "Recyclable Weight");
        Validators.validateNonBlank(address, "Listing Address");
        Validators.validateCoordinates(lat, lng);

        double ratePerKg = (wasteType == WasteType.E_WASTE) ? 30.0 : 10.0;
        double estimatedVal = weightKg * ratePerKg;

        String id = "REC-" + UUID.randomUUID().toString().substring(0, 8);
        Location loc = new Location(lat, lng, address);
        RecyclableListing listing = new RecyclableListing(id, citizenId, wasteType, weightKg, address, loc, estimatedVal);
        recyclableListingDao.save(listing);
        EventBus.getInstance().publish(new EventBus.ListingCreated(id, citizenId));

        notificationService.sendNotification(citizenId, "Recyclables Listed",
                "Your " + weightKg + " kg listing is now visible to nearby verified waste pickers.");
        return listing;
    }

    public List<RecyclableListing> getCitizenListings(String citizenId) {
        return recyclableListingDao.findByCitizen(citizenId);
    }

    public List<SegregationLog> getSegregationLogs(String citizenId) {
        return segregationLogDao.findByCitizen(citizenId);
    }

    public List<LeaderboardEntry> getLeaderboard(int limit) {
        return citizenDao.getLeaderboard(limit);
    }

    public List<RewardTransaction> getRewardTransactions(String citizenId) {
        return rewardTransactionDao.findByCitizen(citizenId);
    }

    /**
     * CO3: Atomic reward redemption transaction. Deducts reward points and writes ledger entry.
     * Rolls back completely if points balance is insufficient or an SQL error occurs.
     */
    public boolean redeemPoints(String citizenId, int pointsToRedeem, String rewardType) {
        if (pointsToRedeem <= 0) {
            throw new ValidationException("Points to redeem must be greater than zero.");
        }

        Connection conn = DatabaseConnection.getInstance().getConnection();
        try {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                Citizen citizen = citizenDao.findById(citizenId)
                        .orElseThrow(() -> new ValidationException("Citizen not found: " + citizenId));

                if (citizen.getPointsBalance() < pointsToRedeem) {
                    throw new ValidationException("Insufficient points balance! You have "
                            + citizen.getPointsBalance() + " points, but requested " + pointsToRedeem);
                }

                int newBalance = citizen.getPointsBalance() - pointsToRedeem;
                citizenDao.updatePointsOnly(citizenId, newBalance);

                String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
                String redemptionCode = "WC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                RewardTransaction tx = new RewardTransaction(txId, citizenId, RewardTransaction.Type.REDEEMED,
                    -pointsToRedeem, "Redeemed for " + rewardType + " | Code: " + redemptionCode);
                rewardTransactionDao.save(tx);

                conn.commit();
                conn.setAutoCommit(autoCommit);

                notificationService.sendNotification(citizenId, "Points Redeemed!",
                    "Successfully redeemed " + pointsToRedeem + " points for " + rewardType
                        + ". Code: " + redemptionCode + ". New balance: " + newBalance);
                AppLogger.info("Citizen " + citizenId + " redeemed " + pointsToRedeem + " points for " + rewardType);
                return true;
            } catch (Exception ex) {
                conn.rollback();
                conn.setAutoCommit(autoCommit);
                if (ex instanceof ValidationException ve) throw ve;
                throw new RuntimeException("Transaction failed: " + ex.getMessage(), ex);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during points redemption", e);
        }
    }

    /**
     * Static in-memory database of 40+ household waste items categorized for the segregation guide.
     */
    public static List<SegregationGuideItem> getSegregationGuide() {
        return SEGREGATION_GUIDE_DATA;
    }

    public record SegregationGuideItem(String name, WasteType binType, String category, String instructions) {}

    private static final List<SegregationGuideItem> SEGREGATION_GUIDE_DATA = List.of(
            // Wet Waste (Green Bin)
            new SegregationGuideItem("Vegetable and Fruit Peels", WasteType.WET, "Kitchen Waste", "Dispose in Green Bin. Great for home composting."),
            new SegregationGuideItem("Leftover Cooked Food", WasteType.WET, "Kitchen Waste", "Drain excess gravy/liquid before disposal."),
            new SegregationGuideItem("Coffee Grounds and Tea Bags", WasteType.WET, "Kitchen Waste", "Remove any plastic tags or staples."),
            new SegregationGuideItem("Egg Shells", WasteType.WET, "Kitchen Waste", "Crush lightly and place in Green Bin."),
            new SegregationGuideItem("Meat and Fish Bones", WasteType.WET, "Kitchen Waste", "Keep wrapped in newspaper if storing temporarily."),
            new SegregationGuideItem("Dry Leaves and Garden Clippings", WasteType.WET, "Horticulture", "Do not burn. Deposit in green organic bin."),
            new SegregationGuideItem("Coconut Shells and Coir", WasteType.WET, "Kitchen Waste", "Can be processed into biochar or coir fiber."),
            new SegregationGuideItem("Stale Bread and Bakery Items", WasteType.WET, "Kitchen Waste", "Compostable organic matter."),
            new SegregationGuideItem("Rotten Fruits", WasteType.WET, "Kitchen Waste", "Compostable in organic green stream."),
            new SegregationGuideItem("Used Paper Napkins / Kitchen Towel", WasteType.WET, "Food Soiled", "Food-soiled paper belongs in wet waste composting."),

            // Dry Waste (Blue Bin)
            new SegregationGuideItem("Newspapers and Magazines", WasteType.DRY, "Paper", "Keep dry and tied in flat bundles."),
            new SegregationGuideItem("Cardboard Delivery Boxes", WasteType.DRY, "Paper", "Flatten completely and remove plastic tape."),
            new SegregationGuideItem("Plastic Milk Pouches", WasteType.DRY, "Plastic", "Rinse with water and dry before binning."),
            new SegregationGuideItem("PET Water / Beverage Bottles", WasteType.DRY, "Plastic", "Crush bottle and screw cap back on."),
            new SegregationGuideItem("Aluminum Soda / Beverage Cans", WasteType.DRY, "Metal", "Rinse clean and crush."),
            new SegregationGuideItem("Tin Food Cans", WasteType.DRY, "Metal", "Rinse clean; push lid inside can."),
            new SegregationGuideItem("Glass Sauce and Jam Jars", WasteType.DRY, "Glass", "Rinse clean. Handle carefully to prevent breakage."),
            new SegregationGuideItem("Shampoo and Detergent Bottles", WasteType.DRY, "Plastic (HDPE)", "Rinse and place in dry bin."),
            new SegregationGuideItem("Tetra Pak Juice / Milk Cartons", WasteType.DRY, "Composite Paper", "Flatten and dry. High recovery paper pulp."),
            new SegregationGuideItem("Plastic Grocery Carry Bags", WasteType.DRY, "Plastic", "Bundle together in one pouch."),
            new SegregationGuideItem("Envelopes and Office Paper", WasteType.DRY, "Paper", "Remove plastic windows if possible."),
            new SegregationGuideItem("Empty Aerosol Deodorant Cans", WasteType.DRY, "Metal", "Ensure completely empty and depressurized."),
            new SegregationGuideItem("Plastic Food Containers / Takeaway", WasteType.DRY, "Plastic (PP)", "Rinse oily residue before recycling."),
            new SegregationGuideItem("Old Books and Notebooks", WasteType.DRY, "Paper", "Directly recyclable at paper mills."),
            new SegregationGuideItem("Clean Aluminum Foil", WasteType.DRY, "Metal", "Roll into a tight ball; must be food-free."),

            // Hazardous Waste (Red Bin)
            new SegregationGuideItem("Sanitary Napkins and Diapers", WasteType.HAZARDOUS, "Sanitary", "Wrap in newspaper and mark with red cross."),
            new SegregationGuideItem("Expired Medicines and Tablets", WasteType.HAZARDOUS, "Pharmaceutical", "Do not flush down toilet; keep in blister pack."),
            new SegregationGuideItem("Used Syringes and Needles", WasteType.HAZARDOUS, "Sharps", "Store in puncture-proof bottle before disposal."),
            new SegregationGuideItem("Paint and Thinner Cans", WasteType.HAZARDOUS, "Chemical", "Toxic residues. Seal tight in red bin."),
            new SegregationGuideItem("Pesticides and Weedicide Bottles", WasteType.HAZARDOUS, "Chemical", "High chemical toxicity; municipal hazard treatment."),
            new SegregationGuideItem("Broken Glass and Mirror Shards", WasteType.HAZARDOUS, "Sharps", "Wrap securely in cardboard to protect handlers."),
            new SegregationGuideItem("Used Face Masks and Bandages", WasteType.HAZARDOUS, "Biomedical", "Wrap securely; incinerated for hygiene."),
            new SegregationGuideItem("CFL Bulbs and Fluorescent Tubes", WasteType.HAZARDOUS, "Toxics", "Contains toxic mercury vapor. Do not break."),
            new SegregationGuideItem("Bleach and Chemical Cleaners", WasteType.HAZARDOUS, "Chemical", "Do not mix with other liquids."),
            new SegregationGuideItem("Motor Oil and Brake Fluid Cans", WasteType.HAZARDOUS, "Automotive", "Petrochemical hazard. Specialized collection."),

            // E-Waste (Purple Bin)
            new SegregationGuideItem("Old Mobile Phones and Tablets", WasteType.E_WASTE, "Consumer Electronics", "Valuable rare-earth recovery. Remove SIM."),
            new SegregationGuideItem("AA / AAA Alkaline Batteries", WasteType.E_WASTE, "Batteries", "Tape terminal ends to prevent short circuits."),
            new SegregationGuideItem("Laptop Power Adapters and Cables", WasteType.E_WASTE, "Cables", "Rich copper and polymer recovery."),
            new SegregationGuideItem("Dead Computer Keyboards and Mice", WasteType.E_WASTE, "Peripherals", "Recyclable plastic and circuit boards."),
            new SegregationGuideItem("Defunct Television / Monitor", WasteType.E_WASTE, "Major Electronics", "Heavy metal content. Scheduled e-waste pickup."),
            new SegregationGuideItem("Earphones and Bluetooth Headphones", WasteType.E_WASTE, "Small Electronics", "Contains lithium cell and fine copper coils."),
            new SegregationGuideItem("Defective Microwave and Toaster", WasteType.E_WASTE, "Appliances", "Dismantled for metal chassis and transformers."),
            new SegregationGuideItem("Printer Ink and Toner Cartridges", WasteType.E_WASTE, "Supplies", "Contains toxic fine particulate toner.")
    );
}
