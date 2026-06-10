package de.dema.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/count")
    public String getUserCount() {
        return String.valueOf(userService.count());
    }

    @GetMapping
    public UserEntity getUser(@RequestParam("id") Long id) {
        return userService.getUser(id);
    }
}
