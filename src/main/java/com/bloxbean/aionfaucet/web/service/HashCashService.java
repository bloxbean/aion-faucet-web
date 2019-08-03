package com.bloxbean.aionfaucet.web.service;

import com.bloxbean.aionfaucet.web.exception.AlreadyUsedException;
import com.bloxbean.aionfaucet.web.exception.InvalidMessageException;
import com.bloxbean.aionfaucet.web.model.Challenge;
import com.nettgryppa.security.HashCash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class HashCashService {

    @Autowired
    private RedisService redisService;

    public String validate(String hashCashStr) throws AlreadyUsedException, NoSuchAlgorithmException, InvalidMessageException {
        Object hashCashOj = redisService.getHashCash(hashCashStr);

        if(hashCashOj != null)
            throw new AlreadyUsedException("Hashcash value has already been used");

        HashCash hashCash = new HashCash(hashCashStr);

        //Get value
        int value = hashCash.getValue();
        Calendar date = hashCash.getDate();

        String message = hashCash.getResource();
        Long counter =  null;
        String account = null;

        List<String> list = hashCash.getExtensions().get("data");

        if(list != null && list.size() >= 2 ) {
            counter = Long.parseLong(list.get(0));
            account = list.get(1);
        }

        if(counter ==  null || counter == 0)
            throw new InvalidMessageException("Invalid or null counter");

        if(StringUtils.isEmpty(account))
            throw new InvalidMessageException("Invalid account in the message");

        Challenge challengeInRedis = redisService.getChallengeForCounter(counter);

        if(challengeInRedis == null) {
            System.out.println("Challenge not found for counter >> " + counter);
            return account;
        }

        if(!challengeInRedis.getMessage().equals(message) || challengeInRedis.getValue() != value) {
            System.out.println("Tempered message");
            return null;
        } else {
            return account;
        }

    }
}
