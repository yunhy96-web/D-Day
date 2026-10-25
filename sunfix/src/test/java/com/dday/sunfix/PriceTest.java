package com.dday.sunfix;

import com.dday.sunfix.code.CodeService;
import com.dday.sunfix.code.CodeService.Selection;
import com.dday.sunfix.common.BadRequest;
import com.dday.sunfix.price.PriceService;
import com.dday.sunfix.price.PriceService.Line;
import com.dday.sunfix.price.PriceService.Quote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 가격 합산·공개/미공개 분류·선택 검증을 예시 데이터(V2__seed)로 확인. 메모리 DB */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:file:pricetest?mode=memory&cache=shared")
class PriceTest {
    @Autowired CodeService codes;
    @Autowired PriceService prices;

    @Test
    void totalsAreBasePlusOptionsSortedAscending() {
        Selection s = codes.select("WALLPAPER", List.of("WALLPAPER_AREA_20", "WALLPAPER_TYPE_SILK", "WALLPAPER_CEIL_NO"), "SEOUL_GANGNAM");
        PriceService.Result r = prices.compare(s);
        assertFalse(r.open().isEmpty());
        int prev = 0;
        for (Quote q : r.open()) {
            assertEquals(q.basePrice() + q.lines().stream().mapToInt(Line::price).sum(), q.total());
            assertTrue(q.total() >= prev);
            prev = q.total();
        }
        assertEquals(r.open().get(0).total(), r.min());
        for (Quote q : r.closed()) assertTrue(q.basePrice() == null || q.lines().stream().anyMatch(l -> l.price() == null));
        assertEquals("WALLPAPER_AREA_20,WALLPAPER_CEIL_NO,WALLPAPER_TYPE_SILK", s.optionKey());
    }

    @Test
    void categoryTreeOnlyPricesLeaves() {
        assertEquals(List.of("BATHROOM", "DRAIN"), codes.path("DRAIN").stream().map(CodeService.Code::code).toList());
        assertTrue(codes.leaves("BATHROOM", true).stream().anyMatch(c -> c.code().equals("DRAIN")));
        assertThrows(BadRequest.class, () -> codes.select("BATHROOM", List.of(), "SEOUL_GANGNAM"));   // 공간은 가격 비교 대상 아님
        int all = prices.count(codes.leaves("BATHROOM", true), null);
        assertTrue(all >= prices.count(List.of(codes.find("DRAIN")), null));
    }

    @Test
    void selectionNeedsExactlyOneOptionPerGroup() {
        assertThrows(BadRequest.class, () -> codes.select("WALLPAPER", List.of("WALLPAPER_AREA_20"), "SEOUL_GANGNAM"));
        assertThrows(BadRequest.class, () -> codes.select("WALLPAPER",
                List.of("WALLPAPER_AREA_20", "WALLPAPER_AREA_30", "WALLPAPER_TYPE_SILK", "WALLPAPER_CEIL_NO"), "SEOUL_GANGNAM"));
        assertThrows(BadRequest.class, () -> codes.select("WALLPAPER",
                List.of("WALLPAPER_AREA_20", "WALLPAPER_TYPE_SILK", "FLOOR_MOVE_NO"), "SEOUL_GANGNAM"));
        assertThrows(BadRequest.class, () -> codes.select("WALLPAPER",
                List.of("WALLPAPER_AREA_20", "WALLPAPER_TYPE_SILK", "WALLPAPER_CEIL_NO"), "SEOUL"));   // 시/도만은 안 됨
    }
}
