package com.bloxbean.aionfaucet.web.util;

public class ConfigHelper {
    public static String MASTERY_NETWORK = "mastery";

    public static String getOperatorKey(String network) {
        if(network != null)
            return System.getenv(network + "_operator_key");
        else
            return System.getenv("operator_key");
    }

    public static String getNodeUrl(String network) {
        if(network != null)
            return System.getenv(network + "_node_url");
        else
            return System.getenv("node_url");
    }

    public static String getCustomFaucetContractAddress() { //If custom network
        return System.getenv("faucet_contract");
    }
}
