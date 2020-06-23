package net.wurstclient.altmanager.screens.thealtening;

import com.thealtening.api.TheAltening;
import com.thealtening.api.response.Account;
import com.thealtening.api.response.License;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.LiteralText;
import net.wurstclient.WurstClient;
import net.wurstclient.util.TheAlteningFile;
import org.lwjgl.glfw.GLFW;

public class TheAlteningScreen extends Screen {

    private final Screen previousScreen;
    private TextFieldWidget apiKeyTextFieldWidget;
    private ButtonWidget authenticateButtonWidget;
    private ButtonWidget generateAccountButtonWidget;
    private ButtonWidget addAccountButtonWidget;
    private String status = "\u00A7fWaiting...";
    private License license;
    private Account account;
    public TheAlteningScreen(Screen previousScreen) {
        super(new LiteralText("The Altening"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        if(minecraft != null) {
            addButton(apiKeyTextFieldWidget = new TextFieldWidget(minecraft.textRenderer, width / 2 + 55, 60, 150, 23, "API Key"));
            apiKeyTextFieldWidget.setSelected(true);
            apiKeyTextFieldWidget.setMaxLength(18);
            this.children.add(apiKeyTextFieldWidget);
            setInitialFocus(apiKeyTextFieldWidget);
            addButton(authenticateButtonWidget =new ButtonWidget(width / 2 - 100, 60, 150, 24, "Retrieve license data",(action) -> {
                if(!apiKeyTextFieldWidget.getText().isEmpty()) {
                    TheAltening.newAsyncRetriever(apiKeyTextFieldWidget.getText()).getLicenseDataAsync().thenAccept((license)-> {
                        status = "\u00a7aSuccess retrieving license data!";
                        this.license = license;
                        generateAccountButtonWidget.visible = true;
                    }).exceptionally((throwable) -> {
                        generateAccountButtonWidget.visible = false;
                        status = "\u00a74\u00A7lFailed retrieving data: " + throwable.getMessage();
                        return null;
                    });
                }
            }));
            this.children.add(authenticateButtonWidget);
            addButton(generateAccountButtonWidget = new ButtonWidget(width / 2 + 55, 90, 150, 14, "Generate Account", (action) -> {
                TheAltening.newAsyncRetriever(apiKeyTextFieldWidget.getText()).getAccountDataAsync().thenAccept((account -> {
                    status = "\u00A7aSuccess retrieving account data!";
                    this.account = account;
                    addAccountButtonWidget.visible = true;
                    TheAlteningFile.setApiKey(apiKeyTextFieldWidget.getText());
                })).exceptionally((throwable -> {
                    addAccountButtonWidget.visible = false;
                    status = "\u00A74\u00A7lFailed retrieving data: " + throwable.getMessage();
                    return null;
                }));
            }));
            if(license == null) {
                generateAccountButtonWidget.visible = false;
            }
            this.children.add(generateAccountButtonWidget);
            addButton(addAccountButtonWidget = new ButtonWidget(width / 2 + 55, 115 + minecraft.textRenderer.fontHeight * 7, 150, 14, "Add generated account", (action) -> {
                if(account == null) {
                    status = "\u00A74\u00A7lNo account was generated. Generate an account and try again";
                    return;
                }
                WurstClient.INSTANCE.getAltManager().add(account.getToken(), "anything", false);
                status = "\u00A7aAdded account to your alt list!";
            }));
            if(account == null) {
                addAccountButtonWidget.visible = false;
            }
            this.children.add(addAccountButtonWidget);
            TheAlteningFile.getAndUpdateApiKeyOrDefault(apiKeyTextFieldWidget.getText()).ifPresent(apiKey -> {
                this.apiKeyTextFieldWidget.setText(apiKey);
            });
        }
        super.init();
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();
        if(minecraft != null) {
            this.drawCenteredString(minecraft.textRenderer, "The Altening", width / 2, 3, -1);
            this.drawCenteredString(minecraft.textRenderer, status, width / 2, 15, -1);

            int fontHeight = minecraft.textRenderer.fontHeight;
            if(license != null) {
                if(license.isPremium()) {
                    this.drawString(minecraft.textRenderer, "User: " + license.getUsername(), width / 2 - 100, 90, -1);
                    this.drawString(minecraft.textRenderer, "Account Type: " + license.getPremiumName(), width / 2 - 100, 90 + fontHeight, -1);
                    this.drawString(minecraft.textRenderer, "Expiry Date: " + license.getExpiryDate(), width / 2 - 100, 90 + fontHeight * 2, -1);
                } else {
                    this.drawString(minecraft.textRenderer, "\u00a74Your account cannot be used to generate accounts.", width / 2 - 100, 90, -1);
                }
            }
            if(account != null) {
                int beginningY = 112;
                int x = width / 2 + 58;
                this.drawString(minecraft.textRenderer, "Hit generate limit: " + account.isLimit(), x, beginningY, -1);
                this.drawString(minecraft.textRenderer, "Name: " + account.getUsername(), x, beginningY + fontHeight, -1);
                this.drawString(minecraft.textRenderer, "Hypixel Level: " + account.getInfo().getHypixelLevel(), x, beginningY + fontHeight *2, -1);
                this.drawString(minecraft.textRenderer, "Hypixel Rank: " + (account.getInfo().getHypixelRank() == null ? "No Rank" : account.getInfo().getHypixelRank() == null), x, beginningY + fontHeight *3, -1);
                this.drawString(minecraft.textRenderer, "Mineplex Level: " + account.getInfo().getMineplexLevel(), x, beginningY + fontHeight *4, -1);
                this.drawString(minecraft.textRenderer, "Mineplex Rank: " + (account.getInfo().getMineplexRank() == null ? "No Rank" : account.getInfo().getMineplexRank() == null), x, beginningY + fontHeight *5, -1);
            }

        }
        super.render(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.apiKeyTextFieldWidget.mouseClicked(mouseX, mouseY, button);
        this.authenticateButtonWidget.mouseClicked(mouseX, mouseY, button);
        this.generateAccountButtonWidget.mouseClicked(mouseX, mouseY, button);
        this.addAccountButtonWidget.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    @Override
    public void tick() {
        this.apiKeyTextFieldWidget.tick();
        super.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if(minecraft != null) {
                minecraft.openScreen(this.previousScreen);
            }
            if(!apiKeyTextFieldWidget.getText().isEmpty()) {
                TheAlteningFile.setApiKey(apiKeyTextFieldWidget.getText());
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
