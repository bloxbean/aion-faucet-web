package com.bloxbean.aionfaucet.web.controller;

import avm.Address;
import com.bloxbean.aionfaucet.web.exception.AlreadyUsedException;
import com.bloxbean.aionfaucet.web.exception.InvalidMessageException;
import com.bloxbean.aionfaucet.web.model.Challenge;
import com.bloxbean.aionfaucet.web.service.HashCashService;
import com.bloxbean.aionfaucet.web.service.RedisService;
import com.bloxbean.aionfaucet.web.util.ConfigHelper;
import com.bloxbean.aionfaucet.web.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.aion4j.avm.helper.api.Log;
import org.aion4j.avm.helper.local.LocalAvmNode;
import org.aion4j.avm.helper.remote.RemoteAVMNode;
import org.aion4j.avm.helper.util.HexUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
@Slf4j
public class AionFaucetController {

    private final static String VERSION = "0.0.2";

    private final String FAUCET_CONTRACT_ADDRESS = "0xa055dc67cd05d013a0b7c064708a0eb86e31c5edbaf00bca645665217779d9f2";

    //private BigInteger MIN_AMNT_TO_TRANSFER = new BigInteger("500000000000000000"); //0.5 Aion
    private long defaultGas = 2000000;
    private long defaultGasPrice = 100000000000L;

    @Autowired
    private RedisService redisService;

    @Autowired
    private HashCashService hashCashService;

    @Autowired
    private HttpServletRequest request;

    @GetMapping(value = "/hello")
    public ResponseEntity<String> hello(HttpServletRequest request) {
        return new ResponseEntity<>("Hello Faucet: " + RequestUtil.getClientIpAddress(request) + " v" + VERSION, HttpStatus.OK);
    }

    @GetMapping(value="/", produces = MediaType.TEXT_PLAIN_VALUE)
    public String home() throws NoSuchAlgorithmException {

        String operatorKey = ConfigHelper.getOperatorKey(ConfigHelper.MASTERY_NETWORK);
        String nodeUrl = ConfigHelper.getNodeUrl(ConfigHelper.MASTERY_NETWORK);

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(operatorKey.getBytes());
        byte[] digest = md.digest();
        String opKey = HexUtil.bytesToHexString(digest);

        return "Hello Faucet" + "  " +  opKey + "  " + nodeUrl.substring(0,8);
    }

    @RequestMapping("challenge")
    public Challenge getChallenge(HttpServletRequest request) {
        String clientIp = RequestUtil.getClientIpAddress(request);

        if(log.isDebugEnabled())
            log.debug("Client IP >> " + clientIp);

        String nodeUrl = ConfigHelper.getNodeUrl(ConfigHelper.MASTERY_NETWORK);

        if(StringUtils.isEmpty(nodeUrl)) {
            return null;
        }

        Log log = new DefaultLog();

        RemoteAVMNode remoteAVMNode = new RemoteAVMNode(nodeUrl, log);
        String latestBlock = remoteAVMNode.getLatestBlock();

        return redisService.getChallenge(clientIp);
    }

    @PostMapping(value = "/register", consumes = "text/plain")
    public TopupResult register(@RequestBody String hashcash) {
        String operatorKey = ConfigHelper.getOperatorKey(ConfigHelper.MASTERY_NETWORK);
        String nodeUrl = ConfigHelper.getNodeUrl(ConfigHelper.MASTERY_NETWORK);

        if(StringUtils.isEmpty(nodeUrl)) {
            return TopupResult.builder().error("Node url not found").build();
        }

        if(StringUtils.isEmpty(operatorKey)) {
            return TopupResult.builder().error("Operator's key not found").build();
        }

        String account = null;
        try {
            account = hashCashService.validate(hashcash);
            if(account == null) {
                return TopupResult.builder().error("Validation failed").build();
            }
        } catch (AlreadyUsedException e) {
            log.error("Error in validation",e);
            return TopupResult.builder().error("Hashcash message has alread been used.").build();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error in validation",e);
            return TopupResult.builder().error("Invalid hashcash message.").build();
        } catch (InvalidMessageException e) {
            log.error("Error in validation",e);
            return TopupResult.builder().error("Invalid hashcash message.").build();
        }

        Log log = new DefaultLog();

        RemoteAVMNode remoteAVMNode = new RemoteAVMNode(nodeUrl, log);
        try {

            String encodedMethodCall = LocalAvmNode.encodeMethodCall("registerAddress", new Object[]{new Address(HexUtil.hexStringToBytes(account))});
            log.info("Encoded method call data: " + encodedMethodCall);

            String txHash = remoteAVMNode.sendRawTransaction(FAUCET_CONTRACT_ADDRESS, operatorKey, encodedMethodCall, BigInteger.ZERO, defaultGas, defaultGasPrice);

            if (txHash != null) {
                return TopupResult.builder().txHash(txHash).build();
            } else {
                return TopupResult.builder().error("Txn hash is null").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error in transaction", e);
            return TopupResult.builder().error("Error in topup: " + e.getMessage()).build();
        }
    }

}