package com.terraguard.backend.websocket;

import com.terraguard.backend.config.HighlightConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeywordMatcherServiceTest {

    private KeywordMatcherService matcher;

    @BeforeEach
    void setup() {
        HighlightConfig config = new HighlightConfig();
        config.setKeywords(List.of(
                "collapsed", "trapped", "casualties",
                "evacuate", "flooding", "blocked"
        ));
        matcher = new KeywordMatcherService(config);
    }

    @Test
    void exactKeyword_shouldMatch() {
        assertTrue(matcher.isHighlightWorthy("bridge collapsed near highway"));
    }

    @Test
    void uppercaseMessage_shouldMatch() {
        assertTrue(matcher.isHighlightWorthy("BRIDGE COLLAPSED NEAR HIGHWAY"));
    }

    @Test
    void mixedCaseMessage_shouldMatch() {
        assertTrue(matcher.isHighlightWorthy("Bridge Collapsed Near Highway"));
    }

    @Test
    void keywordInMiddleOfSentence_shouldMatch() {
        assertTrue(matcher.isHighlightWorthy("people are trapped inside the building"));
    }

    @Test
    void multipleKeywords_shouldMatchOnFirst() {
        assertTrue(matcher.isHighlightWorthy("flooding reported, road also blocked"));
    }

    @Test
    void noKeywordPresent_shouldNotMatch() {
        assertFalse(matcher.isHighlightWorthy("stay safe everyone, prayers for victims"));
    }

    @Test
    void allClearMessage_shouldNotMatch() {
        assertFalse(matcher.isHighlightWorthy("all clear, situation is under control"));
    }

    @Test
    void nullMessage_shouldNotMatch() {
        assertFalse(matcher.isHighlightWorthy(null));
    }

    @Test
    void blankMessage_shouldNotMatch() {
        assertFalse(matcher.isHighlightWorthy("   "));
    }

    @Test
    void emptyKeywordList_shouldNeverMatch() {
        HighlightConfig emptyConfig = new HighlightConfig();
        emptyConfig.setKeywords(List.of());
        KeywordMatcherService emptyMatcher = new KeywordMatcherService(emptyConfig);
        assertFalse(emptyMatcher.isHighlightWorthy("bridge collapsed"));
    }
}