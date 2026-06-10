package de.dema.user;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository sut;

    @ParameterizedTest
    @ValueSource(strings = {"Dennis", "Ronny", "Marc"})
    void findByName(String username) {
        // when
        var result = sut.findByName(username);

        // then
        assertThat(result).isNotEmpty();

        assertThat(result.get().getName()).isEqualTo(username);
    }
}