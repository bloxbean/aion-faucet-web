package com.bloxbean.aionfaucet.web.service;

import org.aion4j.avm.helper.faucet.NetworkHelper;
import org.aion4j.avm.helper.faucet.model.Network;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Scope("singleton")
public class NetworkService {
    private List<Network> networks;

    public NetworkService() {

    }

    public List<Network> getNetworks() {
        if(this.networks == null) {
            NetworkHelper networkHelper = new NetworkHelper(null);
            this.networks = networkHelper.getNetworks();
        }

        return this.networks;
    }

    public String getFaucetContractAddress(String networkId) {
        if(this.networks == null)
            this.networks = getNetworks();

        if(networkId == null) return null;
        else
            networkId = networkId.trim();

        for(Network network: this.networks) {
            if(networkId.equals(network.getId())) {
                return network.getFaucetContract();
            }
        }

        return null;
    }
}
