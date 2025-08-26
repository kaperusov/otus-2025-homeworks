package ru.otus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.otus.model.Reservation;
import ru.otus.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeliveryService {

    private final ReservationRepository reservationRepository;

    public UUID reserveDelivery(UUID orderId, String address, LocalDateTime preferredTimeSlot) {

        Reservation reservation = new Reservation();
        reservation.setOrderId( orderId );
        reservation.setAddress( address );
        reservation.setPreferredTimeSlot( preferredTimeSlot );

        Reservation reserve = reservationRepository.save(reservation);

        return reserve.getId();
    }

    public void cancelReservation(UUID deliveryId) {

        Reservation reservation = reservationRepository.findById(deliveryId)
                .orElseThrow(() -> new ResponseStatusException( HttpStatus.NOT_FOUND ));

        reservationRepository.delete(reservation);
    }
}
