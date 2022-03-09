package net.wurstclient.altmanager;

import com.google.gson.*;
import net.minecraft.client.util.Session;
import net.wurstclient.WurstClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MicrosoftLoginManager {
    private static String clientID = "00000000402b5328";
    private static String scope = "service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL";
    private static String scopeURL = "service::user.auth.xboxlive.com::MBI_SSL";
    private static String redirectURL = "https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf";
    private static String loginURL = "https://login.live.com/oauth20_authorize.srf?client_id=" + clientID + "&response_type=code&scope=" + scope + "&redirect_uri=" + redirectURL;

    public static void login(String email, String password)
            throws LoginException {
        MinecraftProfile minecraftProfile = getAccount(email, password);

        if(minecraftProfile == null)
            throw new LoginException(
                    "\u00a74\u00a7lWrong password! (or shadowbanned)");

        Session session = new Session(minecraftProfile.name, minecraftProfile.uuid.toString(), minecraftProfile.jwt,
                Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);

        WurstClient.IMC.setSession(session);
    }

    public static MinecraftProfile getAccount(String email, String password) {
        try {
            XBLToken xblToken = getXBLToken(getAuthorizationToken(getAuthorizationCode(email, password)));
            return getMinecraftProfile(getMinecraftAccessToken(xblToken.uhs, getXSTSToken(xblToken.token)));
        }catch(NullPointerException | LoginException e) {
            return null;
        }
    }

    private static String getXSTSToken(String xblToken) {
        String xstsToken = "";

        JsonArray tokens = new JsonArray();
        tokens.add(xblToken);
        JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");
        properties.add("UserTokens", tokens);

        JsonObject postData = new JsonObject();
        postData.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        postData.addProperty("TokenType", "JWT");
        postData.add("Properties", properties);

        String request = postData.toString();

        try {
            URL url = new URL("https://xsts.auth.xboxlive.com/xsts/authorize");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("Accept", "application/json");

            httpURLConnection.setDoOutput(true);

            try(OutputStream os = httpURLConnection.getOutputStream()) {
                os.write(request.getBytes(StandardCharsets.US_ASCII));
            }

            String data = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining());

            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(data);
            JsonObject object = jsonElement.getAsJsonObject();

            xstsToken = object.get("Token").getAsString();
        }catch(IOException e) {
            e.printStackTrace();
        }

        return xstsToken;
    }

    private static MinecraftProfile getMinecraftProfile(String token) throws LoginException {
        MinecraftProfile minecraftProfile = null;
        try {
            URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestProperty("Authorization", "Bearer " + token);

            String data = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining());

            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(data);
            JsonObject object = jsonElement.getAsJsonObject();

            if(object.has("error"))
                throw new LoginException("Username / Password was incorrect.");

            minecraftProfile = new MinecraftProfile(UUID.fromString(object.get("id").getAsString().replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
            )), object.get("name").getAsString(), token);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return minecraftProfile;
    }

    private static String getMinecraftAccessToken(String uhs, String xstsToken) {
        String accessToken = "";

        JsonObject postData = new JsonObject();
        postData.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);

        String request = postData.toString();

        try {
            URL url = new URL("https://api.minecraftservices.com/authentication/login_with_xbox");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("Accept", "application/json");

            httpURLConnection.setDoOutput(true);

            try(OutputStream os = httpURLConnection.getOutputStream()) {
                os.write(request.getBytes(StandardCharsets.US_ASCII));
            }

            String data = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining());

            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(data);
            JsonObject object = jsonElement.getAsJsonObject();

            accessToken = object.get("access_token").getAsString();
        }catch(IOException e) {
            e.printStackTrace();
        }

        return accessToken;
    }

    private static XBLToken getXBLToken(String authorizationCode) {
        XBLToken xblToken = null;

        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", authorizationCode);

        JsonObject postData = new JsonObject();
        postData.addProperty("RelyingParty", "http://auth.xboxlive.com");
        postData.addProperty("TokenType", "JWT");
        postData.add("Properties", properties);

        String request = postData.toString();

        try {
            URL url = new URL("https://user.auth.xboxlive.com/user/authenticate");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("Accept", "application/json");

            httpURLConnection.setDoOutput(true);

            try(OutputStream os = httpURLConnection.getOutputStream()) {
                os.write(request.getBytes(StandardCharsets.US_ASCII));
            }

            String data = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining());

            Gson g = new Gson();
            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(data);
            JsonObject object = jsonElement.getAsJsonObject();

            String token = object.get("Token").getAsString();
            String uhs = object.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();

            xblToken = new XBLToken(token, uhs);
        }catch(IOException e) {
            e.printStackTrace();
        }
        return xblToken;
    }

    private static String getAuthorizationToken(String authorizationCode) {
        String authToken = "";

        Map<String, String> postData = new HashMap<>();
        postData.put("client_id", clientID);
        postData.put("code", authorizationCode);
        postData.put("grant_type", "authorization_code");
        postData.put("redirect_uri", "https://login.live.com/oauth20_desktop.srf");
        postData.put("scope", scopeURL);

        String encodedData = encodeMap(postData);

        try {
            byte[] encodedDataBytes = encodedData.getBytes(StandardCharsets.UTF_8);
            URL url = new URL("https://login.live.com/oauth20_token.srf");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            httpURLConnection.setDoOutput(true);

            try(OutputStream os = httpURLConnection.getOutputStream()) {
                os.write(encodedDataBytes);
            }

            String data = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).lines().collect(Collectors.joining());

            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(data);
            JsonObject object = jsonElement.getAsJsonObject();

            authToken = object.get("access_token").getAsString();
        }catch(IOException e) {
            e.printStackTrace();
        }
        return authToken;
    }

    private static String getAuthorizationCode(String email, String password) {
        String cookie = "";
        String ppft = "";
        String login = "";

        try {
            URL url = new URL(loginURL);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = httpURLConnection.getInputStream();

            cookie = httpURLConnection.getHeaderField("set-cookie");

            String data = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining());
            Matcher matcher = Pattern.compile("sFTTag:[ ]?'.*value=\"(.*)\"/>").matcher(data);
            if(matcher.find()) {
                ppft = matcher.group(1);
            }else{
                throw new LoginException("sFTTag not found in response.");
            }

            matcher = Pattern.compile("urlPost:[ ]?'(.+?(?='))").matcher(data);
            if(matcher.find()) {
                login = matcher.group(1);
            }else{
                throw new LoginException("urlPost not found in response.");
            }
        } catch (IOException | LoginException e) {
            e.printStackTrace();
        }
        return microsoftLogin(email, password, cookie, ppft, login);
    }

    private static String microsoftLogin(String email, String password, String cookie, String ppft, String loginURL) {
        String authCode = "";

        Map<String, String> postData = new HashMap<>();
        postData.put("login", email);
        postData.put("loginfmt", email);
        postData.put("passwd", password);
        postData.put("PPFT", ppft);

        String encodedData = encodeMap(postData);

        try {
            byte[] encodedDataBytes = encodedData.getBytes(StandardCharsets.UTF_8);

            URL url = new URL(loginURL);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(encodedDataBytes.length));
            httpURLConnection.setRequestProperty("Cookie", cookie);

            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);

            try(OutputStream os = httpURLConnection.getOutputStream()) {
                os.write(encodedDataBytes);
            }

            System.out.println("Getting authorization code...");
            System.out.println(httpURLConnection.getResponseCode());
            if(httpURLConnection.getResponseCode() != 200) {
                throw new LoginException("Username / Password was incorrect.");
            }

            Pattern pattern = Pattern.compile("[?|&]code=([\\w.-]+)");
            Matcher matcher = pattern.matcher(URLDecoder.decode(httpURLConnection.getURL().toString(), StandardCharsets.UTF_8.name()));
            if(matcher.find()) {
                System.out.println("Authorization code: " + matcher.group(1));
                authCode = matcher.group(1);
            }else{
                throw new LoginException("code not found in response.");
            }

        } catch (IOException | LoginException e) {
            e.printStackTrace();
        }

        return authCode;
    }

    private static String encodeMap(Map<String, String> map) {
        StringBuilder str = new StringBuilder();
        for(Map.Entry<String, String> entry : map.entrySet()) {
            if(str.length() > 0)
                str.append("&");

            try {
                str.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name())).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return str.toString();
    }
}
