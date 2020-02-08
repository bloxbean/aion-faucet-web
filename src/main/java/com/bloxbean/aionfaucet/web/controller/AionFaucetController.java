package com.bloxbean.aionfaucet.web.controller;

import avm.Address;
import com.bloxbean.aionfaucet.web.exception.AlreadyUsedException;
import com.bloxbean.aionfaucet.web.exception.InvalidMessageException;
import com.bloxbean.aionfaucet.web.model.Challenge;
import com.bloxbean.aionfaucet.web.service.HashCashService;
import com.bloxbean.aionfaucet.web.service.NetworkService;
import com.bloxbean.aionfaucet.web.service.RedisService;
import com.bloxbean.aionfaucet.web.util.ConfigHelper;
import com.bloxbean.aionfaucet.web.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.aion4j.avm.helper.api.Log;
import org.aion4j.avm.helper.faucet.model.Network;
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
import java.util.List;

@RestController
@Slf4j
public class AionFaucetController {

    private final static String VERSION = "0.0.5";

    private long defaultGas = 2000000;
    private long defaultGasPrice = 100000000000L;

    @Autowired
    private RedisService redisService;

    @Autowired
    private HashCashService hashCashService;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private HttpServletRequest request;

    @GetMapping(value = "/hello")
    public ResponseEntity<String> hello(HttpServletRequest request) {
        return new ResponseEntity<>("Hello Faucet: " + RequestUtil.getClientIpAddress(request) + " v" + VERSION, HttpStatus.OK);
    }

    @RequestMapping("networks")
    public List<Network> getNetworks() {
        return networkService.getNetworks();
    }

    @GetMapping(value="/", produces = MediaType.TEXT_PLAIN_VALUE)
    public String home() throws NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();
        List<Network> networks = networkService.getNetworks();

        for(Network network: networks) {
            String operatorKey = ConfigHelper.getOperatorKey(network.getId());
            String nodeUrl = ConfigHelper.getNodeUrl(network.getId());

            String encryptedOpKey = null;
            if(operatorKey != null) {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(operatorKey.getBytes());
                byte[] digest = md.digest();
                encryptedOpKey = HexUtil.bytesToHexString(digest);
            }

            sb.append(network.getId())
                    .append(encryptedOpKey)
                    .append(" ")
                    .append(nodeUrl != null? nodeUrl.substring(0,8): null)
                    .append("\n");
        }

        return "Hello Faucet" + " \n " +  sb.toString();
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

//        Log log = new DefaultLog();
//
//        RemoteAVMNode remoteAVMNode = new RemoteAVMNode(nodeUrl, log);
//        String latestBlock = remoteAVMNode.getLatestBlock();

        return redisService.getChallenge(clientIp);
    }

    @PostMapping(value = "/register", consumes = "text/plain")
    public TopupResult register(@RequestBody String hashcash) {
        String account = null;
        String network = null;
        try {
            HashCashService.Extension extension = hashCashService.validate(hashcash);
            if(extension == null || extension.getAccount() == null) {
                return TopupResult.builder().error("Validation failed").build();
            }
            account = extension.getAccount();
            network = extension.getNetwork();

            if(StringUtils.isEmpty(network))
                network = ConfigHelper.MASTERY_NETWORK;

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

        String operatorKey = ConfigHelper.getOperatorKey(network);
        String nodeUrl = ConfigHelper.getNodeUrl(network);

        if(StringUtils.isEmpty(nodeUrl)) {
            return TopupResult.builder().error("Node url not found").build();
        }

        if(StringUtils.isEmpty(operatorKey)) {
            return TopupResult.builder().error("Operator's key not found").build();
        }

        String faucetContractAddress = networkService.getFaucetContractAddress(network);
        if(faucetContractAddress == null) {
            faucetContractAddress = ConfigHelper.getCustomFaucetContractAddress();
        }

        if(StringUtils.isEmpty(faucetContractAddress)) {
            return TopupResult.builder().error("Faucet contract address could not be found for the network : " + network).build();
        }

        Log defaultLog = new DefaultLog();

        if(log.isDebugEnabled()) {
            log.debug("Node url : " + nodeUrl);
            log.debug("Faucet contract address :" + faucetContractAddress);
        }

        RemoteAVMNode remoteAVMNode = new RemoteAVMNode(nodeUrl, defaultLog);
        try {

            String encodedMethodCall = LocalAvmNode.encodeMethodCall("registerAddress", new Object[]{new Address(HexUtil.hexStringToBytes(account))});
            log.info("Encoded method call data: " + encodedMethodCall);

            String txHash = remoteAVMNode.sendRawTransaction(faucetContractAddress, operatorKey, encodedMethodCall, BigInteger.ZERO, defaultGas, defaultGasPrice);

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