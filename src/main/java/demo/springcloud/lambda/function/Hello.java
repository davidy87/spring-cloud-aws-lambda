package demo.springcloud.lambda.function;

import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class Hello implements Function<String, String> {

    @Override
    public String apply(String s) {
        return "Hello, " + s + "!";
    }
}
