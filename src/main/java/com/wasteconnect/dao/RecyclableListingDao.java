package com.wasteconnect.dao;

import com.wasteconnect.model.ListingStatus;
import com.wasteconnect.model.RecyclableListing;

import java.util.List;

public interface RecyclableListingDao extends Repository<RecyclableListing, String> {
    List<RecyclableListing> findByStatus(ListingStatus status);
    List<RecyclableListing> findByCitizen(String citizenId);
    List<RecyclableListing> findByPicker(String pickerId);
    boolean claimListing(String listingId, String pickerId);
    boolean completeCollection(String listingId, String pickerId, double actualWeight, double finalEarnings);
}
