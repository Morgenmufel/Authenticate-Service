package renatius.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import renatius.authenticationservice.dto.UserDto;
import renatius.authenticationservice.service.UserService;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public boolean createUser(@RequestBody UserDto userDto) {
        return userService.addUser(userDto);
    }
}
