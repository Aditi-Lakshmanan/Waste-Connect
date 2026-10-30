package com.wasteconnect.engine;

import com.wasteconnect.dao.BinDao;
import com.wasteconnect.dao.BinDaoImpl;
import com.wasteconnect.dao.ComplaintDao;
import com.wasteconnect.dao.ComplaintDaoImpl;
import com.wasteconnect.dao.ZoneDao;
import com.wasteconnect.dao.ZoneDaoImpl;
import com.wasteconnect.model.*;

import java.util.*;

/**
 * Novelty Engine: Hotspot Detector.
 * Identifies chronic Garbage Vulnerable Points (GVPs) by correlating complaint density,
 * recurring bin overflows, and commercial market patterns.
 */
public class HotspotDetector {
    private final BinDao binDao;
    private final ComplaintDao complaintDao;
    private final ZoneDao zoneDao;

    public HotspotDetector() {
        this(new BinDaoImpl(), new ComplaintDaoImpl(), new ZoneDaoImpl());
    }

    public HotspotDetector(BinDao binDao, ComplaintDao complaintDao, ZoneDao zoneDao) {
        this.binDao = binDao;
        this.complaintDao = complaintDao;
        this.zoneDao = zoneDao;
    }

    /**
     * Scans all municipal zones, clusters vulnerable coordinates, and produces ranked HotspotReports.
     */
    public List<HotspotReport> detectHotspots() {
        List<Zone> zones = zoneDao.findAll();
        List<Complaint> allComplaints = complaintDao.findAll();
        List<Bin> allBins = binDao.findAll();

        List<HotspotReport> reports = new ArrayList<>();

        for (Zone zone : zones) {
            List<Complaint> zoneComplaints = allComplaints.stream()
                    .filter(c -> zone.getId().equals(c.getZoneId()))
                    .toList();

            List<Bin> zoneBins = allBins.stream()
                    .filter(b -> zone.getId().equals(b.getZoneId()))
                    .toList();

            long overflowCount = zoneBins.stream()
                    .filter(b -> b.getStatus() == BinStatus.OVERFLOW || b.getCurrentFillPercentage() >= 90.0)
                    .count();

            int complaintCount = zoneComplaints.size();

            // Calculate Composite Risk Score (0.0 to 100.0)
            // Factors: complaint volume, overflow volume, market area multiplier
            double marketMultiplier = zone.isMarketArea() ? 1.3 : 1.0;
            double complaintScore = Math.min(50.0, complaintCount * 5.0);
            double overflowScore = Math.min(50.0, overflowCount * 3.5);
            double rawRisk = (complaintScore + overflowScore) * marketMultiplier;
            double riskScore = Math.min(100.0, Math.round(rawRisk * 10.0) / 10.0);

            String riskLevel;
            String recommendation;

            if (riskScore >= 70.0) {
                riskLevel = "CRITICAL";
                recommendation = "Deploy 2 additional 660L compactor bins, increase collection to twice-daily, and install anti-dumping surveillance.";
            } else if (riskScore >= 40.0) {
                riskLevel = "MEDIUM";
                recommendation = "Increase collection frequency during peak hours and deploy citizen segregation awareness volunteers.";
            } else {
                riskLevel = "LOW";
                recommendation = "Maintain regular standard route schedule. Monitor fill rates during weekends.";
            }

            reports.add(new HotspotReport(
                    zone.getId(),
                    zone.getName(),
                    zone.getCenterLat(),
                    zone.getCenterLng(),
                    complaintCount,
                    (int) overflowCount,
                    riskScore,
                    riskLevel,
                    recommendation
            ));
        }

        // CO4: Stream + lambda sort in descending order of risk score
        return reports.stream()
                .sorted(Comparator.comparingDouble(HotspotReport::riskScore).reversed())
                .toList();
    }
}
