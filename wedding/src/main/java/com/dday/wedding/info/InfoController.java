package com.dday.wedding.info;

import com.dday.wedding.common.BadRequest;
import com.dday.wedding.common.Valid;
import com.dday.wedding.config.WeddingProps;
import com.dday.wedding.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 계좌·상세주소는 HTML 에 찍지 않고 복사 버튼 클릭 시에만 (설계 §13-2). */
@RestController
@RequiredArgsConstructor
public class InfoController {
    private final WeddingProps props;
    private final RateLimiter limiter;

    @GetMapping("/api/account")
    public Map<String, String> account(@RequestParam String side, HttpServletRequest req) {
        limiter.check(req, "acct", 10, 60);
        WeddingProps.Account a = props.getAccount().get(side);
        if (a == null) throw new BadRequest("side");
        return Map.of("holder", a.getHolder(), "bank", a.getBank(), "number", a.getNumber());
    }

    @GetMapping("/api/address")
    public Map<String, String> address(@RequestParam(defaultValue = "ko") String lang, HttpServletRequest req) {
        limiter.check(req, "addr", 10, 60);
        String l = Valid.lang(lang);
        return Map.of("address", props.getAddress().getOrDefault(l, props.getAddress().getOrDefault("ko", "")));
    }
}
