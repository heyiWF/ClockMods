package com.clockmods.ultimate.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.window.embedding.ActivityRule;
import androidx.window.embedding.ActivityFilter;
import androidx.window.embedding.EmbeddingAspectRatio;
import androidx.window.embedding.RuleController;
import androidx.window.embedding.SplitAttributes;
import androidx.window.embedding.SplitController;
import androidx.window.embedding.SplitPairFilter;
import androidx.window.embedding.SplitPairRule;
import androidx.window.embedding.SplitPlaceholderRule;
import androidx.window.embedding.SplitRule;

import com.clockmods.R;
import com.clockmods.pro.alarm.AlarmRingingActivity;

import java.util.HashSet;
import java.util.Set;

/** AOSP Settings-style Activity Embedding rules for adaptive master-detail navigation. */
public final class UltimateEmbeddingRules {
    private static final String TAG_PAIR = "clockmods.settings.pair";
    private static final String TAG_PLACEHOLDER = "clockmods.settings.placeholder";
    private static final String TAG_ALWAYS_EXPAND = "clockmods.settings.always_expand";

    private UltimateEmbeddingRules() {
    }

    public static void install(Context context) {
        Context appContext = context.getApplicationContext();
        SplitController splitController = SplitController.getInstance(appContext);
        if (splitController.getSplitSupportStatus()
                != SplitController.SplitSupportStatus.SPLIT_AVAILABLE) {
            return;
        }

        ComponentName homeComponent = new ComponentName(
                appContext, UltimateSettingsActivity.class);
        ComponentName detailComponent = new ComponentName(
                appContext, UltimateSubSettingsActivity.class);
        int minCurrentWidthDp = appContext.getResources().getInteger(
                R.integer.ultimate_settings_split_min_width_dp);
        int minSmallestWidthDp = appContext.getResources().getInteger(
                R.integer.ultimate_settings_split_min_smallest_width_dp);
        float primaryRatio = appContext.getResources().getFraction(
                R.fraction.ultimate_settings_split_ratio, 1, 1);
        SplitAttributes splitAttributes = new SplitAttributes.Builder()
                .setSplitType(SplitAttributes.SplitType.ratio(primaryRatio))
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .build();

        Set<SplitPairFilter> pairFilters = new HashSet<>();
        pairFilters.add(new SplitPairFilter(homeComponent, detailComponent,
                UltimateSettingsActivity.ACTION_OPEN_SUBPAGE));
        SplitPairRule pairRule = new SplitPairRule.Builder(pairFilters)
                .setMinWidthDp(minCurrentWidthDp)
                .setMinSmallestWidthDp(minSmallestWidthDp)
                .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
                .setDefaultSplitAttributes(splitAttributes)
                .setFinishPrimaryWithSecondary(SplitRule.FinishBehavior.ADJACENT)
                .setFinishSecondaryWithPrimary(SplitRule.FinishBehavior.ADJACENT)
                .setClearTop(true)
                .setTag(TAG_PAIR)
                .build();

        Set<ActivityFilter> homeFilters = new HashSet<>();
        homeFilters.add(new ActivityFilter(homeComponent,
                UltimateSettingsActivity.ACTION_OPEN));
        Intent placeholderIntent = UltimateSettingsActivity
                .createDefaultSubpageIntent(appContext);
        SplitPlaceholderRule placeholderRule = new SplitPlaceholderRule.Builder(
                homeFilters, placeholderIntent)
                .setMinWidthDp(minCurrentWidthDp)
                .setMinSmallestWidthDp(minSmallestWidthDp)
                .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
                .setDefaultSplitAttributes(splitAttributes)
                .setFinishPrimaryWithPlaceholder(SplitRule.FinishBehavior.ADJACENT)
                .setSticky(true)
                .setTag(TAG_PLACEHOLDER)
                .build();

        Set<ActivityFilter> alwaysExpandFilters = new HashSet<>();
        alwaysExpandFilters.add(new ActivityFilter(new ComponentName(
                appContext, AlarmRingingActivity.class), null));
        ActivityRule alwaysExpandRule = new ActivityRule.Builder(alwaysExpandFilters)
                .setAlwaysExpand(true)
                .setTag(TAG_ALWAYS_EXPAND)
                .build();

        RuleController ruleController = RuleController.getInstance(appContext);
        ruleController.addRule(alwaysExpandRule);
        ruleController.addRule(placeholderRule);
        ruleController.addRule(pairRule);
    }
}
