package net.aabbcc1241.Peer_Alert.utils;

import java.util.HashSet;

/**
 * Created by beenotung on 1/10/16.
 */
public abstract class UIDGenerator<T> {
    private HashSet<T> hashSet = new HashSet<>();

    boolean exist(T id) {
        return hashSet.contains(id);
    }

    T newId() {
        T newId = generateId();
        while (exist(newId))
            newId = generateId();
        hashSet.add(newId);
        return newId;
    }

    abstract T generateId();
}

