package pro.gravit.simplecabinet.web.dto.user;

import pro.gravit.simplecabinet.web.model.user.UserGroup;

public class UserGroupDto {
    public final long id;
    public final String groupName;

    public UserGroupDto(UserGroup entity) {
        this.id = entity.getId();
        this.groupName = entity.getGroup().getId();
    }
}
