package com.softcode.nextstep;

import org.junit.jupiter.api.Test;
import com.softcode.nextstep.config.GeminiTestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(GeminiTestConfig.class)
class NextstepApplicationTests {

	@Test
	void contextLoads() {
	}

}
