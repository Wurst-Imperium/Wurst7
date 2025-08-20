package net.wurstclient.hacks;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.ThreadLocalRandom; // ? Rastgele gecikme için
import net.minecraft.class_1268;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_3965;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import net.minecraft.class_239.class_240;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.AutoBuildTemplate;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.DefaultAutoBuildTemplates;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.json.JsonException;
import org.lwjgl.opengl.GL11;

public final class AutoBuildHack extends Hack implements UpdateListener, RightClickListener, RenderListener {
  private final FileSetting templateSetting =
      new FileSetting(
          "Template",
          "Determines what to build.\n\nTemplates are just JSON files. Feel free to add your own or to edit / delete the default templates.\n\nIf you mess up, simply press the 'Reset to Defaults' button or delete the folder.",
          "autobuild",
          DefaultAutoBuildTemplates::createFiles);

  private final SliderSetting range;
  private final CheckboxSetting checkLOS;
  private final CheckboxSetting instaBuild;
  private final CheckboxSetting fastPlace;

  private AutoBuildHack.Status status;
  private AutoBuildTemplate template;
  private LinkedHashSet<class_2338> remainingBlocks;

  // ==== ?? ZAMANLAMA KONTROLÜ (İSTEDİĞİN ÖZELLİKLER) ====
  // Saniyede 3–5 blok -> 200–333 ms arası rastgele gecikme
  private static final int BASE_DELAY_MIN_MS = 200;
  private static final int BASE_DELAY_MAX_MS = 333;

  // %10 ihtimalle ekstra kısa duraksama
  private static final int PAUSE_CHANCE_PERCENT = 10;
  private static final int PAUSE_MIN_MS = 150;
  private static final int PAUSE_MAX_MS = 400;

  // Sonraki yerleştirme için hedef zaman (epoch millis)
  private long nextPlaceAtMillis = 0L;

  public AutoBuildHack() {
    super("AutoBuild");
    this.range =
        new SliderSetting(
            "Range",
            "How far to reach when placing blocks.\nRecommended values:\n6.0 for vanilla\n4.25 for NoCheat+",
            6.0D,
            1.0D,
            10.0D,
            0.05D,
            SliderSetting.ValueDisplay.DECIMAL);
    this.checkLOS =
        new CheckboxSetting(
            "Check line of sight",
            "Makes sure that you don't reach through walls when placing blocks. Can help with AntiCheat plugins but slows down building.",
            false);
    this.instaBuild =
        new CheckboxSetting(
            "InstaBuild",
            "Builds small templates (<= 64 blocks) instantly.\nFor best results, stand close to the block you're placing.",
            true);
    this.fastPlace =
        new CheckboxSetting(
            "Always FastPlace",
            "Builds as if FastPlace was enabled, even if it's not.", true);

    this.status = AutoBuildHack.Status.NO_TEMPLATE;
    this.remainingBlocks = new LinkedHashSet<>();
    this.setCategory(Category.BLOCKS);
    this.addSetting(this.templateSetting);
    this.addSetting(this.range);
    this.addSetting(this.checkLOS);
    this.addSetting(this.instaBuild);
    this.addSetting(this.fastPlace);
  }

  public String getRenderName() {
    String name = this.getName();
    switch (this.status) {
      case NO_TEMPLATE:
      default:
        break;
      case LOADING:
        name = name + " [Loading...]";
        break;
      case IDLE:
        name = name + " [" + this.template.getName() + "]";
        break;
      case BUILDING:
        double total = (double) this.template.size();
        double placed = total - (double) this.remainingBlocks.size();
        double progress = (double) Math.round(placed / total * 10000.0D) / 100.0D;
        name = name + " [" + this.template.getName() + "] " + progress + "%";
    }
    return name;
  }

  @Override
  public void onEnable() {
    EVENTS.add(UpdateListener.class, this);
    EVENTS.add(RightClickListener.class, this);
    EVENTS.add(RenderListener.class, this);
    nextPlaceAtMillis = 0L; // Güvenli başlat
  }

  @Override
  public void onDisable() {
    EVENTS.remove(UpdateListener.class, this);
    EVENTS.remove(RightClickListener.class, this);
    EVENTS.remove(RenderListener.class, this);
    this.remainingBlocks.clear();
    if (this.template == null) {
      this.status = AutoBuildHack.Status.NO_TEMPLATE;
    } else {
      this.status = AutoBuildHack.Status.IDLE;
    }
  }

  @Override
  public void onUpdate() {
    switch (this.status) {
      case NO_TEMPLATE:
        this.loadSelectedTemplate();
      case LOADING:
      default:
        break;
      case IDLE:
        if (!this.template.isSelected(this.templateSetting)) {
          this.loadSelectedTemplate();
        }
        break;
      case BUILDING:
        this.buildNormally();
    }
  }

  private void loadSelectedTemplate() {
    this.status = AutoBuildHack.Status.LOADING;
    Path path = this.templateSetting.getSelectedFile();
    try {
      this.template = AutoBuildTemplate.load(path);
      this.status = AutoBuildHack.Status.IDLE;
    } catch (JsonException | IOException var6) {
      Path fileName = path.getFileName();
      ChatUtils.error("Couldn't load template '" + fileName + "'.");
      String simpleClassName = var6.getClass().getSimpleName();
      String message = var6.getMessage();
      ChatUtils.message(simpleClassName + ": " + message);
      var6.printStackTrace();
      this.setEnabled(false);
    }
  }

  /**
   * Normal inşâ akışı — BURAYA zamanlama mantığını ekledik:
   * - Blok listesi güncellenir
   * - Eğer yerleştirme zamanı gelmediyse bekler
   * - Zamanı gelince bir blok dener; sonrası için yeni gecikme kurulur
   */
  private void buildNormally() {
    this.updateRemainingBlocks();
    if (this.remainingBlocks.isEmpty()) {
      this.status = AutoBuildHack.Status.IDLE;
      return;
    }

    // Oyun içi cooldown (MC.field_1752) zaten var; ama biz ek zamanlama da istiyoruz.
    // FastPlace açık olsa bile bizim zamanlayıcımız hızı 3–5 blok/s'ye sabitliyor.
    long now = System.currentTimeMillis();

    // first-time init
    if (nextPlaceAtMillis == 0L) {
      nextPlaceAtMillis = now;
    }

    // Zaman henüz gelmediyse hiç yerleştirme yapma
    if (now < nextPlaceAtMillis) {
      return;
    }

    // Cooldown bitti ise ya da fastPlace etkin ise dene
    if (this.fastPlace.isChecked() || MC.field_1752 <= 0) {
      boolean placed = this.placeNextBlock();
      // Bir yerleştirme denemesinden sonra sıradaki hedef zamanı hesapla
      // Başarı olsun olmasın bir sonraki denemeyi geciktiriyoruz (daha akıcı insansı davranış)
      nextPlaceAtMillis = now + computeNextDelayMs();
      if (placed) {
        // Oyun içi yerleştirme gecikmesi zaten set ediliyor (MC.field_1752 = 4) tryToPlace içinde.
        // Ekstra bir şey yapmaya gerek yok.
      }
    }
  }

  // 200–333 ms + (%10 şansla) 150–400 ms ek duraksama
  private int computeNextDelayMs() {
    ThreadLocalRandom rnd = ThreadLocalRandom.current();
    int base = rnd.nextInt(BASE_DELAY_MIN_MS, BASE_DELAY_MAX_MS + 1);
    int extra = 0;
    if (rnd.nextInt(100) < PAUSE_CHANCE_PERCENT) {
      extra = rnd.nextInt(PAUSE_MIN_MS, PAUSE_MAX_MS + 1);
    }
    return base + extra;
  }

  private void updateRemainingBlocks() {
    Iterator<class_2338> itr = this.remainingBlocks.iterator();
    while (itr.hasNext()) {
      class_2338 pos = itr.next();
      class_2680 state = BlockUtils.getState(pos);
      if (!state.method_45474()) {
        itr.remove();
      }
    }
  }

  /**
   * Sıradaki yerleştirilebilir bloğu bulup yerleştirmeyi dener.
   * Dönüş: başarıyla yerleştirdiyse true.
   */
  private boolean placeNextBlock() {
    class_243 eyesPos = RotationUtils.getEyesPos();
    double rangeSq = Math.pow(this.range.getValue(), 2.0D);

    Iterator<class_2338> it = this.remainingBlocks.iterator();
    while (it.hasNext()) {
      class_2338 pos = it.next();
      if (this.tryToPlace(pos, eyesPos, rangeSq)) {
        return true;
      }
    }
    return false;
  }

  private boolean tryToPlace(class_2338 pos, class_243 eyesPos, double rangeSq) {
    class_243 posVec = class_243.method_24953(pos);
    double distanceSqPosVec = eyesPos.method_1025(posVec);
    for (class_2350 side : class_2350.values()) {
      class_2338 neighbor = pos.method_10093(side);
      if (BlockUtils.canBeClicked(neighbor) && !BlockUtils.getState(neighbor).method_45474()) {
        class_243 dirVec = class_243.method_24954(side.method_10163());
        class_243 hitVec = posVec.method_1019(dirVec.method_1021(0.5D));
        boolean inRange =
            !(eyesPos.method_1025(hitVec) > rangeSq)
                && !(distanceSqPosVec > eyesPos.method_1025(posVec.method_1019(dirVec)));
        boolean losOk = !this.checkLOS.isChecked() || BlockUtils.hasLineOfSight(eyesPos, hitVec);
        if (inRange && losOk) {
          RotationUtils.getNeededRotations(hitVec).sendPlayerLookPacket();
          IMC.getInteractionManager().rightClickBlock(neighbor, side.method_10153(), hitVec);
          MC.field_1724.method_6104(class_1268.field_5808);
          MC.field_1752 = 4; // vanilla place cooldown (ticks)
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void onRightClick(RightClickListener.RightClickEvent event) {
    if (this.status == AutoBuildHack.Status.IDLE) {
      class_239 hitResult = MC.field_1765;
      if (hitResult != null
          && hitResult.method_17784() != null
          && hitResult.method_17783() == class_240.field_1332
          && hitResult instanceof class_3965) {
        class_3965 blockHitResult = (class_3965) hitResult;
        class_2338 hitResultPos = blockHitResult.method_17777();
        if (BlockUtils.canBeClicked(hitResultPos)) {
          class_2338 startPos = hitResultPos.method_10093(blockHitResult.method_17780());
          class_2350 direction = MC.field_1724.method_5735();
          this.remainingBlocks = this.template.getPositions(startPos, direction);

          if (this.instaBuild.isChecked() && this.template.size() <= 64) {
            this.buildInstantly();
          } else {
            // İnşaya başlarken zamanlayıcıyı hemen tetikle
            nextPlaceAtMillis = 0L;
            this.status = AutoBuildHack.Status.BUILDING;
          }
        }
      }
    }
  }

  private void buildInstantly() {
    class_243 eyesPos = RotationUtils.getEyesPos();
    IClientPlayerInteractionManager im = IMC.getInteractionManager();
    double rangeSq = Math.pow(this.range.getValue(), 2.0D);

    Iterator<class_2338> it = this.remainingBlocks.iterator();
    while (it.hasNext()) {
      class_2338 pos = it.next();
      if (!BlockUtils.getState(pos).method_45474()) {
        continue;
      }
      class_243 posVec = class_243.method_24953(pos);
      for (class_2350 side : class_2350.values()) {
        class_2338 neighbor = pos.method_10093(side);
        if (BlockUtils.canBeClicked(neighbor)) {
          class_243 sideVec = class_243.method_24954(side.method_10163());
          class_243 hitVec = posVec.method_1019(sideVec.method_1021(0.5D));
          if (!(eyesPos.method_1025(hitVec) > rangeSq)) {
            im.rightClickBlock(neighbor, side.method_10153(), hitVec);
            break;
          }
        }
      }
    }
    this.remainingBlocks.clear();
  }

  @Override
  public void onRender(class_4587 matrixStack, float partialTicks) {
    if (this.status == AutoBuildHack.Status.BUILDING) {
      float scale = 0.875F;
      double offset = (1.0D - (double) scale) / 2.0D;
      class_243 eyesPos = RotationUtils.getEyesPos();
      double rangeSq = Math.pow(this.range.getValue(), 2.0D);

      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2884);
      RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 0.5F);
      matrixStack.method_22903();
      RegionPos region = RenderUtils.getCameraRegion();
      RenderUtils.applyRegionalRenderOffset(matrixStack, region);
      int blocksDrawn = 0;
      RenderSystem.setShader(class_757::method_34539);

      Iterator<class_2338> itr = this.remainingBlocks.iterator();
      while (itr.hasNext() && blocksDrawn < 1024) {
        class_2338 pos = itr.next();
        if (BlockUtils.getState(pos).method_45474()) {
          matrixStack.method_22903();
          matrixStack.method_46416(
              (float) (pos.method_10263() - region.x()),
              (float) pos.method_10264(),
              (float) (pos.method_10260() - region.z()));
          matrixStack.method_22904(offset, offset, offset);
          matrixStack.method_22905(scale, scale, scale);
          class_243 posVec = class_243.method_24953(pos);
          if (eyesPos.method_1025(posVec) <= rangeSq) {
            this.drawGreenBox(matrixStack);
          } else {
            RenderUtils.drawOutlinedBox(matrixStack);
          }
          matrixStack.method_22909();
          ++blocksDrawn;
        }
      }

      matrixStack.method_22909();
      GL11.glDisable(3042);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
  }

  private void drawGreenBox(class_4587 matrixStack) {
    GL11.glDepthMask(false);
    RenderSystem.setShaderColor(0.0F, 1.0F, 0.0F, 0.15F);
    RenderUtils.drawSolidBox(matrixStack);
    GL11.glDepthMask(true);
    RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 0.5F);
    RenderUtils.drawOutlinedBox(matrixStack);
  }

  private static enum Status {
    NO_TEMPLATE,
    LOADING,
    IDLE,
    BUILDING;

    // $FF: synthetic method
    private static AutoBuildHack.Status[] $values() {
      return new AutoBuildHack.Status[] {NO_TEMPLATE, LOADING, IDLE, BUILDING};
    }
  }
}
