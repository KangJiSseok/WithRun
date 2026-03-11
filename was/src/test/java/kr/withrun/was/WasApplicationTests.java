package kr.withrun.was;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false",
                "jdbc.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "jdbc.username=sa",
                "jdbc.password="
        }
)
@DisplayName("WAS 애플리케이션")
class WasApplicationTests {

    @DisplayName("스프링 애플리케이션 컨텍스트를 로드한다")
    @Test
    void contextLoads() {
    }

}
