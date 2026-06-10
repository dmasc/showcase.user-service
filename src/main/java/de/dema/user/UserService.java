package de.dema.user;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public int count() {
        return (int) repository.count();
    }

    public UserEntity getUser(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("User with ID " + id + " not found"));
    }

    public UserEntity getUser(String name) {
        return repository.findByName(name).orElseThrow(() -> new EntityNotFoundException("User '" + name + "' not found"));
    }

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            UserEntity entity = getUser(username);
            return new User(entity.getName(), entity.getPassword(), Collections.singletonList(new SimpleGrantedAuthority("ADMIN")));
        } catch (EntityNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage(), e);
        }
    }
}
