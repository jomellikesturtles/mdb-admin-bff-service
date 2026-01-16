package com.mdb.adminbff.controller;

import com.mdb.adminbff.dto.BanUserRequest;
import com.mdb.adminbff.dto.PagedResponse;
import com.mdb.adminbff.dto.User;
import com.mdb.adminbff.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User Management")
public class UserController {

    private final UserService userService;

    @Operation(summary = "List all users (Paged)")
    @GetMapping
    public ResponseEntity<PagedResponse<User>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(userService.getUsers(page, limit, search));
    }

    @Operation(summary = "Get user details")
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable UUID id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Ban or Unban a User")
    @PatchMapping("/{id}")
    public ResponseEntity<Void> banUser(@PathVariable UUID id, @RequestBody BanUserRequest request) {
        userService.banUser(id, request.getStatus(), request.getReason());
        return ResponseEntity.ok().build();
    }
}
