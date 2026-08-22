package top.g2inp.nlome.client;

import java.text.Collator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import top.g2inp.nlome.config.FavoritesManager;

@Environment(EnvType.CLIENT)
public class ConfigScreen extends Screen {
	private static final int SEARCH_TOP = 24;
	private static final int THRESHOLD_TOP = 48;
	private static final int LIST_TOP = 76;
	private static final int LIST_BOTTOM_OFFSET = 36;
	private static final int ITEM_HEIGHT = 22;
	private static final int THRESHOLD_MIN = 1;
	private static final int THRESHOLD_MAX = 20;

	private static HolderLookup.Provider vanillaRegistries;

	private final Screen parent;
	private final Set<ResourceKey<Enchantment>> selected = new HashSet<>();
	private FavoriteList list;
	private EditBox searchBox;
	private EditBox thresholdBox;
	private Button sortButton;
	private boolean sortByName = true;
	private int breakThreshold = FavoritesManager.DEFAULT_BREAK_THRESHOLD;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("screen.nlome.config"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		this.selected.clear();
		FavoritesManager.ConfigData data = FavoritesManager.loadData();
		this.selected.addAll(data.favorites());
		this.breakThreshold = data.breakThreshold();

		int sortButtonWidth = 100;
		int sortButtonX = this.width - sortButtonWidth - 20;
		Component searchLabel = Component.translatable("screen.nlome.search");
		this.searchBox = this.addRenderableWidget(new EditBox(
			this.font, 20, SEARCH_TOP, sortButtonX - 30, 20, searchLabel));
		this.searchBox.setHint(searchLabel);
		this.searchBox.setResponder(this::applySearch);

		this.sortButton = this.addRenderableWidget(Button.builder(
			Component.translatable("button.nlome.sort.az"),
			button -> {
				this.sortByName = !this.sortByName;
				button.setMessage(this.sortLabel());
				this.applySearch(this.searchBox.getValue());
			})
			.bounds(sortButtonX, SEARCH_TOP, sortButtonWidth, 20)
			.build());
		this.sortButton.setMessage(this.sortLabel());

		Component thresholdLabel = Component.translatable("screen.nlome.threshold");
		int thresholdBoxX = 20 + this.font.width(thresholdLabel) + 8;
		this.thresholdBox = this.addRenderableWidget(new EditBox(
			this.font, thresholdBoxX, THRESHOLD_TOP, 40, 20, thresholdLabel));
		this.thresholdBox.setFilter(text -> text.chars().allMatch(Character::isDigit));
		this.thresholdBox.setMaxLength(2);
		this.thresholdBox.setResponder(text -> {
			if (text.isEmpty()) {
				this.breakThreshold = THRESHOLD_MIN;
				return;
			}

			try {
				this.breakThreshold = Math.max(THRESHOLD_MIN, Math.min(THRESHOLD_MAX, Integer.parseInt(text)));
			} catch (NumberFormatException exception) {
			}
		});
		this.thresholdBox.setValue(String.valueOf(this.breakThreshold));

		int stepButtonWidth = 20;
		int stepButtonX = this.width - stepButtonWidth * 2 - 20;
		this.addRenderableWidget(Button.builder(
			Component.literal("-"),
			button -> {
				this.breakThreshold = Math.max(THRESHOLD_MIN, this.breakThreshold - 1);
				this.thresholdBox.setValue(String.valueOf(this.breakThreshold));
			})
			.bounds(stepButtonX, THRESHOLD_TOP, stepButtonWidth, 20)
			.build());
		this.addRenderableWidget(Button.builder(
			Component.literal("+"),
			button -> {
				this.breakThreshold = Math.min(THRESHOLD_MAX, this.breakThreshold + 1);
				this.thresholdBox.setValue(String.valueOf(this.breakThreshold));
			})
			.bounds(stepButtonX + stepButtonWidth + 2, THRESHOLD_TOP, stepButtonWidth, 20)
			.build());

		int listBottom = this.height - LIST_BOTTOM_OFFSET;
		this.list = new FavoriteList(this, this.width, listBottom - LIST_TOP, LIST_TOP, ITEM_HEIGHT);
		this.addRenderableWidget(this.list);
		this.applySearch("");

		int buttonWidth = 100;
		int buttonY = this.height - 30;
		this.addRenderableWidget(Button.builder(
			Component.translatable("button.nlome.save"),
			button -> this.save())
			.bounds(this.width / 2 - buttonWidth - 2, buttonY, buttonWidth, 20)
			.build());
		this.addRenderableWidget(Button.builder(
			Component.translatable("button.nlome.cancel"),
			button -> this.onClose())
			.bounds(this.width / 2 + 2, buttonY, buttonWidth, 20)
			.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
		super.render(guiGraphics, mouseX, mouseY, tickDelta);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
		guiGraphics.drawString(this.font, Component.translatable("screen.nlome.threshold"),
			20, THRESHOLD_TOP + 6, 0xFFFFFFFF, false);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	private void save() {
		List<ResourceKey<Enchantment>> favorites = List.copyOf(this.selected);
		FavoritesManager.get().setFavorites(favorites);
		FavoritesManager.get().setBreakThreshold(this.breakThreshold);
		this.onClose();
	}

	private Component sortLabel() {
		return this.sortByName
			? Component.translatable("button.nlome.sort.az")
			: Component.translatable("button.nlome.sort.none");
	}

	private void applySearch(String filter) {
		this.list.rebuild(filter);
	}

	private boolean isSelected(ResourceKey<Enchantment> enchantment) {
		return this.selected.contains(enchantment);
	}

	private void toggle(ResourceKey<Enchantment> enchantment) {
		if (!this.selected.remove(enchantment)) {
			this.selected.add(enchantment);
		}
	}

	private static HolderLookup.RegistryLookup<Enchantment> enchantmentLookup() {
		if (Minecraft.getInstance().getConnection() != null) {
			return Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		}

		if (vanillaRegistries == null) {
			vanillaRegistries = VanillaRegistries.createLookup();
		}

		return vanillaRegistries.lookupOrThrow(Registries.ENCHANTMENT);
	}

	private static class FavoriteList extends ObjectSelectionList<FavoriteRow> {
		private static final Collator CJK_COLLATOR = Collator.getInstance(Locale.CHINA);

		private static final Comparator<Holder.Reference<Enchantment>> BY_NAME = (a, b) -> {
			String left = Language.getInstance().getOrDefault(a.value().description().getString());
			String right = Language.getInstance().getOrDefault(b.value().description().getString());
			int leftBucket = nameBucket(left);
			int rightBucket = nameBucket(right);
			if (leftBucket != rightBucket) {
				return Integer.compare(leftBucket, rightBucket);
			}

			return switch (leftBucket) {
				case 0 -> left.compareToIgnoreCase(right);
				case 1 -> {
					int collated = CJK_COLLATOR.compare(left, right);
					yield collated != 0 ? collated : left.compareTo(right);
				}
				default -> left.compareTo(right);
			};
		};

		private static int nameBucket(String name) {
			if (name.isEmpty()) {
				return 2;
			}

			int codePoint = name.codePointAt(0);
			if ((codePoint >= 'A' && codePoint <= 'Z') || (codePoint >= 'a' && codePoint <= 'z')) {
				return 0;
			}

			return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN ? 1 : 2;
		}

		private final ConfigScreen screen;
		private final List<Holder.Reference<Enchantment>> enchantments;

		FavoriteList(ConfigScreen screen, int width, int height, int y, int itemHeight) {
			super(Minecraft.getInstance(), width, height, y, itemHeight);
			this.screen = screen;
			this.enchantments = enchantmentLookup().listElements().toList();
		}

		void rebuild(String filter) {
			this.clearEntries();
			List<Holder.Reference<Enchantment>> entries = this.enchantments;
			if (this.screen.sortByName) {
				entries = this.enchantments.stream().sorted(BY_NAME).toList();
			}

			String query = filter.trim().toLowerCase();
			for (Holder.Reference<Enchantment> holder : entries) {
String name = Language.getInstance().getOrDefault(holder.value().description().getString());
				String id = holder.key().location().toString();
				if (!query.isEmpty() && !name.toLowerCase().contains(query) && !id.toLowerCase().contains(query)) {
					continue;
				}

				this.addEntry(new FavoriteRow(this.screen, holder.key(), holder));
			}
		}
	}

	private static class FavoriteRow extends ObjectSelectionList.Entry<FavoriteRow> {
		private static final int TOGGLE_WIDTH = 76;
		private static final int TOGGLE_HEIGHT = 16;
		private static final int TOGGLE_RIGHT_PADDING = 6;

		private final ConfigScreen screen;
		private final ResourceKey<Enchantment> enchantment;
		private final Holder<Enchantment> holder;

		FavoriteRow(ConfigScreen screen, ResourceKey<Enchantment> enchantment, Holder<Enchantment> holder) {
			this.screen = screen;
			this.enchantment = enchantment;
			this.holder = holder;
		}

		@Override
		public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
			Font font = this.screen.font;
			boolean selected = this.screen.isSelected(this.enchantment);
			int x = this.getX();
			int y = this.getY();

			if (hovered) {
				guiGraphics.fill(x + 1, y + 1, x + this.getWidth() - 1, y + this.getHeight() - 1, 0x20FFFFFF);
			}

			int textY = this.getContentY() + (this.getContentHeight() - font.lineHeight) / 2;
			guiGraphics.drawString(font, this.holder.value().description(), this.getContentX(), textY, 0xFFFFFFFF, false);

			int toggleX = x + this.getWidth() - TOGGLE_WIDTH - TOGGLE_RIGHT_PADDING;
			int toggleY = this.getContentY() + (this.getContentHeight() - TOGGLE_HEIGHT) / 2;
			int borderColor = selected ? 0xFF3CBF3C : 0xFF6A6A6A;
			int fillColor = selected ? 0xFF2A8F2A : 0xFF4A4A4A;
			guiGraphics.fill(toggleX - 1, toggleY - 1, toggleX + TOGGLE_WIDTH + 1, toggleY + TOGGLE_HEIGHT + 1, borderColor);
			guiGraphics.fill(toggleX, toggleY, toggleX + TOGGLE_WIDTH, toggleY + TOGGLE_HEIGHT, fillColor);
			Component label = selected
				? Component.translatable("button.nlome.selected")
				: Component.translatable("button.nlome.select");
			guiGraphics.drawString(font, label, toggleX + (TOGGLE_WIDTH - font.width(label)) / 2,
				toggleY + (TOGGLE_HEIGHT - font.lineHeight) / 2, 0xFFFFFFFF, false);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
			if (event.button() == 0 && this.isInsideToggle(event.x(), event.y())) {
				this.screen.toggle(this.enchantment);
			}

			return super.mouseClicked(event, bl);
		}

		private boolean isInsideToggle(double mouseX, double mouseY) {
			int toggleX = this.getX() + this.getWidth() - TOGGLE_WIDTH - TOGGLE_RIGHT_PADDING;
			int toggleY = this.getContentY() + (this.getContentHeight() - TOGGLE_HEIGHT) / 2;
			return mouseX >= toggleX && mouseX <= toggleX + TOGGLE_WIDTH
				&& mouseY >= toggleY && mouseY <= toggleY + TOGGLE_HEIGHT;
		}

		@Override
		public Component getNarration() {
			return this.holder.value().description();
		}
	}
}
