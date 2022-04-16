package net.wurstclient.core;

import net.minecraft.client.util.Session;

import java.util.Optional;

public class MCSession extends Session {


    public MCSession(String username, String uuid, String accessToken, Optional<String> xuid, Optional<String> clientId, AccountType accountType) {
        super(username, uuid, accessToken, xuid, clientId, accountType);
    }
}
