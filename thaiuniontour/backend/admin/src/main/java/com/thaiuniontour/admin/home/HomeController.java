package com.thaiuniontour.admin.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 운영 홈(부팅 확인용 플레이스홀더). 이후 상품/예약/카테고리 화면으로 확장. */
@Controller
public class HomeController {

    @GetMapping({"/", "/admin"})
    public String index(Model model) {
        model.addAttribute("appName", "thaiuniontour admin");
        return "index";
    }
}
