package com.bloxbean.aionfaucet.web.util;

public class ConfigHelper {
    public static String MASTERY_NETWORK = "mastery";
    public static String FAUCET_CONTRACT_ADDRESS = "faucet_contract_address";

    public static String getOperatorKey(String network) {
        return System.getenv("operator_key");
    }

    public static String getNodeUrl(String network) {
        return System.getenv("node_url");
    }
}
