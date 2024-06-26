package book.store.service.user;

import book.store.dto.user.UserAdminResponseDto;
import book.store.dto.user.UserRegistrationRequestDto;
import book.store.dto.user.UserResponseDto;
import book.store.dto.user.UserSearchParametersDto;
import book.store.dto.user.UserUpdateRequestDto;
import book.store.exception.RegistrationException;
import book.store.mapper.UserMapper;
import book.store.model.Role;
import book.store.model.ShoppingCart;
import book.store.model.User;
import book.store.repository.ShoppingCartRepository;
import book.store.repository.UserRepository;
import book.store.repository.specification.SpecificationBuilder;
import book.store.telegram.strategy.notification.AdminNotificationStrategy;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final String USER_UPDATING = "Role updating";
    private static final String USER_DELETING = "User deleting";
    private static final String TELEGRAM = "Telegram";
    private static final int TWO = 2;
    private static final int ONE = 1;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SpecificationBuilder<User, UserSearchParametersDto> specificationBuilder;
    private final AdminNotificationStrategy<User> notificationStrategy;
    private final ShoppingCartRepository shoppingCartRepository;

    @Override
    public UserResponseDto register(UserRegistrationRequestDto requestDto)
            throws RegistrationException {
        if (userRepository.findByEmail(requestDto.email()).isPresent()) {
            throw new RegistrationException("""
                    Can't register a new user
                    Passed email already exists
                    Try another one
                    """);
        }
        User user = userMapper.toModel(requestDto);
        ShoppingCart shoppingCart = new ShoppingCart();
        user.setPassword(passwordEncoder.encode(requestDto.password()));
        shoppingCart.setUser(user);
        shoppingCartRepository.save(shoppingCart);
        return userMapper.toResponseDto(user);
    }

    @Override
    public List<UserAdminResponseDto> getAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .stream()
                .map(userMapper::toAdminResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserAdminResponseDto changeUserRole(Long id, String roleName) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException(
                        "Can't find a user by id " + id));
        Role.RoleName role = Role.RoleName.fromString(roleName);
        if (userIs(user, Role.RoleName.ROLE_USER) && role.equals(Role.RoleName.ROLE_USER)) {
            return userMapper.toAdminResponseDto(user);
        }
        if (userIs(user, Role.RoleName.ROLE_ADMIN) && role.equals(Role.RoleName.ROLE_ADMIN)) {
            return userMapper.toAdminResponseDto(user);
        }
        if (userIs(user, Role.RoleName.ROLE_USER) && role.equals(Role.RoleName.ROLE_ADMIN)) {
            user.getRoles().add(new Role(2L));
        } else {
            Set<Role> roles = new HashSet<>();
            roles.add(new Role(1L));
            user.setRoles(roles);
        }
        userRepository.save(user);
        sendMessage(TELEGRAM, USER_UPDATING, null, user);
        return userMapper.toAdminResponseDto(user);
    }

    @Override
    public UserResponseDto getMyInfo(User user) {
        return userMapper.toResponseDto(user);
    }

    @Override
    public UserResponseDto updateMyInfo(User user, UserUpdateRequestDto requestDto) {
        if (requestDto.email() != null
                && userRepository.findByEmail(requestDto.email()).isPresent()) {
            throw new IllegalArgumentException("""
                    Can't update email
                    This one is already taken
                    Try another one
                    """);
        }
        if (requestDto.password() != null) {
            String passwordEncoded = passwordEncoder.encode(requestDto.password());
            user.setPassword(passwordEncoded);
        }
        userMapper.toModel(user, requestDto);
        userRepository.save(user);
        return userMapper.toResponseDto(user);
    }

    @Override
    public List<UserAdminResponseDto> search(
            UserSearchParametersDto parametersDto,
            Pageable pageable) {
        Specification<User> userSpecification = specificationBuilder.build(parametersDto);
        return userRepository.findAll(userSpecification, pageable)
                .stream()
                .map(userMapper::toAdminResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (userRepository.findByIdWithoutRole(id).isEmpty()) {
            return;
        }
        userRepository.deleteById(id);
        shoppingCartRepository.deleteById(id);
        sendMessage(TELEGRAM, USER_DELETING, null, new User(id));
    }

    private boolean userIs(User user, Role.RoleName roleName) {
        if (roleName.equals(Role.RoleName.ROLE_USER)) {
            return user.getRoles().size() == ONE;
        }
        return user.getRoles().size() == TWO;
    }

    private void sendMessage(
            String notificationsService,
            String messageType,
            Long chatId,
            User user) {
        notificationStrategy
                .getNotificationService(
                        notificationsService, messageType
                )
                .sendMessage(
                        chatId, user);
    }
}
