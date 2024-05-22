package roomescape.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import roomescape.domain.Member;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.Waiting;
import roomescape.handler.exception.CustomException;
import roomescape.handler.exception.ExceptionCode;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;
import roomescape.service.dto.request.WaitingRequest;
import roomescape.service.dto.response.WaitingResponse;

@Service
public class WaitingService {

    private final ReservationRepository reservationRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final ThemeRepository themeRepository;
    private final MemberRepository memberRepository;
    private final WaitingRepository waitingRepository;

    public WaitingService(ReservationRepository reservationRepository, ReservationTimeRepository reservationTimeRepository,
                          ThemeRepository themeRepository,
                          MemberRepository memberRepository,
                          WaitingRepository waitingRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.themeRepository = themeRepository;
        this.memberRepository = memberRepository;
        this.waitingRepository = waitingRepository;
    }


    public WaitingResponse createWaiting(WaitingRequest waitingRequest, Long memberId) {
        Member member = getMember(memberId);
        ReservationTime reservationTime = getReservationTime(waitingRequest.timeId());
        Theme theme = getTheme(waitingRequest.themeId());

        validateIsPastTime(waitingRequest.date(), reservationTime);

        if (waitingRepository.existsWaitingByDateAndAndThemeAndMember(waitingRequest.date(), theme, member)) {
            throw new CustomException(ExceptionCode.ALREADY_WAITING_EXIST);
        }

        if (reservationRepository.existsByTimeAndDateAndThemeAndMember(reservationTime, waitingRequest.date(), theme, member)) {
            throw new CustomException(ExceptionCode.DUPLICATE_RESERVATION);
        }

        Waiting waiting = waitingRequest.toEntity(member, reservationTime, theme);
        Waiting savedReservation = waitingRepository.save(waiting);
        return WaitingResponse.from(savedReservation);
    }

    private void validateIsPastTime(LocalDate date, ReservationTime time) {
        LocalDateTime reservationDateTime = LocalDateTime.of(date, time.getStartAt());
        if (LocalDateTime.now().isAfter(reservationDateTime)) {
            throw new CustomException(ExceptionCode.PAST_TIME_SLOT_RESERVATION);
        }
    }

    public List<WaitingResponse> findAllWaitings() {
        List<Waiting> waitings = waitingRepository.findAll();
        return waitings.stream()
                .map(WaitingResponse::from)
                .toList();
    }

    public void deleteWaiting(Long id) {
        waitingRepository.deleteById(id);
    }

    private Theme getTheme(Long id) {
        return themeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_THEME));
    }

    private ReservationTime getReservationTime(Long id) {
        return reservationTimeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_RESERVATION_TIME));
    }

    private Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));
    }
}
