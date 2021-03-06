package com.chat.app.services;

import com.chat.app.exceptions.PasswordsMissMatchException;
import com.chat.app.models.specs.NewPasswordSpec;
import com.chat.app.models.specs.RegisterSpec;
import com.chat.app.repositories.base.UserRepository;
import com.chat.app.exceptions.UsernameExistsException;
import com.chat.app.models.UserDetails;
import com.chat.app.models.UserModel;
import com.chat.app.models.specs.UserSpec;
import com.chat.app.services.base.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService,UserDetailsService {

    private final UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserModel> findAll() {
        return userRepository.findAll();
    }

    @Override
    public UserModel findById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User doesn't exist."));
    }
    @Override
    public List<UserModel> findByUsernameWithRegex(String username){
        return userRepository.findByUsernameWithRegex(username);
    }

    @Override
    public UserModel create(UserModel user) {
        UserModel existingUser = userRepository.findByUsername(user.getUsername());

        if (existingUser != null) {
            throw new UsernameExistsException("Username is already taken.");
        }

        user.setPassword(BCrypt.hashpw(user.getPassword(),BCrypt.gensalt(4)));
        return userRepository.save(user);
    }

    @Override
    public void delete(long id, UserDetails loggedUser) {
        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        if(id != loggedUser.getId() &&
                !AuthorityUtils.authorityListToSet(loggedUser.getAuthorities()).contains("ROLE_ADMIN")){
            throw new UserProfileUnavailableException("You are not allowed to modify the user.");
        }

        userRepository.delete(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserModel userModel = userRepository.findByUsername(username);
        if(userModel == null){
            throw new BadCredentialsException("Bad credentials");
        }
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(userModel.getRole()));

        return new UserDetails(userModel,authorities);
    }

    @Override
    public UserModel changeUserInfo(int loggedUser, UserSpec userSpec){
        UserModel user = userRepository.findById(loggedUser)
                .orElseThrow(() -> new EntityNotFoundException("Username not found."));
        user.setFirstName(userSpec.getFirstName());
        user.setLastName(userSpec.getLastName());
        user.setAge(userSpec.getAge());
        user.setCountry(userSpec.getCountry());

        return userRepository.save(user);
    }

    @Override
    public UserModel changePassword(NewPasswordSpec passwordSpec){
        UserModel user = userRepository.findByUsername(passwordSpec.getUsername());

        if(user == null){
            throw new EntityNotFoundException("User with" + passwordSpec.getUsername() + "is not found.");
        }

        if (!user.getPassword().equals(passwordSpec.getCurrentPassword())){
            throw new BadCredentialsException("Invalid current password.");
        }
        user.setPassword(passwordSpec.getNewPassword());
        return userRepository.save(user);

    }
}
