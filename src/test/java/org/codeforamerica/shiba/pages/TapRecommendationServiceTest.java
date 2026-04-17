package org.codeforamerica.shiba.pages;

import static org.assertj.core.api.Assertions.assertThat;


import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class TapRecommendationServiceTest {

    private TapRecommendationService tapRecommendationService;

    @BeforeEach
    void setUp() {
        tapRecommendationService = new TapRecommendationService();
    }

    @Test
    void shouldShowTapMessage_whenCountyIsHennepin() {
        ApplicationData applicationData = new ApplicationData();
        applicationData.setOriginalCounty("Hennepin");
        
        assertThat(tapRecommendationService.showTapMessage(applicationData)).isTrue();
    }

    @Test
    void shouldNotShowTapMessage_whenCountyIsNotInPilotList() {
        ApplicationData applicationData = new ApplicationData();
        applicationData.setOriginalCounty("Aitkin");
        
        assertThat(tapRecommendationService.showTapMessage(applicationData)).isFalse();
    }

    @Test
    void shouldShowTapMessage_whenCountyIsAnoka() {
        ApplicationData applicationData = new ApplicationData();
        applicationData.setOriginalCounty("Anoka");
        
        assertThat(tapRecommendationService.showTapMessage(applicationData)).isTrue();
    }
}