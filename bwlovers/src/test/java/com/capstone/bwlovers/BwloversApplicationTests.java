package com.capstone.bwlovers;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(classes = BwloversApplication.class)
class BwloversApplicationTests {

    @Test
    void contextLoads() {
    }

}
