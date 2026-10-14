package com.dday.family.shift;

import com.dday.family.auth.Me;
import com.dday.family.shift.ShiftService.Day;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 설계 §10. memberId 를 주면 그 사람 것(shift.write.any 필요), 없으면 내 것. 탭 1번 = 요청 1건(자동 저장). */
@RestController
@RequiredArgsConstructor
public class ShiftController {
    private final ShiftService shifts;

    public record DayBody(Long memberId, Long typeId, String note) {}
    public record DaysBody(Long memberId, List<Day> days) {}
    public record FillBody(Long memberId, String start, Integer months) {}
    public record PresetBody(Long memberId, String system) {}
    public record PatternBody(Long memberId, List<Long> pattern) {}

    @GetMapping("/api/shifts")
    public Map<String, Object> month(@RequestParam String month, @RequestParam(defaultValue = "me") String scope,
                                     @RequestParam(required = false) Long memberId, Me me) {
        return Map.of("people", shifts.month(me, month, "family".equals(scope), memberId));
    }

    @PutMapping("/api/shifts/{day}")
    public Map<String, Object> day(@PathVariable String day, @RequestBody DayBody b, Me me) {
        return Map.of("previous", shifts.setDay(me, shifts.target(me, b.memberId()), day, b.typeId(), b.note()));
    }

    @PutMapping("/api/shifts")
    public ResponseEntity<Void> days(@RequestBody DaysBody b, Me me) {
        shifts.setDays(me, shifts.target(me, b.memberId()), b.days());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/shifts/fill")
    public Map<String, Object> fill(@RequestBody FillBody b, Me me) {
        return Map.of("previous", shifts.fill(me, shifts.target(me, b.memberId()), b.start(), b.months() == null ? 3 : b.months()));
    }

    @PostMapping("/api/shift-setting/preset")
    public Map<String, Object> preset(@RequestBody PresetBody b, Me me) {
        long mid = shifts.target(me, b.memberId());
        return Map.of("setting", shifts.applyPreset(me, mid, b.system(), me.lang()), "types", shifts.types(me.familyId(), mid));
    }

    @PutMapping("/api/shift-setting/pattern")
    public ResponseEntity<Void> pattern(@RequestBody PatternBody b, Me me) {
        shifts.savePattern(me, shifts.target(me, b.memberId()), b.pattern());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/shift-types")
    public Map<String, Object> createType(@RequestParam(required = false) Long memberId, @RequestBody ShiftService.TypeBody b, Me me) {
        return Map.of("id", shifts.createType(me, shifts.target(me, memberId), b));
    }

    @PutMapping("/api/shift-types/{id}")
    public ResponseEntity<Void> updateType(@PathVariable long id, @RequestBody ShiftService.TypeBody b, Me me) {
        shifts.updateType(me, id, b);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/shift-types/{id}")
    public ResponseEntity<Void> deleteType(@PathVariable long id, Me me) {
        shifts.deleteType(me, id);
        return ResponseEntity.noContent().build();
    }
}
