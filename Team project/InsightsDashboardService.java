package bham.team.service;

import bham.team.domain.Emissions;
import bham.team.domain.TripStorage;
import bham.team.domain.enumeration.Rating;
import bham.team.repository.EmissionsRepository;
import bham.team.repository.TripStorageRepository;
import bham.team.security.SecurityUtils;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InsightsDashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(InsightsDashboardService.class);

    private final EmissionsRepository emissionsRepository;
    private final TripStorageRepository tripStorageRepository;

    public InsightsDashboardService(EmissionsRepository emissionsRepository, TripStorageRepository tripStorageRepository) {
        this.emissionsRepository = emissionsRepository;
        this.tripStorageRepository = tripStorageRepository;
    }

    /**
     * Get total number of green journeys for the current user in the given time period
     *
     * @param timePeriod "week", "month", or "year"
     * @return number of green journeys
     */
    public int getGreenJourneysCount(String timePeriod) {
        LOG.debug("Request to get green journeys count for period: {}", timePeriod);

        LocalDate startDate = getStartDateForPeriod(timePeriod);
        LocalDate endDate = getEndDateForPeriod(timePeriod);

        // Get all emissions for the current user
        List<Emissions> userEmissions = emissionsRepository.findByUserIsCurrentUser();
        LOG.debug("Found {} emissions for current user", userEmissions.size());

        // Filter to only include low emission trips in the specified time period
        return (int) userEmissions
            .stream()
            .filter(
                e ->
                    e.getIsLowEmissionTrip() &&
                    e.getTrips() != null &&
                    e.getTrips().getTripDate() != null &&
                    !e.getTrips().getTripDate().isBefore(startDate) &&
                    !e.getTrips().getTripDate().isAfter(endDate)
            )
            .count();
    }

    /**
     * Get the average rating for the current user's trips in the given time period
     *
     * @param timePeriod "week", "month", or "year"
     * @return the rating as a string (Excellent, Good, or Poor)
     */
    public String getAverageRating(String timePeriod) {
        LOG.debug("Request to get average rating for period: {}", timePeriod);

        LocalDate startDate = getStartDateForPeriod(timePeriod);
        LocalDate endDate = getEndDateForPeriod(timePeriod);

        // Get all emissions for the current user
        List<Emissions> userEmissions = emissionsRepository.findByUserIsCurrentUser();

        // Filter emissions for the specified time period
        List<Emissions> filteredEmissions = userEmissions
            .stream()
            .filter(
                e ->
                    e.getTrips() != null &&
                    e.getTrips().getTripDate() != null &&
                    !e.getTrips().getTripDate().isBefore(startDate) &&
                    !e.getTrips().getTripDate().isAfter(endDate)
            )
            .collect(Collectors.toList());

        if (filteredEmissions.isEmpty()) {
            return "No Data";
        }

        // Count occurrences of each rating
        long excellentCount = filteredEmissions.stream().filter(e -> e.getEmissionRating() == Rating.EXCELLENT).count();
        long goodCount = filteredEmissions.stream().filter(e -> e.getEmissionRating() == Rating.GOOD).count();
        long poorCount = filteredEmissions.stream().filter(e -> e.getEmissionRating() == Rating.POOR).count();

        // Determine most common rating
        if (excellentCount >= goodCount && excellentCount >= poorCount) {
            return "Excellent";
        } else if (goodCount >= excellentCount && goodCount >= poorCount) {
            return "Good";
        } else {
            return "Poor";
        }
    }

    /**
     * Get the total CO2 emissions for the current user in the given time period
     *
     * @param timePeriod "week", "month", or "year"
     * @return the total CO2 emissions in kg
     */
    public float getTotalCO2Emissions(String timePeriod) {
        LOG.debug("Request to get total CO2 emissions for period: {}", timePeriod);

        LocalDate startDate = getStartDateForPeriod(timePeriod);
        LocalDate endDate = getEndDateForPeriod(timePeriod);

        // Get all emissions for the current user
        List<Emissions> userEmissions = emissionsRepository.findByUserIsCurrentUser();

        // Filter emissions for the specified time period and sum CO2
        return userEmissions
            .stream()
            .filter(
                e ->
                    e.getTrips() != null &&
                    e.getTrips().getTripDate() != null &&
                    !e.getTrips().getTripDate().isBefore(startDate) &&
                    !e.getTrips().getTripDate().isAfter(endDate)
            )
            .map(Emissions::getcO2)
            .reduce(0.0f, Float::sum);
    }

    /**
     * Get emissions data for chart display
     *
     * @param timePeriod "week", "month", or "year"
     * @return Map containing labels and emissions data series
     */
    public Map<String, Object> getEmissionsChartData(String timePeriod) {
        LOG.debug("Request to get emissions chart data for period: {}", timePeriod);

        LocalDate startDate = getStartDateForPeriod(timePeriod);
        LocalDate endDate = getEndDateForPeriod(timePeriod);

        // Get all emissions for the current user
        List<Emissions> userEmissions = emissionsRepository.findByUserIsCurrentUser();
        // Get all emissions from all users for average calculation
        List<Emissions> allUserEmissions = emissionsRepository.findAll();

        // Filter current user emissions for the specified time period
        List<Emissions> filteredUserEmissions = userEmissions
            .stream()
            .filter(
                e ->
                    e.getTrips() != null &&
                    e.getTrips().getTripDate() != null &&
                    !e.getTrips().getTripDate().isBefore(startDate) &&
                    !e.getTrips().getTripDate().isAfter(endDate)
            )
            .collect(Collectors.toList());

        // Filter all user emissions for the specified time period
        List<Emissions> filteredAllEmissions = allUserEmissions
            .stream()
            .filter(
                e ->
                    e.getTrips() != null &&
                    e.getTrips().getTripDate() != null &&
                    !e.getTrips().getTripDate().isBefore(startDate) &&
                    !e.getTrips().getTripDate().isAfter(endDate)
            )
            .collect(Collectors.toList());

        // Prepare result data
        Map<String, Object> result = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Float> yourData = new ArrayList<>();
        List<Float> avgData = new ArrayList<>();

        // Generate data based on time period
        if (timePeriod.equalsIgnoreCase("week")) {
            // For week, we want daily data (Monday to Sunday)
            Map<LocalDate, Float> dailyUserEmissions = new HashMap<>();
            Map<LocalDate, Map<String, Float>> dailyUserTotals = new HashMap<>();

            // Initialize structure for all days in the week
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                dailyUserTotals.put(current, new HashMap<>());
                current = current.plusDays(1);
            }

            // Group user emissions by day
            filteredUserEmissions.forEach(e -> {
                LocalDate tripDate = e.getTrips().getTripDate();
                dailyUserEmissions.merge(tripDate, e.getcO2(), Float::sum);
            });

            // Group all user emissions by day and by user
            filteredAllEmissions.forEach(e -> {
                if (e.getUser() != null && e.getUser().getLogin() != null) {
                    LocalDate tripDate = e.getTrips().getTripDate();
                    String userLogin = e.getUser().getLogin();

                    Map<String, Float> userMap = dailyUserTotals.getOrDefault(tripDate, new HashMap<>());
                    userMap.merge(userLogin, e.getcO2(), Float::sum);
                    dailyUserTotals.put(tripDate, userMap);
                }
            });

            // Generate data for each day of the week
            current = startDate;
            while (!current.isAfter(endDate)) {
                labels.add(current.getDayOfWeek().toString().substring(0, 3)); // Mon, Tue, etc.
                yourData.add(dailyUserEmissions.getOrDefault(current, 0.0f));

                // Calculate average for this day across all users
                Map<String, Float> userTotals = dailyUserTotals.getOrDefault(current, new HashMap<>());
                float sumEmissions = userTotals.values().stream().reduce(0.0f, Float::sum);
                int userCount = userTotals.size();

                // Avoid division by zero
                float avgEmission = userCount > 0 ? sumEmissions / userCount : 0.0f;
                avgData.add(avgEmission);

                current = current.plusDays(1);
            }
        } else if (timePeriod.equalsIgnoreCase("month")) {
            // For month, we want weekly data
            labels.add("Week 1");
            labels.add("Week 2");
            labels.add("Week 3");
            labels.add("Week 4");

            // Create data structure for weekly averages
            List<Map<String, Object>> weeklyAverages = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Map<String, Object> weekData = new HashMap<>();
                weekData.put("sum", 0.0f);
                weekData.put("count", 0);
                weeklyAverages.add(weekData);
            }

            // Split the month into 4 weeks and calculate user emissions
            LocalDate weekStart = startDate;
            for (int i = 0; i < 4; i++) {
                LocalDate weekEnd = i < 3 ? weekStart.plusDays(6) : endDate;

                final LocalDate ws = weekStart;
                final LocalDate we = weekEnd;
                final int weekIndex = i;

                // Calculate user's weekly total
                float weekTotal = filteredUserEmissions
                    .stream()
                    .filter(e -> !e.getTrips().getTripDate().isBefore(ws) && !e.getTrips().getTripDate().isAfter(we))
                    .map(Emissions::getcO2)
                    .reduce(0.0f, Float::sum);

                yourData.add(weekTotal);

                // Calculate weekly average by user first
                Map<String, Float> weeklyUserTotals = new HashMap<>();

                filteredAllEmissions
                    .stream()
                    .filter(e -> !e.getTrips().getTripDate().isBefore(ws) && !e.getTrips().getTripDate().isAfter(we))
                    .forEach(e -> {
                        if (e.getUser() != null && e.getUser().getLogin() != null) {
                            String userLogin = e.getUser().getLogin();
                            weeklyUserTotals.merge(userLogin, e.getcO2(), Float::sum);
                        }
                    });

                // Sum up all users' totals and store in weeklyAverages
                Map<String, Object> weekData = weeklyAverages.get(weekIndex);
                float weeklySum = weeklyUserTotals.values().stream().reduce(0.0f, Float::sum);
                int userCount = weeklyUserTotals.size();

                weekData.put("sum", weeklySum);
                weekData.put("count", Math.max(1, userCount)); // Avoid division by zero

                weekStart = weekEnd.plusDays(1);
            }

            // Calculate weekly averages
            for (Map<String, Object> weekData : weeklyAverages) {
                float sum = (float) weekData.get("sum");
                int count = (int) weekData.get("count");
                // Avoid division by zero
                float avg = count > 0 ? sum / count : 0.0f;
                avgData.add(avg);
            }
        } else { // year
            // For year, we want monthly data
            // Create data structure for monthly averages
            Map<Integer, Map<String, Object>> monthlyAverages = new HashMap<>();
            for (int i = 1; i <= 12; i++) {
                Map<String, Object> monthData = new HashMap<>();
                monthData.put("sum", 0.0f);
                monthData.put("count", 0);
                monthlyAverages.put(i, monthData);
            }

            // Group by month and by user
            Map<Integer, Map<String, Float>> userMonthlyTotals = new HashMap<>();

            // Initialize the structure for all months
            for (int i = 1; i <= 12; i++) {
                userMonthlyTotals.put(i, new HashMap<>());
            }

            // Calculate monthly emissions by user
            filteredAllEmissions.forEach(e -> {
                if (e.getUser() != null && e.getUser().getLogin() != null) {
                    int month = e.getTrips().getTripDate().getMonthValue();
                    String userLogin = e.getUser().getLogin();

                    Map<String, Float> userMap = userMonthlyTotals.get(month);
                    userMap.merge(userLogin, e.getcO2(), Float::sum);
                }
            });

            // Calculate monthly averages based on user totals
            for (int month = 1; month <= 12; month++) {
                Map<String, Float> userTotals = userMonthlyTotals.get(month);
                Map<String, Object> monthData = monthlyAverages.get(month);

                float monthlySum = userTotals.values().stream().reduce(0.0f, Float::sum);
                int userCount = userTotals.size();

                monthData.put("sum", monthlySum);
                monthData.put("count", Math.max(1, userCount)); // Avoid division by zero
            }

            // Generate monthly data
            for (int month = 1; month <= 12; month++) {
                LocalDate monthDate = LocalDate.of(LocalDate.now().getYear(), month, 1);
                labels.add(monthDate.getMonth().toString().substring(0, 3)); // Jan, Feb, etc.

                final int m = month;
                float monthTotal = filteredUserEmissions
                    .stream()
                    .filter(e -> e.getTrips().getTripDate().getMonthValue() == m)
                    .map(Emissions::getcO2)
                    .reduce(0.0f, Float::sum);

                yourData.add(monthTotal);

                // Calculate average
                Map<String, Object> monthData = monthlyAverages.get(month);
                float sum = (float) monthData.get("sum");
                int count = (int) monthData.get("count");
                // Avoid division by zero
                float avg = count > 0 ? sum / count : 0.0f;
                avgData.add(avg);
            }
        }

        result.put("labels", labels);
        result.put("yourData", yourData);
        result.put("avgData", avgData);

        // Add comparison data
        Map<String, Object> comparisonData = calculateComparisonData(yourData, avgData);
        result.put("comparisonData", comparisonData);

        return result;
    }

    /**
     * Calculate the comparison data between user emissions and average user emissions
     *
     * @param timePeriod "week", "month", or "year"
     * @return Map containing comparison data
     */
    public Map<String, Object> getEmissionsComparisonData(String timePeriod) {
        LOG.debug("Request to get emissions comparison data for period: {}", timePeriod);

        LocalDate startDate = getStartDateForPeriod(timePeriod);
        LocalDate endDate = getEndDateForPeriod(timePeriod);

        // Get current user emissions
        List<Emissions> userEmissions = emissionsRepository.findByUserIsCurrentUser();

        // Get all emissions from all users
        List<Emissions> allUserEmissions = emissionsRepository.findAll();

        // Get current user login
        Optional<String> currentUserLogin = SecurityUtils.getCurrentUserLogin();
        String currentUser = currentUserLogin.orElse("");

        // Filter current user emissions for the specified time period
        float totalUserEmissions = userEmissions
            .stream()
            .filter(
                e ->
                    e.getTrips() != null &&
                    e.getTrips().getTripDate() != null &&
                    !e.getTrips().getTripDate().isBefore(startDate) &&
                    !e.getTrips().getTripDate().isAfter(endDate)
            )
            .map(Emissions::getcO2)
            .reduce(0.0f, Float::sum);

        // Filter and calculate average emissions for other users
        // Group by user to get per-user totals first
        Map<String, Float> otherUserTotals = new HashMap<>();

        allUserEmissions
            .stream()
            .filter(
                e ->
                    e.getTrips() != null &&
                    e.getTrips().getTripDate() != null &&
                    !e.getTrips().getTripDate().isBefore(startDate) &&
                    !e.getTrips().getTripDate().isAfter(endDate) &&
                    e.getUser() != null &&
                    e.getUser().getLogin() != null &&
                    !e.getUser().getLogin().equals(currentUser)
            )
            .forEach(e -> {
                String userLogin = e.getUser().getLogin();
                otherUserTotals.merge(userLogin, e.getcO2(), Float::sum);
            });

        // Calculate the average total emissions per user
        float avgUserEmissions = 0.0f;
        if (!otherUserTotals.isEmpty()) {
            avgUserEmissions = otherUserTotals.values().stream().reduce(0.0f, Float::sum) / otherUserTotals.size();
        }

        // Calculate percentage difference
        int percentageDiff = 0;
        boolean isBetterThanAverage = true;

        if (avgUserEmissions > 0) {
            if (totalUserEmissions < avgUserEmissions) {
                // User emits less than average (good)
                percentageDiff = Math.round(((avgUserEmissions - totalUserEmissions) / avgUserEmissions) * 100);
                isBetterThanAverage = true;
            } else {
                // User emits more than average (not so good)
                percentageDiff = Math.round(((totalUserEmissions - avgUserEmissions) / avgUserEmissions) * 100);
                isBetterThanAverage = false;
            }
        }

        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("userEmissions", totalUserEmissions);
        result.put("avgEmissions", avgUserEmissions);
        result.put("percentageDiff", percentageDiff);
        result.put("isBetterThanAverage", isBetterThanAverage);

        return result;
    }

    /**
     * Helper method to calculate comparison data from the chart data arrays
     */
    private Map<String, Object> calculateComparisonData(List<Float> yourData, List<Float> avgData) {
        // Sum up all emissions
        float totalUserEmissions = yourData.stream().reduce(0.0f, Float::sum);
        float totalAvgEmissions = avgData.stream().reduce(0.0f, Float::sum);

        // Calculate percentage difference
        int percentageDiff = 0;
        boolean isBetterThanAverage = true;

        if (totalAvgEmissions > 0) {
            if (totalUserEmissions < totalAvgEmissions) {
                // User emits less than average (good)
                percentageDiff = Math.round(((totalAvgEmissions - totalUserEmissions) / totalAvgEmissions) * 100);
                isBetterThanAverage = true;
            } else {
                // User emits more than average (not so good)
                percentageDiff = Math.round(((totalUserEmissions - totalAvgEmissions) / totalAvgEmissions) * 100);
                isBetterThanAverage = false;
            }
        }

        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("userEmissions", totalUserEmissions);
        result.put("avgEmissions", totalAvgEmissions);
        result.put("percentageDiff", percentageDiff);
        result.put("isBetterThanAverage", isBetterThanAverage);

        return result;
    }

    /**
     * Helper method to get the start date for a given time period
     */
    private LocalDate getStartDateForPeriod(String timePeriod) {
        LocalDate now = LocalDate.now();

        switch (timePeriod.toLowerCase()) {
            case "week":
                // Start from the beginning of the current week (Monday)
                return now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            case "month":
                // Start from the beginning of the current month
                return now.withDayOfMonth(1);
            case "year":
                // Start from the beginning of the current year
                return now.withYear(now.getYear()).withDayOfYear(1);
            default:
                return now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)); // Default to current week
        }
    }

    /**
     * Helper method to get the end date for a given time period (inclusive)
     */
    private LocalDate getEndDateForPeriod(String timePeriod) {
        LocalDate now = LocalDate.now();

        switch (timePeriod.toLowerCase()) {
            case "week":
                // End with the Sunday of the current week
                return now.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
            case "month":
                // End with the last day of the current month
                return now.with(TemporalAdjusters.lastDayOfMonth());
            case "year":
                // End with the last day of the current year
                return now.with(TemporalAdjusters.lastDayOfYear());
            default:
                return now.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)); // Default to end of week
        }
    }

    /**
     * Get rating breakdown data for chart display
     *
     * @param timePeriod "week", "month", or "year"
     * @return Map containing counts of each rating category
     */
    public Map<String, Object> getRatingChartData(String timePeriod) {
        LOG.debug("Request to get rating chart data for period: {}", timePeriod);

        LocalDate startDate = getStartDateForPeriod(timePeriod);
        LocalDate endDate = getEndDateForPeriod(timePeriod);

        // Get all emissions for the current user
        List<Emissions> userEmissions = emissionsRepository.findByUserIsCurrentUser();

        // Filter emissions for the specified time period
        List<Emissions> filteredEmissions = userEmissions
            .stream()
            .filter(
                e ->
                    e.getTrips() != null &&
                    e.getTrips().getTripDate() != null &&
                    !e.getTrips().getTripDate().isBefore(startDate) &&
                    !e.getTrips().getTripDate().isAfter(endDate)
            )
            .collect(Collectors.toList());

        // Count occurrences of each rating
        long excellentCount = filteredEmissions.stream().filter(e -> e.getEmissionRating() == Rating.EXCELLENT).count();
        long goodCount = filteredEmissions.stream().filter(e -> e.getEmissionRating() == Rating.GOOD).count();
        long poorCount = filteredEmissions.stream().filter(e -> e.getEmissionRating() == Rating.POOR).count();

        // Prepare result data
        Map<String, Object> result = new HashMap<>();
        result.put("excellent", excellentCount);
        result.put("good", goodCount);
        result.put("poor", poorCount);
        result.put("total", excellentCount + goodCount + poorCount);

        return result;
    }

    /**
     * Get the trip with highest emissions for the current user in the given time period
     *
     * @param timePeriod "week", "month", or "year"
     * @return Map containing trip details and equivalent coal amount
     */
    public Map<String, Object> getHighestEmissionTrip(String timePeriod) {
        LOG.debug("Request to get highest emission trip for period: {}", timePeriod);

        LocalDate startDate = getStartDateForPeriod(timePeriod);
        LocalDate endDate = getEndDateForPeriod(timePeriod);

        // Get all emissions for the current user
        List<Emissions> userEmissions = emissionsRepository.findByUserIsCurrentUser();

        // Filter emissions for the specified time period
        List<Emissions> filteredEmissions = userEmissions
            .stream()
            .filter(
                e ->
                    e.getTrips() != null &&
                    e.getTrips().getTripDate() != null &&
                    !e.getTrips().getTripDate().isBefore(startDate) &&
                    !e.getTrips().getTripDate().isAfter(endDate)
            )
            .collect(Collectors.toList());

        // Find the emission with the highest CO2 value
        Optional<Emissions> highestEmission = filteredEmissions.stream().max(Comparator.comparing(Emissions::getcO2));

        Map<String, Object> result = new HashMap<>();

        if (highestEmission.isPresent()) {
            Emissions emission = highestEmission.get();
            Long tripId = emission.getTrips().getId();

            // Get fresh trip data directly from repository
            TripStorage trip = tripStorageRepository.findById(tripId).orElse(null);

            if (trip == null) {
                result.put("found", false);
                return result;
            }

            // Calculate with null checks
            float co2Value = emission.getcO2() != null ? emission.getcO2() : 0f;
            float coalEquivalent = co2Value * 0.35f;

            // Try to get distance from entity first
            Double distance = trip.getTotalDistance();

            // If distance is null or zero, try direct database query as fallback
            if (distance == null || distance == 0) {
                LOG.debug("Trip {} has zero/null distance via entity, trying direct query", tripId);
                Double rawDistance = tripStorageRepository.getRawDistanceById(tripId);
                distance = rawDistance != null ? rawDistance : 0.0;
                LOG.debug("Direct query result for trip {}: distance = {}", tripId, distance);
            }

            // Format date nicely
            String formattedDate = trip.getTripDate().format(DateTimeFormatter.ofPattern("EEEE dd'th' MMMM"));

            // Build result with null checks
            result.put("found", true);
            result.put("co2Value", co2Value);
            result.put("date", formattedDate);
            result.put("startPoint", trip.getStartPoint());
            result.put("endPoint", trip.getEndPoint());
            result.put("distance", distance);
            result.put("transportMode", trip.getModeOfTransport());
            result.put("coalEquivalent", coalEquivalent);

            LOG.debug("Returning trip data: {}", result);
        } else {
            result.put("found", false);
        }

        return result;
    }
}
