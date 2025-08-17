package com.liberarium.api.support.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

class TestContainerTest {

  @Test
  void 도커_접속_확인_테스트() {
    DockerClientFactory.instance().client();
  }
}
