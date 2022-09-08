package com.fajurion.learn.repository.tests.topics;

import com.fajurion.learn.repository.topic.Topic;

public class ETopicResponse {

    public int id, topic;
    public String name;
    public boolean category, locked;

    public ETopicResponse(Topic topic, int id) {
        this.id = id;
        this.topic = topic.getId();
        this.name = topic.getName();
        this.locked = topic.isLocked();
        this.category = topic.isCategory();
    }

}
