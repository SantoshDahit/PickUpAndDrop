package com.landgreet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.landgreet.storage.ObjectStorage;
import com.landgreet.user.User;
import com.landgreet.user.UserRepository;
import com.landgreet.user.UserService;
import jakarta.servlet.http.Cookie;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountFlowIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectStorage storage;

    private static byte[] tinyJpeg() throws Exception {
        var image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, 640, 480);
        g.dispose();
        var out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    /** Spring Session stores auth server-side; the SESSION cookie is the handle. */
    private Cookie loginAs(String email, String password) throws Exception {
        Cookie cookie = mvc.perform(formLogin().user(email).password(password))
                .andExpect(authenticated())
                .andReturn().getResponse().getCookie("SESSION");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    @Test
    void signupUploadAvatarEditProfileAndDeleteAccount() throws Exception {
        // Signup through the real form; auto-login redirects to /trips.
        mvc.perform(post("/signup").with(csrf())
                        .param("name", "Flow Tester")
                        .param("email", "Flow@Example.com")
                        .param("phone", "+82 10-0000-0000")
                        .param("password", "secret1"))
                .andExpect(redirectedUrl("/trips"));

        User user = userRepository.findByEmailAndDeletedAtIsNull("flow@example.com").orElseThrow();
        var cookie = loginAs("flow@example.com", "secret1");

        // Upload a real JPEG: stored as a processed avatar object.
        mvc.perform(multipart("/account/avatar")
                        .file(new MockMultipartFile("file", "photo.jpg", "image/jpeg", tinyJpeg()))
                        .cookie(cookie).with(csrf()))
                .andExpect(redirectedUrl("/account"))
                .andExpect(flash().attribute("ok", "Photo updated."));

        String firstKey = userRepository.findById(user.getId()).orElseThrow().getAvatarKey();
        assertThat(firstKey).matches("avatars/[0-9a-f\\-]{36}\\.jpg");
        assertThat(storage.get(firstKey)).isPresent();

        // Replacing mints a new key and deletes the old object.
        mvc.perform(multipart("/account/avatar")
                        .file(new MockMultipartFile("file", "photo2.jpg", "image/jpeg", tinyJpeg()))
                        .cookie(cookie).with(csrf()))
                .andExpect(redirectedUrl("/account"));
        String secondKey = userRepository.findById(user.getId()).orElseThrow().getAvatarKey();
        assertThat(secondKey).isNotEqualTo(firstKey);
        assertThat(storage.get(firstKey)).isEmpty();
        assertThat(storage.get(secondKey)).isPresent();

        // Garbage that claims to be a JPEG is rejected, profile untouched.
        mvc.perform(multipart("/account/avatar")
                        .file(new MockMultipartFile("file", "evil.jpg", "image/jpeg", "not an image".getBytes()))
                        .cookie(cookie).with(csrf()))
                .andExpect(redirectedUrl("/account"))
                .andExpect(flash().attributeExists("error"));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getAvatarKey()).isEqualTo(secondKey);

        // Profile edit persists.
        mvc.perform(post("/account/profile").cookie(cookie).with(csrf())
                        .param("name", "Flow Edited")
                        .param("email", "flow@example.com")
                        .param("phone", ""))
                .andExpect(redirectedUrl("/account"));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getName()).isEqualTo("Flow Edited");

        // Delete with wrong password refused; with the right one the account
        // is soft-deleted, avatar object removed, login blocked, email freed.
        mvc.perform(post("/account/delete").cookie(cookie).with(csrf()).param("password", "wrong"))
                .andExpect(redirectedUrl("/account"))
                .andExpect(flash().attributeExists("error"));

        mvc.perform(post("/account/delete").cookie(cookie).with(csrf()).param("password", "secret1"))
                .andExpect(redirectedUrl("/"));

        User deleted = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getEmail()).isEqualTo("deleted:" + user.getId() + ":flow@example.com");
        assertThat(storage.get(secondKey)).isEmpty();

        mvc.perform(formLogin().user("flow@example.com").password("secret1"))
                .andExpect(unauthenticated());

        userService.register("Flow Again", "flow@example.com", null, "secret2");
    }
}
