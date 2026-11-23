package com.selfuse.timesquest.data

/**
 * Static game catalog: all 9 fantasy islands (36 stages), 18 bestiary monsters,
 * equipment shop items, and canonical Chinese multiplication rhymes.
 */
object CurriculumCatalog {

    private val CHINESE_DIGITS = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")

    /**
     * Standard 2nd-grade Chinese multiplication table rhyme, always ordered small x large:
     * e.g. (3, 7) -> "三七二十一", (2, 3) -> "二三得六", (1, 1) -> "一一得一", (2, 5) -> "二五一十".
     */
    fun chineseRhyme(a: Int, b: Int): String {
        val lo = minOf(a, b).coerceIn(1, 9)
        val hi = maxOf(a, b).coerceIn(1, 9)
        val p = lo * hi
        val prefix = "${CHINESE_DIGITS[lo]}${CHINESE_DIGITS[hi]}"
        return when {
            p < 10 -> "${prefix}得${CHINESE_DIGITS[p]}"
            p == 10 -> "${prefix}一十"
            p < 20 -> "${prefix}十${CHINESE_DIGITS[p % 10]}"
            p % 10 == 0 -> "${prefix}${CHINESE_DIGITS[p / 10]}十"
            else -> "${prefix}${CHINESE_DIGITS[p / 10]}十${CHINESE_DIGITS[p % 10]}"
        }
    }

    /** Natural spoken Chinese question for TTS. */
    fun spokenQuestion(a: Int, b: Int): String = "${CHINESE_DIGITS[a]}乘${CHINESE_DIGITS[b]}等于几？"

    /** Natural spoken Chinese missing-factor question for TTS. */
    fun spokenMissingFactor(known: Int, product: Int, hideLeft: Boolean): String {
        return if (hideLeft) {
            "几乘${CHINESE_DIGITS[known]}等于${spokenNumber(product)}？"
        } else {
            "${CHINESE_DIGITS[known]}乘几等于${spokenNumber(product)}？"
        }
    }

    fun spokenNumber(n: Int): String = when {
        n < 10 -> CHINESE_DIGITS[n.coerceIn(0, 9)]
        n == 10 -> "十"
        n < 20 -> "十${CHINESE_DIGITS[n % 10]}"
        n % 10 == 0 -> "${CHINESE_DIGITS[n / 10]}十"
        else -> "${CHINESE_DIGITS[n / 10]}十${CHINESE_DIGITS[n % 10]}"
    }

    val allFacts45: List<FactKey> by lazy {
        buildList {
            for (i in 1..9) {
                for (j in i..9) {
                    add(FactKey(i, j))
                }
            }
        }
    }

    val monsters: List<MonsterDef> = listOf(
        // Island 1 - Emerald Slime Meadow (1, 2, 5)
        MonsterDef(
            id = "m_slime_scout",
            name = "果冻小绿",
            title = "草地小史莱姆",
            islandId = 1,
            isBoss = false,
            spriteKey = "boss_slime",
            accentHex = 0xFF43A047,
            flavorText = "最爱在太阳下跳来跳去，听到「二五一十」就会原地打转！",
            weakFactHint = "2 和 5 的口诀"
        ),
        MonsterDef(
            id = "boss_slime",
            name = "蹦蹦草帽史莱姆王",
            title = "翡翠草地岛主",
            islandId = 1,
            isBoss = true,
            spriteKey = "boss_slime",
            accentHex = 0xFF2E7D32,
            flavorText = "戴着大草帽和小皇冠的软萌大王，最怕小勇者一口气背完 2 和 5！",
            weakFactHint = "五五二十五、二九十八"
        ),

        // Island 2 - Mushroom Forest (3)
        MonsterDef(
            id = "m_mushroom_scout",
            name = "叮咚小菇",
            title = "荧光蘑菇精灵",
            islandId = 2,
            isBoss = false,
            spriteKey = "boss_mushroom",
            accentHex = 0xFFEC407A,
            flavorText = "头顶会发出彩色的光，每答对一道 3 的口诀就会羞红脸。",
            weakFactHint = "3 的口诀"
        ),
        MonsterDef(
            id = "boss_mushroom",
            name = "三头咕噜菇大王",
            title = "蘑菇森林岛主",
            islandId = 2,
            isBoss = true,
            spriteKey = "boss_mushroom",
            accentHex = 0xFFD81B60,
            flavorText = "身边总跟着两朵迷你小蘑菇，喊出「三七二十一」就能让它们一起鼓掌！",
            weakFactHint = "三七二十一、三八二十四"
        ),

        // Island 3 - Golden Sand Canyon (4)
        MonsterDef(
            id = "m_golem_scout",
            name = "沙砾小砖",
            title = "峡谷石块宝宝",
            islandId = 3,
            isBoss = false,
            spriteKey = "boss_golem",
            accentHex = 0xFFFFA000,
            flavorText = "喜欢把自己堆成四四方方的城堡，怕极了破盾逆向咒。",
            weakFactHint = "4 的口诀"
        ),
        MonsterDef(
            id = "boss_golem",
            name = "四臂沙石傀儡",
            title = "流沙峡谷岛主",
            islandId = 3,
            isBoss = true,
            spriteKey = "boss_golem",
            accentHex = 0xFFEF6C00,
            flavorText = "虽然有四条粗壮的手臂，但只要算清「四八三十二」，护盾就会咔嚓碎掉！",
            weakFactHint = "四七二十八、四九三十六"
        ),

        // Island 4 - Coral Bubble Bay (2~5 mix)
        MonsterDef(
            id = "m_octopus_scout",
            name = "吐泡小章",
            title = "海湾泡泡水手",
            islandId = 4,
            isBoss = false,
            spriteKey = "boss_octopus",
            accentHex = 0xFF00ACC1,
            flavorText = "整天吹出藏着算式的彩色泡泡，专挑粗心大意的小冒险家。",
            weakFactHint = "2~5 综合与同积口诀"
        ),
        MonsterDef(
            id = "boss_octopus",
            name = "泡泡章鱼船长",
            title = "泡泡海湾岛主",
            islandId = 4,
            isBoss = true,
            spriteKey = "boss_octopus",
            accentHex = 0xFF00838F,
            flavorText = "海湾最调皮的船长！最喜欢考大家「哪些口诀都等于 12 或 20」。",
            weakFactHint = "三四十二、四五二十"
        ),

        // Island 5 - Amethyst Crystal Cavern (6)
        MonsterDef(
            id = "m_crystal_scout",
            name = "紫晶小棱",
            title = "矿洞晶石小兵",
            islandId = 5,
            isBoss = false,
            spriteKey = "boss_crystal",
            accentHex = 0xFF8E24AA,
            flavorText = "身上闪着六边形紫光，只有精准的 6 的口诀能敲开它的外壳。",
            weakFactHint = "6 的口诀"
        ),
        MonsterDef(
            id = "boss_crystal",
            name = "六角水晶巨像",
            title = "水晶矿洞岛主",
            islandId = 5,
            isBoss = true,
            spriteKey = "boss_crystal",
            accentHex = 0xFF6A1B9A,
            flavorText = "守护矿洞宝藏的大块头，一听到「六七四十二」「六八四十八」就浑身发抖！",
            weakFactHint = "六七四十二、六八四十八"
        ),

        // Island 6 - Rainbow Frost Mountain (7)
        MonsterDef(
            id = "m_yeti_scout",
            name = "雪球团子",
            title = "雪山绒毛小怪",
            islandId = 6,
            isBoss = false,
            spriteKey = "boss_yeti",
            accentHex = 0xFF039BE5,
            flavorText = "圆滚滚像个冰淇淋球，最爱把 7 的口诀藏在雪堆里。",
            weakFactHint = "7 的口诀"
        ),
        MonsterDef(
            id = "boss_yeti",
            name = "七彩雪怪领主",
            title = "雷鸣雪山岛主",
            islandId = 6,
            isBoss = true,
            spriteKey = "boss_yeti",
            accentHex = 0xFF0277BD,
            flavorText = "戴着红金围巾的温柔大雪怪，只要能稳稳答出「七八五十六」「七九六十三」就会请你吃糖霜！",
            weakFactHint = "七七四十九、七八五十六、七九六十三"
        ),

        // Island 7 - Lava Pancake Volcano (8)
        MonsterDef(
            id = "m_crab_scout",
            name = "火钳小蟹",
            title = "火山泡泡蟹兵",
            islandId = 7,
            isBoss = false,
            spriteKey = "boss_crab",
            accentHex = 0xFFFF5722,
            flavorText = "两只大钳子咔嚓响，横着走的时候嘴里念叨着 8 的乘法。",
            weakFactHint = "8 的口诀"
        ),
        MonsterDef(
            id = "boss_crab",
            name = "八爪炎魔蟹皇",
            title = "熔岩火山岛主",
            islandId = 7,
            isBoss = true,
            spriteKey = "boss_crab",
            accentHex = 0xFFD84315,
            flavorText = "火山口最拉风的烤饼大厨，专治分不清「六八四十八」和「八九七十二」的小迷糊！",
            weakFactHint = "八八六十四、八九七十二"
        ),

        // Island 8 - Nebula Clockwork Castle (9)
        MonsterDef(
            id = "m_cat_scout",
            name = "星尘咪咪",
            title = "古堡星光小猫",
            islandId = 8,
            isBoss = false,
            spriteKey = "boss_cat",
            accentHex = 0xFF5E35B1,
            flavorText = "眨眼时会掉出金色小星星，它偷偷告诉你：9 的口诀十位和个位加起来总是 9！",
            weakFactHint = "9 的口诀"
        ),
        MonsterDef(
            id = "boss_cat",
            name = "九尾星光暗影猫",
            title = "星云古堡岛主",
            islandId = 8,
            isBoss = true,
            spriteKey = "boss_cat",
            accentHex = 0xFF4527A0,
            flavorText = "拥有九条银河尾巴的神秘大魔法师，只有用符文键盘亲自打出「九九八十一」才能收服它！",
            weakFactHint = "七九六十三、八九七十二、九九八十一"
        ),

        // Island 9 - Dragon Cloud Summit (1~9 Mastery)
        MonsterDef(
            id = "m_dragon_scout",
            name = "云端龙卫",
            title = "天空守卫小龙",
            islandId = 9,
            isBoss = false,
            spriteKey = "boss_dragon",
            accentHex = 0xFFE53935,
            flavorText = "飞在彩虹云层里的龙族守卫，会用全部 1~9 口诀检验勇者的实力。",
            weakFactHint = "1~9 全部口诀"
        ),
        MonsterDef(
            id = "boss_dragon",
            name = "算术喷火萌龙王",
            title = "九九终极守护龙",
            islandId = 9,
            isBoss = true,
            spriteKey = "boss_dragon",
            accentHex = 0xFFC62828,
            flavorText = "口诀王国最威风也最可爱的传奇萌龙王！打败它就能获得「九九大勇者」至高荣耀！",
            weakFactHint = "全表薄弱口诀终极考"
        )
    )

    private val monstersById: Map<String, MonsterDef> = monsters.associateBy { it.id }

    fun monster(id: String): MonsterDef = monstersById[id] ?: monsters.first()

    val islands: List<IslandDef> = listOf(
        buildIsland(
            id = 1,
            name = "翡翠史莱姆草地",
            subtitle = "轻松启航 · 1、2、5 的口诀",
            badgeText = "1·2·5 口诀",
            targetTables = listOf(1, 2, 5),
            scoutMonsterId = "m_slime_scout",
            bossId = "boss_slime",
            theme = IslandTheme(0xFF7AD7F0, 0xFFD9F7E8, 0xFF81C784, 0xFF4CAF50, 0xFF2E7D32)
        ),
        buildIsland(
            id = 2,
            name = "荧光蘑菇森林",
            subtitle = "连击奥义 · 3 的口诀",
            badgeText = "3 的口诀",
            targetTables = listOf(3),
            scoutMonsterId = "m_mushroom_scout",
            bossId = "boss_mushroom",
            theme = IslandTheme(0xFF8C9EFF, 0xFFFCE4EC, 0xFF80CBC4, 0xFF26A69A, 0xFFD81B60)
        ),
        buildIsland(
            id = 3,
            name = "黄金流沙峡谷",
            subtitle = "破盾逆向 · 4 的口诀",
            badgeText = "4 的口诀",
            targetTables = listOf(4),
            scoutMonsterId = "m_golem_scout",
            bossId = "boss_golem",
            theme = IslandTheme(0xFFFFCA61, 0xFFFFF3D1, 0xFFFFB74D, 0xFFE69A3A, 0xFFEF6C00)
        ),
        buildIsland(
            id = 4,
            name = "珊瑚泡泡海湾",
            subtitle = "趣味合击 · 2~5 综合特训",
            badgeText = "2~5 综合",
            targetTables = listOf(2, 3, 4, 5),
            scoutMonsterId = "m_octopus_scout",
            bossId = "boss_octopus",
            theme = IslandTheme(0xFF4FC3F7, 0xFFE0F7FA, 0xFF4DD0E1, 0xFF26C6DA, 0xFF00838F)
        ),
        buildIsland(
            id = 5,
            name = "紫晶魔法矿洞",
            subtitle = "进阶攻坚 · 6 的口诀",
            badgeText = "6 的口诀",
            targetTables = listOf(6),
            scoutMonsterId = "m_crystal_scout",
            bossId = "boss_crystal",
            theme = IslandTheme(0xFF9575CD, 0xFFEDE7F6, 0xFF7E57C2, 0xFF5E35B1, 0xFF6A1B9A)
        ),
        buildIsland(
            id = 6,
            name = "雷鸣糖霜雪山",
            subtitle = "难点突破 · 7 的口诀",
            badgeText = "7 的口诀",
            targetTables = listOf(7),
            scoutMonsterId = "m_yeti_scout",
            bossId = "boss_yeti",
            theme = IslandTheme(0xFF64B5F6, 0xFFE3F2FD, 0xFF90CAF9, 0xFF42A5F5, 0xFF0277BD)
        ),
        buildIsland(
            id = 7,
            name = "熔岩烤饼火山",
            subtitle = "火力全开 · 8 的口诀",
            badgeText = "8 的口诀",
            targetTables = listOf(8),
            scoutMonsterId = "m_crab_scout",
            bossId = "boss_crab",
            theme = IslandTheme(0xFFFF8A65, 0xFFFFE0B2, 0xFFFF7043, 0xFFD84315, 0xFFBF360C)
        ),
        buildIsland(
            id = 8,
            name = "星云时钟古堡",
            subtitle = "符文精准 · 9 的口诀",
            badgeText = "9 的口诀",
            targetTables = listOf(9),
            scoutMonsterId = "m_cat_scout",
            bossId = "boss_cat",
            theme = IslandTheme(0xFF5C6BC0, 0xFFD1C4E9, 0xFF4527A0, 0xFF311B92, 0xFF512DA8)
        ),
        buildIsland(
            id = 9,
            name = "巨龙云端之巅",
            subtitle = "巅峰决战 · 1~9 全口诀",
            badgeText = "1~9 终极",
            targetTables = listOf(2, 3, 4, 5, 6, 7, 8, 9),
            scoutMonsterId = "m_dragon_scout",
            bossId = "boss_dragon",
            theme = IslandTheme(0xFFFFB74D, 0xFFFFF8E1, 0xFFFF8A65, 0xFFE53935, 0xFFC62828)
        )
    )

    val allLevels: List<LevelDef> = islands.flatMap { it.levels }
    private val levelsById: Map<String, LevelDef> = allLevels.associateBy { it.id }

    fun island(id: Int): IslandDef = islands.firstOrNull { it.id == id } ?: islands.first()

    fun level(id: String): LevelDef = levelsById[id] ?: islands.first().levels.first()

    val endlessTowerLevel = LevelDef(
        id = "endless_tower",
        islandId = 9,
        stageIndex = 5,
        title = "无尽勇者塔",
        subtitle = "全口诀连战到底，刷新最高纪录！",
        stageType = StageType.ENDLESS,
        monsterId = "boss_dragon",
        questionCount = 24,
        monsterMaxHp = 600,
        baseCoins = 80,
        baseXp = 120
    )

    private fun buildIsland(
        id: Int,
        name: String,
        subtitle: String,
        badgeText: String,
        targetTables: List<Int>,
        scoutMonsterId: String,
        bossId: String,
        theme: IslandTheme
    ): IslandDef {
        val baseHp = 120 + (id - 1) * 20
        val levels = listOf(
            LevelDef(
                id = "i${id}_s1",
                islandId = id,
                stageIndex = 1,
                title = "第 1 关 · 魔法侦察",
                subtitle = "极速施法热身，打出神速暴击！",
                stageType = StageType.SCOUT,
                monsterId = scoutMonsterId,
                questionCount = 8,
                monsterMaxHp = baseHp,
                baseCoins = 30 + id * 4,
                baseXp = 45 + id * 5
            ),
            LevelDef(
                id = "i${id}_s2",
                islandId = id,
                stageIndex = 2,
                title = "第 2 关 · 破盾精英",
                subtitle = "识破缺项魔法护盾，粉碎防御！",
                stageType = StageType.SHIELD_ELITE,
                monsterId = scoutMonsterId,
                questionCount = 9,
                monsterMaxHp = baseHp + 35,
                baseCoins = 38 + id * 5,
                baseXp = 55 + id * 6
            ),
            LevelDef(
                id = "i${id}_s3",
                islandId = id,
                stageIndex = 3,
                title = "第 3 关 · 奇趣特训",
                subtitle = "泡泡连消、双怪比大小与符文手输！",
                stageType = StageType.MAGIC_TRIAL,
                monsterId = scoutMonsterId,
                questionCount = 9,
                monsterMaxHp = baseHp + 45,
                baseCoins = 42 + id * 5,
                baseXp = 60 + id * 6
            ),
            LevelDef(
                id = "i${id}_s4",
                islandId = id,
                stageIndex = 4,
                title = "第 4 关 · 岛主决战",
                subtitle = "迎战 ${monster(bossId).name}，看准红光弹反大招！",
                stageType = StageType.BOSS,
                monsterId = bossId,
                questionCount = 11,
                monsterMaxHp = baseHp + 90,
                baseCoins = 65 + id * 8,
                baseXp = 90 + id * 10
            )
        )
        return IslandDef(
            id = id,
            name = name,
            subtitle = subtitle,
            badgeText = badgeText,
            targetTables = targetTables,
            bossId = bossId,
            theme = theme,
            levels = levels
        )
    }

    val equipments: List<EquipmentDef> = listOf(
        // Wands (change spell visuals + tiny attack flair)
        EquipmentDef(
            id = "wand_wood",
            slot = EquipSlot.WAND,
            name = "森叶芽法杖",
            description = "见习勇者的好伙伴，射出清新翠绿的叶光魔法弹。",
            priceCoins = 0,
            accentHex = 0xFF4CAF50,
            element = ElementStyle.WOOD
        ),
        EquipmentDef(
            id = "wand_fire",
            slot = EquipSlot.WAND,
            name = "烈焰火球权杖",
            description = "炽热流星火球！命中怪物炸开金色火花。",
            priceCoins = 120,
            accentHex = 0xFFFF5722,
            element = ElementStyle.FIRE
        ),
        EquipmentDef(
            id = "wand_ice",
            slot = EquipSlot.WAND,
            name = "极光冰晶法杖",
            description = "晶莹冰霜飞弹，命中绽放六角雪花粒子！",
            priceCoins = 180,
            accentHex = 0xFF29B6F6,
            element = ElementStyle.ICE
        ),
        EquipmentDef(
            id = "wand_lightning",
            slot = EquipSlot.WAND,
            name = "雷霆闪电神杖",
            description = "一道金色闪电劈向怪物，暴击视觉拉满！",
            priceCoins = 260,
            accentHex = 0xFFFFC107,
            element = ElementStyle.LIGHTNING
        ),
        EquipmentDef(
            id = "wand_rainbow",
            slot = EquipSlot.WAND,
            name = "星辰彩虹魔杖",
            description = "传说级七彩流星光束！连续签到 7 天也可免费领取。",
            priceCoins = 380,
            accentHex = 0xFFAB47BC,
            element = ElementStyle.RAINBOW,
            streakUnlockDays = 7
        ),

        // Hats / Outfits (shown on hero badge & battle stage)
        EquipmentDef(
            id = "hat_apprentice",
            slot = EquipSlot.HAT,
            name = "星光魔法帽",
            description = "经典勇者学徒尖顶帽，自带幸运小星星。",
            priceCoins = 0,
            accentHex = 0xFF3F51B5
        ),
        EquipmentDef(
            id = "hat_dino",
            slot = EquipSlot.HAT,
            name = "小恐龙连帽兜",
            description = "超萌绿色恐龙锯齿帽兜，穿上气势翻倍！",
            priceCoins = 140,
            accentHex = 0xFF43A047
        ),
        EquipmentDef(
            id = "hat_astro",
            slot = EquipSlot.HAT,
            name = "星际宇航头盔",
            description = "来自未来算术星系的酷炫透明宇航头盔。",
            priceCoins = 220,
            accentHex = 0xFF00ACC1
        ),
        EquipmentDef(
            id = "hat_crown",
            slot = EquipSlot.HAT,
            name = "九九黄金皇冠",
            description = "闪闪发光的冠军小皇冠，口诀大王专属！",
            priceCoins = 350,
            accentHex = 0xFFFFB300
        ),

        // Companion Pets (battle alongside hero & auto-assist every 4 combo!)
        EquipmentDef(
            id = "pet_dragon",
            slot = EquipSlot.PET,
            name = "火球小龙 · 噗噗",
            description = "每达成 4 连击，噗噗自动吐出一颗小火球帮你补刀！",
            priceCoins = 150,
            accentHex = 0xFFFF7043,
            element = ElementStyle.FIRE,
            spriteKey = "pet_dragon"
        ),
        EquipmentDef(
            id = "pet_otter",
            slot = EquipSlot.PET,
            name = "泡泡水獭 · 波波",
            description = "爱吐亮晶晶水泡的小水獭，连签 3 天可免费领养！",
            priceCoins = 180,
            accentHex = 0xFF29B6F6,
            element = ElementStyle.ICE,
            spriteKey = "pet_otter",
            streakUnlockDays = 3
        ),
        EquipmentDef(
            id = "pet_cat",
            slot = EquipSlot.PET,
            name = "雷电小猫 · 咪呜",
            description = "每 4 连击放出闪电爪击，还会给小勇者喵喵加油！",
            priceCoins = 240,
            accentHex = 0xFFFFC107,
            element = ElementStyle.LIGHTNING,
            spriteKey = "pet_cat"
        ),
        EquipmentDef(
            id = "pet_owl",
            slot = EquipSlot.PET,
            name = "星光猫头鹰 · 咕咕",
            description = "博学的星辰小伙伴，每 4 连击降下星光射线！",
            priceCoins = 300,
            accentHex = 0xFF7E57C2,
            element = ElementStyle.RAINBOW,
            spriteKey = "pet_owl"
        )
    )

    private val equipById: Map<String, EquipmentDef> = equipments.associateBy { it.id }

    fun equipment(id: String?): EquipmentDef? = id?.let { equipById[it] }

    fun wand(id: String): EquipmentDef = equipById[id] ?: equipments.first()

    fun createDefaultDailyQuests(): List<DailyQuestItem> = listOf(
        DailyQuestItem(
            id = "q_clear_1",
            title = "完成任意 1 场冒险战斗",
            target = 1,
            rewardCoins = 40
        ),
        DailyQuestItem(
            id = "q_combo_5",
            title = "在战斗中打出 1 次 5 连击",
            target = 1,
            rewardCoins = 50
        ),
        DailyQuestItem(
            id = "q_correct_15",
            title = "累计答对 15 道口诀魔法",
            target = 15,
            rewardCoins = 60
        )
    )
}
