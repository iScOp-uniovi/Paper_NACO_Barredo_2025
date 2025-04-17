package com.uniovi.sercheduler.util;

/**
 * Contains two objects, useful for working with maps.
 *
 * @param key The first element of the pair.
 * @param value The second element of the pair.
 * @param <K> Defines the type of first element of the pair.
 * @param <V> Defines the type of second element of the pair.
 */
public record Pair<K, V>(K key, V value) {}
