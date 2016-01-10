package net.aabbcc1241.Peer_Alert.utils;

/**
 * Created by beenotung on 1/10/16.
 */

public class LongUIDGenerator extends UIDGenerator<Long> {
    long lastId = 0;

    @Override
    Long generateId() {
        return ++lastId;
    }
}
