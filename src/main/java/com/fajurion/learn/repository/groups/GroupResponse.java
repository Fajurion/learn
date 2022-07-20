package com.fajurion.learn.repository.groups;

import lombok.Data;

@Data
public class GroupResponse {

    private final String name;
    private final int id, memberCount;

    public GroupResponse(Group group, int memberCount) {
        this.name = group.getName();
        this.id = group.getId();
        this.memberCount = memberCount;
    }

}
