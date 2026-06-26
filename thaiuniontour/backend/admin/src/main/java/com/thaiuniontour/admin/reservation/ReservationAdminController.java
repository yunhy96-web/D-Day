package com.thaiuniontour.admin.reservation;

import com.thaiuniontour.domain.reservation.model.CancelReason;
import com.thaiuniontour.domain.reservation.model.ReservationStatus;
import com.thaiuniontour.domain.reservation.repository.ReservationRepository;
import com.thaiuniontour.domain.reservation.service.ReservationCommandService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequestMapping("/admin/reservations")
public class ReservationAdminController {

    private final ReservationRepository reservationRepository;
    private final ReservationCommandService reservationCommandService;

    public ReservationAdminController(ReservationRepository reservationRepository,
                                      ReservationCommandService reservationCommandService) {
        this.reservationRepository = reservationRepository;
        this.reservationCommandService = reservationCommandService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status, Model model) {
        if ("PENDING".equals(status)) {
            model.addAttribute("reservations",
                    reservationRepository.findByStatusOrderByCreatedAtDesc(ReservationStatus.PENDING));
            model.addAttribute("filterStatus", "PENDING");
        } else {
            model.addAttribute("reservations",
                    reservationRepository.findAllByOrderByCreatedAtDesc());
            model.addAttribute("filterStatus", "ALL");
        }
        return "reservation/reservation-list";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable UUID id) {
        reservationCommandService.confirm(id);
        return "redirect:/admin/reservations";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable UUID id) {
        reservationCommandService.cancel(id, CancelReason.ADMIN);
        return "redirect:/admin/reservations";
    }
}
