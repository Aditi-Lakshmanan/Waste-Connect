package com.wasteconnect.service;

import com.wasteconnect.dao.CitizenDao;
import com.wasteconnect.dao.CitizenDaoImpl;
import com.wasteconnect.dao.UserDao;
import com.wasteconnect.dao.UserDaoImpl;
import com.wasteconnect.dao.WastePickerDao;
import com.wasteconnect.dao.WastePickerDaoImpl;
import com.wasteconnect.exception.AuthenticationException;
import com.wasteconnect.exception.DuplicateEntryException;
import com.wasteconnect.exception.ValidationException;
import com.wasteconnect.model.Citizen;
import com.wasteconnect.model.User;
import com.wasteconnect.model.UserRole;
import com.wasteconnect.model.WastePicker;
import com.wasteconnect.pattern.UserFactory;
import com.wasteconnect.util.AppLogger;
import com.wasteconnect.util.PasswordHasher;
import com.wasteconnect.util.Validators;

import java.util.Optional;

/**
 * Service managing user authentication, registration, session state, and password security.
 */
public class AuthService {
    private final UserDao userDao;
    private final CitizenDao citizenDao;
    private final WastePickerDao wastePickerDao;
    private static User currentUser;

    /**
     * Ensures that the current logged‑in user is an admin. Throws {@link AuthorizationException}
     * if the check fails. This protects admin‑only operations such as creating new users.
     */
    private void ensureAdmin() {
        User cur = AuthService.getCurrentUser();
        if (cur == null || cur.getRole() != UserRole.ADMIN) {
            throw new com.wasteconnect.exception.AuthorizationException("Only administrators can perform this operation.");
        }
    }

    public AuthService() {
        this(new UserDaoImpl(), new CitizenDaoImpl(), new WastePickerDaoImpl());
    }

    public AuthService(UserDao userDao, CitizenDao citizenDao, WastePickerDao wastePickerDao) {
        this.userDao = userDao;
        this.citizenDao = citizenDao;
        this.wastePickerDao = wastePickerDao;
    }

    /**
     * Authenticates a user using email and password.
     */
    public User login(String email, String password) {
        return login(email, password, null);
    }

    /**
     * Authenticates a user and optionally verifies the selected sign-in role.
     */
    public User login(String email, String password, UserRole expectedRole) {
        Validators.validateEmail(email);
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password cannot be empty.");
        }

        Optional<User> userOpt = userDao.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Invalid email or password.");
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            throw new AuthenticationException("This account has been deactivated. Please contact municipal support.");
        }

        if (!PasswordHasher.verifyPassword(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        if (expectedRole != null && user.getRole() != expectedRole) {
            throw new AuthenticationException("This account is registered as "
                    + user.getRole().getDisplayName() + ". Select that login type and try again.");
        }

        currentUser = user;
        AppLogger.info("User logged in successfully: " + email + " (" + user.getRole() + ")");
        return user;
    }

    /**
     * Registers a new Citizen resident.
     */
    public Citizen registerCitizen(String name, String email, String password, String address, String phone, String zoneId) {
        ensureAdmin();
        Validators.validateNonBlank(name, "Full Name");
        Validators.validateEmail(email);
        Validators.validatePasswordStrength(password);
        Validators.validateNonBlank(address, "Residential Address");
        Validators.validatePhone(phone);

        if (userDao.findByEmail(email).isPresent()) {
            throw new DuplicateEntryException("An account already exists with email: " + email);
        }

        String passwordHash = PasswordHasher.hashPassword(password);
        Citizen citizen = UserFactory.createCitizen(name, email, passwordHash, address, phone, zoneId != null ? zoneId : "Z1");
        citizenDao.save(citizen);
        AppLogger.info("Registered new citizen: " + email);
        return citizen;
    }

    /**
     * Registers a new Waste Picker informal recycling partner.
     */
    public WastePicker registerWastePicker(String name, String email, String password, String phone, String vehicleDesc) {
        ensureAdmin();
        Validators.validateNonBlank(name, "Full Name");
        Validators.validateEmail(email);
        Validators.validatePasswordStrength(password);
        Validators.validatePhone(phone);

        if (userDao.findByEmail(email).isPresent()) {
            throw new DuplicateEntryException("An account already exists with email: " + email);
        }

        String passwordHash = PasswordHasher.hashPassword(password);
        WastePicker picker = UserFactory.createWastePicker(name, email, passwordHash, phone, vehicleDesc != null ? vehicleDesc : "Cargo Tricycle");
        wastePickerDao.save(picker);
        AppLogger.info("Registered new waste picker: " + email);
        return picker;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void logout() {
        if (currentUser != null) {
            AppLogger.info("User logged out: " + currentUser.getEmail());
            currentUser = null;
        }
    }
}
