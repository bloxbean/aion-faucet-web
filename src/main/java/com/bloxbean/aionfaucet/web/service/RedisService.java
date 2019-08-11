package com.bloxbean.aionfaucet.web.service;

import com.bloxbean.aionfaucet.web.model.Challenge;
import com.nettgryppa.security.HashCash;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final static long HASHCASH_USED_MESSAGE_EXPIRY = 60;
    private final static long CHALLENGE_TIMEOUT = 120;
    private final static int defaultChallengeValue = 24;

    private final static String HASHCASH_KEY = "hashcash_key";
    private final static String CHALLEGE_VALUE_KEY = "current_challenge_value";
    private final static String CHALLENGE_COUNTER_PREFIX = "CC:";

    private RedisTemplate redisTemplate;
    private ValueOperations<String, Challenge> challengeOperation;

    public RedisService(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.challengeOperation = redisTemplate.opsForValue();
    }

    public Long nextChallengeNo() {
        return redisTemplate.opsForValue().increment("challenge_counter");
    }

    public void addHashCash(HashCash hashCash) {
        redisTemplate.opsForValue().set(hashCash.toString(), hashCash.getValue(), HASHCASH_USED_MESSAGE_EXPIRY, TimeUnit.SECONDS);
    }

    public Object getHashCash(String hashcash) {
        return redisTemplate.opsForValue().get(hashcash);
    }

    public int getHashCashValue(String hashCashString) {
        try {
            return Integer.parseInt((String) redisTemplate.opsForValue().get(hashCashString));
        } catch (Exception e) {
            return -1;
        }
    }

    public Challenge getChallenge() {
        ValueOperations<String, Integer> valueOperation = redisTemplate.opsForValue();

        Integer currentChallengeValue = valueOperation.get(CHALLEGE_VALUE_KEY);
        if(currentChallengeValue == null)
            currentChallengeValue = defaultChallengeValue;

        if(currentChallengeValue < defaultChallengeValue) {
            currentChallengeValue = defaultChallengeValue;
        }

        Long challengeCounter = nextChallengeNo();

        String randomString = RandomStringUtils.randomAlphabetic(5);

        Challenge challenge = Challenge.builder().value(currentChallengeValue)
                .message(randomString)
                .counter(challengeCounter)
                .build();

        challengeOperation.set(CHALLENGE_COUNTER_PREFIX + challengeCounter, challenge, CHALLENGE_TIMEOUT, TimeUnit.SECONDS);

        return challenge;
    }

    public Challenge getChallengeForCounter(long counter) {
        return challengeOperation.get(CHALLENGE_COUNTER_PREFIX + counter);
    }

}
