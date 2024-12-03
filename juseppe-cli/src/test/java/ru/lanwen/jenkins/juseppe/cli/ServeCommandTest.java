package ru.lanwen.jenkins.juseppe.cli;


import org.junit.ClassRule;
import org.junit.Test;

import static io.restassured.RestAssured.given;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class ServeCommandTest {

    @ClassRule
    public static ServeRule srv = new ServeRule();

    @Test(timeout = 10000)
    public void shouldFetchUpdateCenter() throws Exception {
        given()
                .baseUri(srv.uri().toString())
                .log().all()
                .expect()
                .log().status()
                .when().get("update-center.json").then().assertThat().statusCode(200);
    }

    @Test(timeout = 10000)
    public void shouldFetchHpi() throws Exception {
        given()
                .baseUri(srv.uri().toString())
                .log().all()
                .expect()
                .log().status()
                .when().get("clang-scanbuild-plugin.hpi").then().assertThat().statusCode(200);
    }

    @Test(timeout = 10000)
    public void shouldFetchJpi() throws Exception {
        given()
                .baseUri(srv.uri().toString())
                .log().all()
                .expect()
                .log().status()
                .when().get("sample-pipeline-dsl-ext-plugin-0.1.0.jpi").then().assertThat().statusCode(200);
    }
}