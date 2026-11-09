package com.example.vpn.repo

import com.example.vpn.db.BlockedDomainEntity
import com.example.vpn.db.CategoryEntity

object StarterWebsiteBlocklist {

    val STARTER_CATEGORIES = listOf(
        CategoryEntity(
            name = "Social Media",
            description = "Social networks, feeds, short video clips and chats",
            iconName = "social",
            colorHex = 0xFF26E5B0, // Teal
            isEnabled = true,
            isCustom = false,
            domainCount = 12
        ),
        CategoryEntity(
            name = "Video Streaming",
            description = "Long and short-form video streaming platforms",
            iconName = "video",
            colorHex = 0xFF3B82F6, // Blue
            isEnabled = true,
            isCustom = false,
            domainCount = 10
        ),
        CategoryEntity(
            name = "Entertainment",
            description = "Memes, music, pop culture and media aggregators",
            iconName = "entertainment",
            colorHex = 0xFFB388FF, // Purple
            isEnabled = false,
            isCustom = false,
            domainCount = 8
        ),
        CategoryEntity(
            name = "Gaming",
            description = "Online games, distribution stores and esports streams",
            iconName = "gaming",
            colorHex = 0xFFF59E0B, // Amber
            isEnabled = false,
            isCustom = false,
            domainCount = 10
        ),
        CategoryEntity(
            name = "Shopping",
            description = "E-commerce stores and consumer retail marketplaces",
            iconName = "shopping",
            colorHex = 0xFFFF7043, // Coral
            isEnabled = false,
            isCustom = false,
            domainCount = 10
        ),
        CategoryEntity(
            name = "News",
            description = "Breaking news, political feeds and tabloid sites",
            iconName = "news",
            colorHex = 0xFF64B5F6, // Light Blue
            isEnabled = false,
            isCustom = false,
            domainCount = 9
        ),
        CategoryEntity(
            name = "Adult Content",
            description = "Explicit adult materials and webcam websites",
            iconName = "adult",
            colorHex = 0xFFFF5252, // Red
            isEnabled = true,
            isCustom = false,
            domainCount = 7
        ),
        CategoryEntity(
            name = "Gambling",
            description = "Online casinos, sports betting and lottery platforms",
            iconName = "gambling",
            colorHex = 0xFFFFD700, // Gold
            isEnabled = true,
            isCustom = false,
            domainCount = 8
        ),
        CategoryEntity(
            name = "Custom Websites",
            description = "Your personal custom blocked web domains",
            iconName = "custom",
            colorHex = 0xFF00E5FF, // Cyan
            isEnabled = true,
            isCustom = false,
            domainCount = 0
        )
    )

    val STARTER_DOMAINS = listOf(
        // Social Media
        BlockedDomainEntity("instagram.com", "Social Media", isFavorite = true),
        BlockedDomainEntity("facebook.com", "Social Media"),
        BlockedDomainEntity("twitter.com", "Social Media", isFavorite = true),
        BlockedDomainEntity("x.com", "Social Media", isFavorite = true),
        BlockedDomainEntity("tiktok.com", "Social Media", isFavorite = true),
        BlockedDomainEntity("reddit.com", "Social Media", isFavorite = true),
        BlockedDomainEntity("snapchat.com", "Social Media"),
        BlockedDomainEntity("linkedin.com", "Social Media"),
        BlockedDomainEntity("pinterest.com", "Social Media"),
        BlockedDomainEntity("threads.net", "Social Media"),
        BlockedDomainEntity("tumblr.com", "Social Media"),
        BlockedDomainEntity("discord.com", "Social Media"),

        // Video Streaming
        BlockedDomainEntity("youtube.com", "Video Streaming", isFavorite = true),
        BlockedDomainEntity("netflix.com", "Video Streaming"),
        BlockedDomainEntity("primevideo.com", "Video Streaming"),
        BlockedDomainEntity("hulu.com", "Video Streaming"),
        BlockedDomainEntity("disneyplus.com", "Video Streaming"),
        BlockedDomainEntity("twitch.tv", "Video Streaming", isFavorite = true),
        BlockedDomainEntity("dailymotion.com", "Video Streaming"),
        BlockedDomainEntity("vimeo.com", "Video Streaming"),
        BlockedDomainEntity("crunchyroll.com", "Video Streaming"),
        BlockedDomainEntity("hotstar.com", "Video Streaming"),

        // Entertainment
        BlockedDomainEntity("buzzfeed.com", "Entertainment"),
        BlockedDomainEntity("9gag.com", "Entertainment"),
        BlockedDomainEntity("boredpanda.com", "Entertainment"),
        BlockedDomainEntity("imgur.com", "Entertainment"),
        BlockedDomainEntity("fandom.com", "Entertainment"),
        BlockedDomainEntity("spotify.com", "Entertainment"),
        BlockedDomainEntity("soundcloud.com", "Entertainment"),
        BlockedDomainEntity("mashable.com", "Entertainment"),

        // Gaming
        BlockedDomainEntity("steamcommunity.com", "Gaming"),
        BlockedDomainEntity("steampowered.com", "Gaming"),
        BlockedDomainEntity("epicgames.com", "Gaming"),
        BlockedDomainEntity("roblox.com", "Gaming"),
        BlockedDomainEntity("battlenet.com", "Gaming"),
        BlockedDomainEntity("riotgames.com", "Gaming"),
        BlockedDomainEntity("ea.com", "Gaming"),
        BlockedDomainEntity("playstation.com", "Gaming"),
        BlockedDomainEntity("xbox.com", "Gaming"),
        BlockedDomainEntity("ign.com", "Gaming"),

        // Shopping
        BlockedDomainEntity("amazon.com", "Shopping"),
        BlockedDomainEntity("ebay.com", "Shopping"),
        BlockedDomainEntity("aliexpress.com", "Shopping"),
        BlockedDomainEntity("walmart.com", "Shopping"),
        BlockedDomainEntity("target.com", "Shopping"),
        BlockedDomainEntity("shein.com", "Shopping"),
        BlockedDomainEntity("temu.com", "Shopping"),
        BlockedDomainEntity("etsy.com", "Shopping"),
        BlockedDomainEntity("flipkart.com", "Shopping"),
        BlockedDomainEntity("bestbuy.com", "Shopping"),

        // News
        BlockedDomainEntity("cnn.com", "News"),
        BlockedDomainEntity("bbc.com", "News"),
        BlockedDomainEntity("foxnews.com", "News"),
        BlockedDomainEntity("dailymail.co.uk", "News"),
        BlockedDomainEntity("nytimes.com", "News"),
        BlockedDomainEntity("theguardian.com", "News"),
        BlockedDomainEntity("forbes.com", "News"),
        BlockedDomainEntity("huffpost.com", "News"),
        BlockedDomainEntity("reuters.com", "News"),

        // Adult Content
        BlockedDomainEntity("pornhub.com", "Adult Content"),
        BlockedDomainEntity("xvideos.com", "Adult Content"),
        BlockedDomainEntity("xnxx.com", "Adult Content"),
        BlockedDomainEntity("onlyfans.com", "Adult Content"),
        BlockedDomainEntity("chaturbate.com", "Adult Content"),
        BlockedDomainEntity("redtube.com", "Adult Content"),
        BlockedDomainEntity("youporn.com", "Adult Content"),

        // Gambling
        BlockedDomainEntity("bet365.com", "Gambling"),
        BlockedDomainEntity("stake.com", "Gambling"),
        BlockedDomainEntity("draftkings.com", "Gambling"),
        BlockedDomainEntity("fanduel.com", "Gambling"),
        BlockedDomainEntity("pokerstars.com", "Gambling"),
        BlockedDomainEntity("betway.com", "Gambling"),
        BlockedDomainEntity("bovada.lv", "Gambling"),
        BlockedDomainEntity("888casino.com", "Gambling")
    )
}
