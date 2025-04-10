package com.chancetop.naixt.agent.utils;

import org.junit.jupiter.api.Test;

/**
 * @author stephen
 */
class IdeUtilsTest {
    @Test
    void test() {
        var rst = IdeUtils.searchFileByName(".", ".*Test.*", true, true);
        assert !rst.isEmpty();
    }
}