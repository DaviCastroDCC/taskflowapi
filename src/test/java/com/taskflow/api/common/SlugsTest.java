package com.taskflow.api.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugsTest {

    @Test
    void lowercasesAndReplacesSpaces() {
        assertThat(Slugs.slugify("Acme Rockets")).isEqualTo("acme-rockets");
    }

    @Test
    void collapsesRunsOfSymbolsIntoASingleDash() {
        assertThat(Slugs.slugify("  Team:: Alpha // 2026 !!")).isEqualTo("team-alpha-2026");
    }

    @Test
    void stripsAccents() {
        assertThat(Slugs.slugify("Equipe Inovação")).isEqualTo("equipe-inovacao");
    }

    @Test
    void fallsBackWhenNothingUsableRemains() {
        assertThat(Slugs.slugify("日本語")).isEqualTo("workspace");
        assertThat(Slugs.slugify("!!!")).isEqualTo("workspace");
    }

    @Test
    void trimsToMaxLengthWithoutLeavingATrailingDash() {
        String slug = Slugs.slugify("a".repeat(40) + " " + "b".repeat(40));
        assertThat(slug).hasSizeLessThanOrEqualTo(50).doesNotEndWith("-");
    }
}
