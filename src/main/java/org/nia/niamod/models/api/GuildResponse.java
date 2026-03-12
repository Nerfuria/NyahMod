package org.nia.niamod.models.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public record GuildResponse(Members members) {

    public List<String> allUsernames() {
        return Stream.of(members.owner.keySet(), members.chief.keySet(), members.strategist.keySet(), members.captain.keySet(), members.recruiter.keySet(), members.recruit.keySet()).filter(Objects::nonNull).flatMap(Collection::stream).toList();
    }

    public record Members(Map<String, Member> owner, Map<String, Member> chief, Map<String, Member> strategist, Map<String, Member> captain, Map<String, Member> recruiter, Map<String, Member> recruit) { }

    public record Member() { }
}