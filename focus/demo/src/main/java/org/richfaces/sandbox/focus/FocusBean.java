package org.richfaces.sandbox.focus;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ViewScoped
@ManagedBean
public class FocusBean implements Serializable {
// ------------------------------ FIELDS ------------------------------

    private String email;

    private String username;

    private List<User> users = new ArrayList<User>();

    public FocusBean() {
        users.addAll(Arrays.asList(new User("Jack"), new User("Don")));
    }

    public List<User> getUsers() {
        return users;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
