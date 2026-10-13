package be.npl.shelfwise.security;

public enum Role {
    MEMBER,
    LIBRARIAN,
    BRANCH_MANAGER,
    ADMINISTRATOR;

    public boolean isStaff() {
        return this != MEMBER;
    }
}
