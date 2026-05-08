package ayd2.p2b.conference_service_api;

import org.springframework.boot.SpringApplication;

public class TestConferenceServiceApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(ConferenceServiceApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
