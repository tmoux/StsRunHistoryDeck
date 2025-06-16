package runhistorydeck.patches;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;
import com.megacrit.cardcrawl.screens.runHistory.RunHistoryScreen;
import com.megacrit.cardcrawl.screens.stats.RunData;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class RunHistoryScreenPatch {
    public static final Logger log = LogManager.getLogger();
    public static DropdownMenu deckSortOptions;


    @SpirePatch(
            clz = RunHistoryScreen.class,
            method = "reloadCards"
    )
    public static class ReloadCardsPatch {
        @SpireInsertPatch(
                locator = Locator.class,
                localvars = { "sortedMasterDeck" }
        )
        public static void sortDeckByType(RunHistoryScreen __instance, RunData runData, CardGroup sortedMasterDeck) {
            log.info("Sorting deck by index {}", deckSortOptions.getSelectedIndex());
            switch (deckSortOptions.getSelectedIndex()) {
                case 0: // type
                    sortedMasterDeck.sortByType(true);
                    break;
                case 1: // rarity
                    sortedMasterDeck.sortByRarityPlusStatusCardType(false);
                    break;
                case 2: // A-Z
                    sortedMasterDeck.sortAlphabetically(true);
                    break;
            }
        }

        public static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws PatchingException, CannotCompileException {
                Matcher matcher = new Matcher.MethodCallMatcher(CardGroup.class, "getGroupedByColor");
                return LineFinder.findInOrder(ctMethodToPatch, new ArrayList<>(), matcher);
            }
        }
    }

    @SpirePatch(
            clz = RunHistoryScreen.class,
            method = SpirePatch.CONSTRUCTOR
    )
    public static class ConstructorPatch {
        @SpirePostfixPatch
        public static void addDeckSortMenu(RunHistoryScreen __instance) {
            String[] options = {"Type", "Rarity", "A-Z"};
            deckSortOptions = new DropdownMenu(__instance, options, FontHelper.cardDescFont_N, Settings.CREAM_COLOR);
        }
    }

    @SpirePatch(
            clz = RunHistoryScreen.class,
            method = "renderFilters"
    )
    public static class RenderFiltersPatch {
        @SpirePostfixPatch
        public static void renderDeckFilters(RunHistoryScreen __instance, SpriteBatch sb, float ___screenX, float ___scrollY) {

            float screenPosX = ___screenX + (1075.0F * Settings.xScale);
            float screenPosY = ___scrollY + (1000.0F * Settings.yScale) - (504.0F * Settings.yScale);

            FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, "Sort Deck", screenPosX, screenPosY, 9999.0F, 50.0F * Settings.yScale, Settings.GOLD_COLOR);
            deckSortOptions.render(sb, screenPosX + (180.0F * Settings.xScale), screenPosY);
        }
    }

    @SpirePatch(clz= RunHistoryScreen.class, method="update")
    public static class UpdateOpenPatch {
        @SpireInsertPatch(
                locator = Locator.class
        )
        public static SpireReturn<Void> Insert(RunHistoryScreen __instance) {
            if (deckSortOptions.isOpen) {
                deckSortOptions.update();
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate (CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(DropdownMenu.class, "update");
                int[] lineNums = LineFinder.findAllInOrder(ctMethodToPatch, finalMatcher);
                return new int[]{lineNums[5]}; // only call this after the fifth instance of DropdownMenu.update
            }
        }
    }

    @SpirePatch(clz= RunHistoryScreen.class, method="update")
    public static class UpdateNotOpenPatch {
        @SpireInsertPatch(
                locator = Locator.class
        )
        public static void Insert(RunHistoryScreen __instance) {
            deckSortOptions.update();
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate (CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(MenuCancelButton.class, "update");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }

    @SpirePatch(clz= RunHistoryScreen.class, method="changedSelectionTo")
    public static class DropdownUpdatedPatch {
        @SpirePostfixPatch
        public static void dropdownUpdated(RunHistoryScreen __instance, DropdownMenu dropdownMenu, int index, String optionText) {
            if (dropdownMenu == deckSortOptions) {
                ReflectionHacks.privateMethod(RunHistoryScreen.class, "resetRunsDropdown")
                        .invoke(__instance);
            }
        }
    }
}
