package com.dongly.modules.booking.repository;

import com.dongly.modules.booking.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    Optional<Ticket> findByBookingItemId(UUID bookingItemId);

    boolean existsByTicketCode(String ticketCode);
}
