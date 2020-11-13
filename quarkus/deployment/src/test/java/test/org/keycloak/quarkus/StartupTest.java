package test.org.keycloak.quarkus;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StartupTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application.properties", "application.properties")
                    .addAsResource("keycloak.properties", "META-INF/keycloak.properties"));

    @Test
    public void testWelcomePage() throws InterruptedException {
        RestAssured.given()
                .when().get("/")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("Please create an initial admin user to get started"));
    }
}
