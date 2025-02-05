package itstep.learning.dal.dao;

public class UserRole {
    private String id; // Ідентифікатор ролі (наприклад, admin, guest)
    private String description; // Опис ролі
    private boolean canCreate; // Дозвіл на створення
    private boolean canRead; // Дозвіл на читання
    private boolean canUpdate; // Дозвіл на оновлення
    private boolean canDelete; // Дозвіл на видалення

    // Конструктор
    public UserRole(String id, String description, boolean canCreate, boolean canRead, boolean canUpdate, boolean canDelete) {
        this.id = id;
        this.description = description;
        this.canCreate = canCreate;
        this.canRead = canRead;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

    // Гетери та сетери
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCanCreate() {
        return canCreate;
    }

    public void setCanCreate(boolean canCreate) {
        this.canCreate = canCreate;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public void setCanRead(boolean canRead) {
        this.canRead = canRead;
    }

    public boolean isCanUpdate() {
        return canUpdate;
    }

    public void setCanUpdate(boolean canUpdate) {
        this.canUpdate = canUpdate;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }
}
