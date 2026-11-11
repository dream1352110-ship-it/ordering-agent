package com.sm.orderingagent.domain

object DemoMenu {
    val categories = listOf("全部", "主食", "轻食", "小食", "烘焙", "咖啡", "茶饮", "冷饮")
    private val size = OptionGroup("size", "杯型", listOf(Choice("medium", "中杯"), Choice("large", "大杯", 400)), "medium")
    private val temperature = OptionGroup("temperature", "温度", listOf(Choice("hot", "热"), Choice("iced", "冰")), "hot")
    private val sugar = OptionGroup("sugar", "甜度", listOf(Choice("none", "无糖"), Choice("half", "半糖"), Choice("normal", "标准糖")), "none")
    private val drinkOptions = listOf(size, temperature, sugar)
    private val iced = OptionGroup("temperature", "温度", listOf(Choice("iced", "冰")), "iced")
    private val icedOptions = listOf(size, iced, sugar)
    private val sweetened = OptionGroup("sugar", "额外加糖", listOf(Choice("none", "不额外加糖"), Choice("half", "少量加糖")), "none")
    private val milkTeaOptions = listOf(size, temperature, sweetened)

    val items = listOf(
        MenuItem("beef-burger", "经典牛肉汉堡", "主食", 3200, "burger", "炙烤牛肉搭配芝士、生菜与番茄，饱满的一餐。", "牛肉、面包、芝士、生菜、番茄、蛋黄酱", "单人份 · 约280g", setOf(Allergen.WHEAT, Allergen.MILK, Allergen.EGG), setOf(Allergen.PEANUT), stock = 12),
        MenuItem("steak", "香烤牛排配土豆", "主食", 6800, "steak", "切片牛排配烤土豆与迷迭香，固定熟度出品。", "牛肉、土豆、迷迭香、黄油", "单人份 · 约350g", setOf(Allergen.MILK), stock = 8),
        MenuItem("meatballs", "瑞典风味肉丸", "主食", 3600, "meatballs", "肉丸搭配浓郁酱汁，适合搭配轻食分享。", "牛肉、猪肉、面包糠、奶油、鸡蛋", "单人份 · 约260g", setOf(Allergen.WHEAT, Allergen.MILK, Allergen.EGG), stock = 10),
        MenuItem("garden-salad", "缤纷田园沙拉", "轻食", 2400, "salad", "新鲜蔬果配独立包装酱汁，清爽轻盈。", "生菜、番茄、水果、沙拉酱", "单人份 · 约220g", setOf(Allergen.EGG), allergenInfoComplete = false, stock = 10),
        MenuItem("salmon-salad", "三文鱼沙拉", "轻食", 3800, "salmon", "三文鱼搭配时蔬，轻食也有满足感。", "三文鱼、生菜、番茄、柠檬、橄榄油", "单人份 · 约240g", setOf(Allergen.FISH), stock = 8),
        MenuItem("fries", "金黄薯条", "小食", 1600, "fries", "外脆里软的薯条，适合佐餐分享。", "土豆、植物油、盐", "分享份 · 约180g", mayContain = setOf(Allergen.WHEAT), stock = 20),
        MenuItem("wings", "奥尔良风味鸡翅", "小食", 2200, "wings", "甜辣风味烤翅，四只装。", "鸡翅、酱油、蜂蜜、香辛料", "4只 · 微辣", setOf(Allergen.SOY, Allergen.WHEAT), stock = 12),
        MenuItem("mash", "奶香土豆泥", "小食", 1400, "mash", "细腻土豆泥，奶香柔和。", "土豆、牛奶、黄油", "单人份 · 约160g", setOf(Allergen.MILK), stock = 0),
        MenuItem("latte", "醇香拿铁", "咖啡", 2200, "latte", "浓缩咖啡与牛奶的柔和搭配，含咖啡因。", "咖啡、牛奶；糖按规格添加", "中杯300ml / 大杯400ml", setOf(Allergen.MILK), options = drinkOptions),
        MenuItem("americano", "经典美式", "咖啡", 1800, "americano", "清晰醇厚的咖啡香气，含咖啡因。", "咖啡、水；糖按规格添加", "中杯300ml / 大杯400ml", options = drinkOptions),
        MenuItem("milk-tea", "珍珠奶茶", "茶饮", 2000, "milk_tea", "奶茶与珍珠的经典组合，含咖啡因。无额外糖仍含珍珠原有糖。", "茶、牛奶、木薯珍珠；糖按规格添加", "中杯400ml / 大杯500ml", setOf(Allergen.MILK), options = drinkOptions),
        MenuItem("matcha", "抹茶拿铁", "茶饮", 2400, "matcha", "抹茶与牛奶的清香组合，含咖啡因。", "抹茶、牛奶；糖按规格添加", "中杯300ml / 大杯400ml", setOf(Allergen.MILK), options = drinkOptions),
        MenuItem("fish-chips", "炸鱼薯条", "主食", 3800, "fish_chips", "酥炸鱼块搭配薯条，适合一人享用。", "鱼肉、小麦面粉、鸡蛋、土豆、植物油", "单人份 · 约320g", setOf(Allergen.FISH, Allergen.WHEAT, Allergen.EGG), allergenInfoComplete = false, stock = 10),
        MenuItem("sausage-platter", "香肠冷切拼盘", "小食", 4200, "sausage_platter", "多种肉类冷切与香肠组合，适合分享。", "香肠、冷切肉、配菜；加工配料尚未完整列明", "分享份 · 约280g", setOf(Allergen.SOY), allergenInfoComplete = false, stock = 8),
        MenuItem("bread", "餐前面包", "烘焙", 1200, "bread", "佐餐面包，搭配主食或热饮。", "小麦面粉、酵母、黄油、牛奶", "一份 · 约100g", setOf(Allergen.WHEAT, Allergen.MILK), allergenInfoComplete = false, stock = 20),
        MenuItem("cinnamon-roll", "肉桂卷", "烘焙", 1600, "cinnamon_roll", "柔软面包卷搭配肉桂糖香。", "小麦面粉、牛奶、鸡蛋、黄油、肉桂、糖", "1个 · 约100g", setOf(Allergen.WHEAT, Allergen.MILK, Allergen.EGG), allergenInfoComplete = false, stock = 12),
        MenuItem("scone", "烘焙甜点", "烘焙", 1400, "scone", "独立份烘焙甜点，搭配咖啡享用；具体夹料需确认。", "小麦面粉、黄油、牛奶、鸡蛋、糖；夹料未核实", "1份 · 约90g", setOf(Allergen.WHEAT, Allergen.MILK, Allergen.EGG), allergenInfoComplete = false, stock = 12),
        MenuItem("bagel", "原味贝果", "烘焙", 1400, "bagel", "有嚼劲的环形面包，按单份计价。", "小麦面粉、酵母、水、糖、盐", "1个 · 约100g", setOf(Allergen.WHEAT), mayContain = setOf(Allergen.MILK, Allergen.EGG), allergenInfoComplete = false, stock = 16),
        MenuItem("black-tea", "经典红茶", "茶饮", 1200, "black_tea", "清香红茶，含咖啡因。", "红茶、水；糖按规格添加", "中杯300ml / 大杯400ml", allergenInfoComplete = false, options = drinkOptions),
        MenuItem("cola", "冰爽可乐", "冷饮", 1000, "cola", "标准含糖可乐，固定冰饮，不支持调整原有糖。", "碳酸饮料；具体配方以实际包装为准", "一份 · 330ml", allergenInfoComplete = false),
        MenuItem("lemonade", "清爽柠檬饮", "冷饮", 1600, "lemonade", "柠檬风味冰饮，饮品基底含糖。", "柠檬、水、糖；额外糖按规格添加", "中杯300ml / 大杯400ml", allergenInfoComplete = false, options = listOf(size, iced, sweetened)),
        MenuItem("orange-juice", "橙汁", "冷饮", 1800, "orange_juice", "橙味果汁饮品，含原有糖分，固定冰饮。", "橙汁；原料及添加成分需确认", "中杯300ml / 大杯400ml", allergenInfoComplete = false, options = listOf(size, iced)),
        MenuItem("espresso", "意式浓缩", "咖啡", 1400, "espresso", "小杯浓缩咖啡，风味浓郁，含咖啡因。", "咖啡、水", "单份 · 约30ml · 热饮", allergenInfoComplete = false),
        MenuItem("cappuccino", "卡布奇诺", "咖啡", 2400, "cappuccino", "浓缩咖啡配牛奶与绵密奶泡，含咖啡因。", "咖啡、牛奶；糖按规格添加", "中杯300ml / 大杯400ml", setOf(Allergen.MILK), allergenInfoComplete = false, options = listOf(size, sugar)),
        MenuItem("cold-brew", "冷萃咖啡", "咖啡", 2400, "cold_brew", "低温浸萃咖啡，固定冰饮，含咖啡因。", "咖啡、水；糖按规格添加", "中杯300ml / 大杯400ml", allergenInfoComplete = false, options = icedOptions),
        MenuItem("dirty", "Dirty咖啡", "咖啡", 2600, "dirty", "浓缩咖啡与冷牛奶的层次搭配，固定配方，含咖啡因。", "咖啡、牛奶", "一杯 · 约180ml · 冷饮", setOf(Allergen.MILK), allergenInfoComplete = false),
        MenuItem("con-panna", "康宝蓝", "咖啡", 2000, "con_panna", "浓缩咖啡配奶油顶，固定配方，奶油含糖，含咖啡因。", "咖啡、奶油、糖", "一杯 · 约80ml · 热饮", setOf(Allergen.MILK), allergenInfoComplete = false),
        MenuItem("flat-white", "馥芮白", "咖啡", 2400, "flat_white", "浓缩咖啡搭配细腻奶泡，含咖啡因。", "咖啡、牛奶；糖按规格添加", "一杯 · 约200ml · 热饮", setOf(Allergen.MILK), allergenInfoComplete = false, options = listOf(sugar)),
        MenuItem("macchiato", "玛奇朵", "咖啡", 2000, "macchiato", "浓缩咖啡点缀奶泡，固定配方，含咖啡因。", "咖啡、牛奶", "一杯 · 约80ml · 热饮", setOf(Allergen.MILK), allergenInfoComplete = false),
        MenuItem("mocha", "巧克力摩卡", "咖啡", 2600, "mocha", "咖啡、牛奶与巧克力融合，巧克力基底含糖，含咖啡因。", "咖啡、牛奶、巧克力酱；额外糖按规格添加", "中杯300ml / 大杯400ml", setOf(Allergen.MILK, Allergen.SOY), allergenInfoComplete = false, options = milkTeaOptions),
        MenuItem("brown-sugar-tea", "黑糖珍珠奶茶", "茶饮", 2400, "brown_sugar_tea", "黑糖与珍珠风味，基底及珍珠原有糖不可去除，含咖啡因。", "茶、牛奶、木薯珍珠、黑糖", "中杯400ml / 大杯500ml", setOf(Allergen.MILK), allergenInfoComplete = false, options = listOf(size, temperature)),
        MenuItem("cheese-tea", "奶盖茶", "茶饮", 2200, "cheese_tea", "茶汤配奶盖，奶盖含糖，含咖啡因。", "茶、牛奶、奶油、糖；额外糖按规格添加", "中杯400ml / 大杯500ml", setOf(Allergen.MILK), allergenInfoComplete = false, options = listOf(size, iced, sweetened)),
        MenuItem("jasmine-tea", "茉莉奶茶", "茶饮", 2000, "jasmine_tea", "茉莉茶香与牛奶融合，基底含糖，含咖啡因。", "茉莉茶、牛奶、糖；额外糖按规格添加", "中杯400ml / 大杯500ml", setOf(Allergen.MILK), allergenInfoComplete = false, options = milkTeaOptions),
        MenuItem("oolong-tea", "乌龙奶茶", "茶饮", 2200, "oolong_tea", "乌龙茶韵搭配奶香，基底含糖，含咖啡因。", "乌龙茶、牛奶、糖；额外糖按规格添加", "中杯400ml / 大杯500ml", setOf(Allergen.MILK), allergenInfoComplete = false, options = milkTeaOptions),
        MenuItem("red-bean-tea", "红豆奶茶", "茶饮", 2400, "red_bean_tea", "奶茶搭配甜红豆，红豆及基底原有糖不可去除，含咖啡因。", "茶、牛奶、红豆、糖；额外糖按规格添加", "中杯400ml / 大杯500ml", setOf(Allergen.MILK), allergenInfoComplete = false, options = milkTeaOptions),
        MenuItem("strawberry-tea", "草莓奶盖茶", "茶饮", 2600, "strawberry_tea", "草莓风味茶饮配奶盖，基底含糖，含咖啡因。", "茶、草莓制品、牛奶、奶油、糖", "中杯400ml / 大杯500ml", setOf(Allergen.MILK), allergenInfoComplete = false, options = listOf(size, iced)),
        MenuItem("taro-tea", "芋头奶茶", "茶饮", 2400, "taro_tea", "香芋风味奶茶，基底含糖，含咖啡因。", "茶、牛奶、芋头制品、糖；额外糖按规格添加", "中杯400ml / 大杯500ml", setOf(Allergen.MILK), allergenInfoComplete = false, options = milkTeaOptions),
    )
}
