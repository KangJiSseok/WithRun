package kr.withrun.was;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@DisplayName("WAS 애플리케이션")
class WasApplicationTests {

    @DisplayName("스프링 애플리케이션 컨텍스트를 로드한다")
    @Test
    void contextLoads() {
    }

}
