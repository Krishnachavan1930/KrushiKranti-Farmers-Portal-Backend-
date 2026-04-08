package com.krushikranti.repository;

import com.krushikranti.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

        /**
         * Find all events sorted by start date (upcoming events first)
         */
        Page<Event> findAllByOrderByStartDateAsc(Pageable pageable);

        /**
         * Find events with optional filtering by search term, location, date range,
         * etc.
         */
        @Query("SELECT e FROM Event e WHERE " +
                        "(LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
                        +
                        "(:state IS NULL OR e.state = :state) AND " +
                        "(:district IS NULL OR e.district = :district) AND " +
                        "(:zone IS NULL OR e.zone = :zone) AND " +
                        "(:fromDate IS NULL OR e.startDate >= :fromDate) AND " +
                        "(:toDate IS NULL OR e.endDate <= :toDate) " +
                        "ORDER BY e.startDate ASC")
        Page<Event> findEventsByFilters(
                        @Param("search") String search,
                        @Param("state") String state,
                        @Param("district") String district,
                        @Param("zone") String zone,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        Pageable pageable);

        /**
         * Find upcoming events (by start date >= today)
         */
        Page<Event> findByStartDateGreaterThanEqualOrderByStartDateAsc(LocalDate date, Pageable pageable);
}
