package de.dema.user;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserServiceTest {
    private final UserRepository userRepository = mock();

    private final UserService sut = new UserService(userRepository);

    @Test
    void count() {
        // given
        var count = 134679L;

        given(userRepository.count())
                .willReturn(count);

        // when
        int result = sut.count();

        // then
        assertThat(result).isEqualTo(count);
        verify(userRepository).count();
    }

    @Test
    void getUserById() {
        // given
        var id = 123L;
        UserEntity entity = mock();

        given(userRepository.findById(id))
                .willReturn(Optional.of(entity));

        // when
        UserEntity result = sut.getUser(id);

        // then
        assertThat(result).isSameAs(entity);

        verify(userRepository).findById(id);
    }

    @Test
    void getUserByIdThrowsNotFound() {
        // given
        var id = 123L;

        given(userRepository.findById(id))
                .willReturn(Optional.empty());

        // when, then
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> sut.getUser(id))
                .withMessageContaining(" " + id + " ");

        verify(userRepository).findById(id);
    }

    @Test
    void getUserByName() {
        // given
        var name = "test";
        UserEntity entity = mock();

        given(userRepository.findByName(name))
                .willReturn(Optional.of(entity));

        // when
        UserEntity result = sut.getUser(name);

        // then
        assertThat(result).isSameAs(entity);

        verify(userRepository).findByName(name);
    }

    @Test
    void getUserByNameThrowsNotFound() {
        // given
        var name = "test";

        given(userRepository.findByName(name))
                .willReturn(Optional.empty());

        // when, then
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> sut.getUser(name))
                .withMessageContaining(" '" + name + "' ");

        verify(userRepository).findByName(name);
    }

    @Test
    void loadUserByUsername() {
        // given
        var name = "test";
        var entity = new UserEntity(123L, "test name", "test pw", 777);

        given(userRepository.findByName(name))
                .willReturn(Optional.of(entity));

        // when
        User result = sut.loadUserByUsername(name);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(entity.getName());
        assertThat(result.getPassword()).isEqualTo(entity.getPassword());
        assertThat(result.getAuthorities()).containsExactly(new SimpleGrantedAuthority("ADMIN"));

        verify(userRepository).findByName(name);
    }

    @Test
    void loadUserByUsernameThrowsNotFound() {
        // given
        var name = "test";

        given(userRepository.findByName(name))
                .willReturn(Optional.empty());

        // when, then
        assertThatExceptionOfType(UsernameNotFoundException.class)
                .isThrownBy(() -> sut.loadUserByUsername(name))
                .withMessageContaining(" '" + name + "' ");

        verify(userRepository).findByName(name);
    }
}