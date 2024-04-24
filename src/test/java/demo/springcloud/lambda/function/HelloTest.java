package demo.springcloud.lambda.function;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

@FunctionalSpringBootTest(webEnvironment = RANDOM_PORT)
class HelloTest {

    @Autowired
    private TestRestTemplate template;

    @Test
    void hello() throws Exception {
        // given
        String name = "David";

        // when
        ResponseEntity<String> result = template.exchange(
                RequestEntity.post(new URI("/hello")).body(name), String.class);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Hello, " + name + "!");
    }
}