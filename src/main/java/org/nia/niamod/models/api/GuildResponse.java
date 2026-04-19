package org.nia.niamod.models.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public record GuildResponse(Members members) {

    public List<String> allUsernames() {
        if (members == null) {
            return List.of();
        }
        return Stream.of(members.owner, members.chief, members.strategist, members.captain, members.recruiter, members.recruit)
                .filter(Objects::nonNull)
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .toList();
    }

    public record Members(Map<String, Member> owner, Map<String, Member> chief, Map<String, Member> strategist,
                          Map<String, Member> captain, Map<String, Member> recruiter, Map<String, Member> recruit) {
    }

    public record Member() {
    }
}
