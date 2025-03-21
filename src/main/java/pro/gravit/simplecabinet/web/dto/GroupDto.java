package pro.gravit.simplecabinet.web.dto;

import pro.gravit.simplecabinet.web.model.user.Group;

public class GroupDto {
    public final String id;
    public final String displayName;
    public final String parentId;

    public GroupDto(String id, String displayName, String parentId) {
        this.id = id;
        this.displayName = displayName;
        this.parentId = parentId;
    }

    public GroupDto(Group group) {
        this.id = group.getId();
        this.displayName = group.getDisplayName();
        this.parentId = group.getParent().getId();
    }
}
