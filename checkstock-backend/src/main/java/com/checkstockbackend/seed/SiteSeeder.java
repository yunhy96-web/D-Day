package com.checkstockbackend.seed;

import com.checkstockbackend.entity.MonitoredSite;
import com.checkstockbackend.repository.MonitoredSiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SiteSeeder implements CommandLineRunner {

    private final MonitoredSiteRepository siteRepository;

    private static final List<String> DEFAULT_INCLUDE = List.of("빈티지");
    private static final List<String> DEFAULT_EXCLUDE = List.of("에이버리 진");

    @Override
    public void run(String... args) {
        if (siteRepository.count() > 0) {
            backfillMissingKeywords();
            return;
        }

        MonitoredSite rrl = new MonitoredSite();
        rrl.setName("RRL Double RL 데님");
        rrl.setUrl("https://www.ralphlauren.co.kr/men/brands/double-rl?prefn1=CategoryCode&prefv1=%EB%8D%B0%EB%8B%98");
        rrl.setBaseUrl("https://www.ralphlauren.co.kr");
        rrl.setListSelector("a.name-link.js-pdp-link");
        rrl.setRefreshIntervalSec(300);
        rrl.setActive(true);
        rrl.setIncludeKeywords(new ArrayList<>(DEFAULT_INCLUDE));
        rrl.setExcludeKeywords(new ArrayList<>(DEFAULT_EXCLUDE));
        siteRepository.save(rrl);

        log.info("RRL 사이트 시드 완료: id={}", rrl.getId());
    }

    private void backfillMissingKeywords() {
        List<MonitoredSite> sites = siteRepository.findAll();
        boolean dirty = false;
        for (MonitoredSite s : sites) {
            if (s.getIncludeKeywords() == null || s.getIncludeKeywords().isEmpty()) {
                s.setIncludeKeywords(new ArrayList<>(DEFAULT_INCLUDE));
                dirty = true;
            }
            if (s.getExcludeKeywords() == null || s.getExcludeKeywords().isEmpty()) {
                s.setExcludeKeywords(new ArrayList<>(DEFAULT_EXCLUDE));
                dirty = true;
            }
        }
        if (dirty) {
            siteRepository.saveAll(sites);
            log.info("기존 사이트에 기본 키워드 백필 완료");
        } else {
            log.info("이미 모니터링 사이트가 등록되어 있어 시드 skip");
        }
    }
}
