package by.piskunou.teletronics.config;

import by.piskunou.teletronics.core.domain.User;
import by.piskunou.teletronics.infra.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class UserInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String username = request.getHeader("X-User-Id");

        if (username != null && !username.isBlank()) {
            User user = userRepository.findByUsername(username)
                    .orElseGet(() -> userRepository.save(new User(username)));
            request.setAttribute("user", user);
        }

        return true;
    }
}
