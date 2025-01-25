package com.example.bookjourneybackend.domain.room.domain.repository;

import com.example.bookjourneybackend.domain.book.domain.GenreType;
import com.example.bookjourneybackend.domain.room.domain.Room;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    @Query("SELECT r FROM Room r " +
            "WHERE (:searchTerm IS NULL OR r.roomName LIKE CONCAT('%', :searchTerm, '%') " +
            "OR r.book.bookTitle LIKE CONCAT('%', :searchTerm, '%')) " +
            "AND (:genre IS NULL OR r.book.genre = :genre) " +
            "AND (:recruitStartDate IS NULL OR r.startDate >= :recruitStartDate) " +
            "AND (:recruitEndDate IS NULL OR r.recruitEndDate <= :recruitEndDate) " +
            "AND (:roomStartDate IS NULL OR r.startDate >= :roomStartDate) " +
            "AND (:roomEndDate IS NULL OR r.progressEndDate <= :roomEndDate) " +
            "AND (:recordCount IS NULL OR SIZE(r.records) >= :recordCount) " +
            "ORDER BY r.roomId DESC")
    Slice<Room> findRoomsByFilters(@Param("searchTerm") String searchTerm,
                                   @Param("genre") GenreType genre,
                                   @Param("recruitStartDate") LocalDate recruitStartDate,
                                   @Param("recruitEndDate") LocalDate recruitEndDate,
                                   @Param("roomStartDate") LocalDate roomStartDate,
                                   @Param("roomEndDate") LocalDate roomEndDate,
                                   @Param("recordCount") Integer recordCount,
                                   Pageable pageable);
}
