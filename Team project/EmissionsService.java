package bham.team.service;

import bham.team.domain.Emissions;
import bham.team.domain.TripStorage;
import bham.team.domain.enumeration.Rating;
import bham.team.events.TripStorageEvent;
import bham.team.repository.EmissionsRepository;
import bham.team.repository.TripStorageRepository;
import bham.team.repository.UserRepository;
import bham.team.service.dto.EmissionsDTO;
import bham.team.service.mapper.EmissionsMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link bham.team.domain.Emissions}.
 */
@Service
@Transactional
public class EmissionsService {

    private static final Logger LOG = LoggerFactory.getLogger(EmissionsService.class);

    private final EmissionsRepository emissionsRepository;
    private final EmissionsMapper emissionsMapper;
    private final TripStorageRepository tripStorageRepository;
    private final UserRepository userRepository;

    public EmissionsService(
        EmissionsRepository emissionsRepository,
        EmissionsMapper emissionsMapper,
        TripStorageRepository tripStorageRepository,
        UserRepository userRepository
    ) {
        this.emissionsRepository = emissionsRepository;
        this.emissionsMapper = emissionsMapper;
        this.tripStorageRepository = tripStorageRepository;
        this.userRepository = userRepository;
    }

    /**
     * Save a emissions.
     *
     * @param emissionsDTO the entity to save.
     * @return the persisted entity.
     */
    public EmissionsDTO save(EmissionsDTO emissionsDTO) {
        LOG.debug("Request to save Emissions : {}", emissionsDTO);
        Emissions emissions = emissionsMapper.toEntity(emissionsDTO);
        emissions = emissionsRepository.save(emissions);
        return emissionsMapper.toDto(emissions);
    }

    /**
     * Update a emissions.
     *
     * @param emissionsDTO the entity to save.
     * @return the persisted entity.
     */
    public EmissionsDTO update(EmissionsDTO emissionsDTO) {
        LOG.debug("Request to update Emissions : {}", emissionsDTO);
        Emissions emissions = emissionsMapper.toEntity(emissionsDTO);
        emissions = emissionsRepository.save(emissions);
        return emissionsMapper.toDto(emissions);
    }

    /**
     * Partially update a emissions.
     *
     * @param emissionsDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<EmissionsDTO> partialUpdate(EmissionsDTO emissionsDTO) {
        LOG.debug("Request to partially update Emissions : {}", emissionsDTO);

        return emissionsRepository
            .findById(emissionsDTO.getId())
            .map(existingEmissions -> {
                emissionsMapper.partialUpdate(existingEmissions, emissionsDTO);

                return existingEmissions;
            })
            .map(emissionsRepository::save)
            .map(emissionsMapper::toDto);
    }

    /**
     * Get all the emissions.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<EmissionsDTO> findAll() {
        LOG.debug("Request to get all Emissions");
        return emissionsRepository.findAll().stream().map(emissionsMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one emissions by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<EmissionsDTO> findOne(Long id) {
        LOG.debug("Request to get Emissions : {}", id);
        return emissionsRepository.findById(id).map(emissionsMapper::toDto);
    }

    /**
     * Delete the emissions by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Emissions : {}", id);
        emissionsRepository.deleteById(id);
    }

    /**
     * Event listener for TripStorage events.
     * This will handle automatic creation, update, and deletion of Emissions records.
     */
    @EventListener
    @Transactional
    public void handleTripStorageEvent(TripStorageEvent event) {
        TripStorage tripStorage = event.getTripStorage();
        String action = event.getAction();

        LOG.debug("Received TripStorage event: {} for TripStorage ID: {}", action, tripStorage.getId());

        try {
            switch (action) {
                case "CREATE":
                    // Create new emissions record
                    EmissionsDTO emissionsDTO = createEmissionsForTrip(tripStorage);
                    if (emissionsDTO == null) {
                        LOG.warn("Could not create Emissions for TripStorage ID: {}", tripStorage.getId());
                    }
                    break;
                case "UPDATE":
                    // Update existing emissions record
                    updateEmissionsForTrip(tripStorage);
                    break;
                case "DELETE":
                    // Delete associated emissions record
                    deleteEmissionsForTrip(tripStorage.getId());
                    break;
                default:
                    LOG.debug("Unknown action: {}", action);
            }
        } catch (Exception e) {
            // Log the error but don't propagate it to prevent disrupting the main flow
            LOG.error("Error handling TripStorage event: {} for TripStorage ID: {}", action, tripStorage.getId(), e);
        }
    }

    /**
     * Create emissions for a TripStorage record.
     *
     * @param tripStorage the trip storage record
     * @return the created emissions entity
     */
    @Transactional
    public EmissionsDTO createEmissionsForTrip(TripStorage tripStorage) {
        LOG.debug("Request to create Emissions for TripStorage : {}", tripStorage.getId());

        // Check if we can determine the user
        // Renuka needs to change UserProfile to inbuilt User before I can test that the listener works
        if (tripStorage.getUser() == null || tripStorage.getUser() == null) {
            LOG.info("Skipping creation of Emissions record for TripStorage ID: {} - No user association found", tripStorage.getId());
            return null;
        }

        Emissions emissions = new Emissions();

        // Calculate CO2 based on distance and transport mode
        Float co2 = calculateCO2(tripStorage.getTotalDistance(), tripStorage.getModeOfTransport());
        emissions.setcO2(co2);

        // Determine rating based on CO2, transport mode, and distance
        Rating rating = determineRating(co2, tripStorage.getModeOfTransport(), tripStorage.getTotalDistance());
        emissions.setEmissionRating(rating);

        // Set low emission flag based on transport mode
        String transportMode = tripStorage.getModeOfTransport();
        boolean isLowEmission = transportMode != null && !transportMode.equals("CAR") && !transportMode.equals("Car");
        emissions.setIsLowEmissionTrip(isLowEmission);

        // Set relationships
        emissions.setTrips(tripStorage);
        emissions.setUser(tripStorage.getUser());

        // Save the emissions
        emissions = emissionsRepository.save(emissions);
        return emissionsMapper.toDto(emissions);
    }

    /**
     * Update emissions for a modified TripStorage record.
     */
    @Transactional
    public EmissionsDTO updateEmissionsForTrip(TripStorage tripStorage) {
        LOG.debug("Request to update Emissions for TripStorage : {}", tripStorage.getId());

        // Find existing emissions for this trip
        Optional<Emissions> existingEmissionsOpt = emissionsRepository.findByTripsId(tripStorage.getId());

        return existingEmissionsOpt
            .map(emissions -> {
                // Recalculate CO2 based on updated distance and transport mode
                Float co2 = calculateCO2(tripStorage.getTotalDistance(), tripStorage.getModeOfTransport());
                emissions.setcO2(co2);

                // Recalculate rating
                Rating rating = determineRating(co2, tripStorage.getModeOfTransport(), tripStorage.getTotalDistance());
                emissions.setEmissionRating(rating);

                // Update low emission flag based on transport mode
                String transportMode = tripStorage.getModeOfTransport();
                boolean isLowEmission = transportMode != null && !transportMode.equals("CAR") && !transportMode.equals("Car");
                emissions.setIsLowEmissionTrip(isLowEmission);

                // Save the updated emissions
                emissions = emissionsRepository.save(emissions);
                return emissionsMapper.toDto(emissions);
            })
            .orElseGet(() -> createEmissionsForTrip(tripStorage));
    }

    /**
     * Delete emissions for a deleted TripStorage record.
     */
    @Transactional
    public void deleteEmissionsForTrip(Long tripId) {
        LOG.debug("Request to delete Emissions for TripStorage ID: {}", tripId);

        // Find emissions for this trip
        Optional<Emissions> emissionsOpt = emissionsRepository.findByTripsId(tripId);

        // Delete the emissions record
        emissionsOpt.ifPresent(emissionsRepository::delete);
    }

    /**
     * Calculate CO2 emissions based on distance and transport mode
     */
    private Float calculateCO2(Double distance, String transportMode) {
        if (distance == null || transportMode == null) {
            return 0.0f;
        }

        // Basic CO2 calculation based on transport mode
        if (transportMode.toUpperCase().contains("ELECTRIC")) {
            return (float) (distance * 0.053); // 53g CO2 per km
        } else if (transportMode.toUpperCase().contains("CAR")) {
            return (float) (distance * 0.192); // 192g CO2 per km
        } else if (transportMode.toUpperCase().contains("BUS")) {
            return (float) (distance * 0.105); // 105g CO2 per km per passenger
        } else if (transportMode.toUpperCase().contains("TRAIN")) {
            return (float) (distance * 0.041); // 41g CO2 per km per passenger
        } else {
            return 0.0f; // Walking, cycling
        }
    }

    /**
     * Determine emission rating based on CO2 amount, transport mode, and distance
     */
    private Rating determineRating(Float co2, String transportMode, Double distance) {
        if (co2 == null || distance == null || distance == 0) {
            return Rating.POOR;
        }

        // For zero-emission transport modes (walking, cycling)
        if (transportMode != null && (transportMode.toUpperCase().contains("WALKING") || transportMode.toUpperCase().contains("CYCLING"))) {
            return Rating.EXCELLENT;
        }

        // Calculate emissions per kilometer
        float emissionsPerKm = co2 / distance.floatValue();

        // Rating thresholds based on emissions per km
        // Values based on typical emission levels for different transport modes
        if (emissionsPerKm < 0.05) {
            return Rating.EXCELLENT; // Electric vehicles, efficient public transport
        } else if (emissionsPerKm < 0.12) {
            return Rating.GOOD; // Regular public transport, hybrid vehicles
        } else {
            return Rating.POOR; // Conventional cars, inefficient transport
        }
    }

    /**
     * Get or create emissions for all trips without emissions.
     * This method can be called periodically to ensure all trips have emissions records.
     */
    @Transactional
    public void createMissingEmissionsRecords() {
        LOG.debug("Creating emissions records for trips without emissions");

        List<TripStorage> tripsWithoutEmissions = tripStorageRepository.findAllWithoutEmissions();

        LOG.debug("Found {} trips without emissions", tripsWithoutEmissions.size());

        for (TripStorage trip : tripsWithoutEmissions) {
            createEmissionsForTrip(trip);
        }
    }

    /**
     * Scheduled job to check for any missed TripStorage records and create corresponding Emissions.
     * This is a fallback mechanism in case the event-based approach fails.
     * Runs once per hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void scheduledEmissionsSync() {
        LOG.debug("Running scheduled emissions sync as fallback");
        createMissingEmissionsRecords();
    }

    /**
     * Scheduled job to clean up orphaned emissions records.
     * This checks for emissions records that reference deleted TripStorage records.
     * Runs once per day.
     */
    @Scheduled(cron = "0 0 0 * * ?") // Midnight every day
    public void scheduledEmissionsCleanup() {
        LOG.debug("Running scheduled job to clean up orphaned emissions records");

        // Find emissions with trip IDs that don't exist in TripStorage
        List<Emissions> orphanedEmissions = emissionsRepository.findOrphanedEmissions();

        if (!orphanedEmissions.isEmpty()) {
            LOG.debug("Found {} orphaned emissions records to delete", orphanedEmissions.size());
            emissionsRepository.deleteAll(orphanedEmissions);
        }
    }
}
